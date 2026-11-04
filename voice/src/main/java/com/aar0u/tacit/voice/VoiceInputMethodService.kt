// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * A voice-only input method. The Tacit keyboard switches to it from its
 * microphone key; the "keyboard" button here switches back.
 *
 * Tap the microphone to start. Each sentence is transcribed after a short
 * pause and typed into the focused field. Tap again to stop.
 */
class VoiceInputMethodService : InputMethodService() {
    private val main = Handler(Looper.getMainLooper())
    private val decoder = Executors.newSingleThreadExecutor()

    private var engine: VoiceEngine? = null
    private var loading = false

    @Volatile private var recording = false
    private var recordThread: Thread? = null

    private lateinit var status: TextView
    private lateinit var micButton: Button

    override fun onCreateInputView(): View {
        val padding = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(BACKGROUND)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230))
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(TEXT)
            gravity = Gravity.CENTER
        }
        micButton = pill("开始说话", ACCENT, Color.WHITE).apply { setOnClickListener { toggle() } }
        val back = pill("返回键盘", FUNCTION, ACCENT_DARK).apply {
            setOnClickListener { switchToPreviousInputMethod() }
        }
        val erase = pill("退格", FUNCTION, ACCENT_DARK).apply {
            setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) }
        }
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(back, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
            addView(erase, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(micButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)))
        root.addView(
            bottom,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) },
        )
        return root
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshState()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopRecording()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        stopRecording()
        recordThread?.join(1000)
        decoder.execute { engine?.release(); engine = null }
        decoder.shutdown()
        super.onDestroy()
    }

    private fun hasMicPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun refreshState() {
        when {
            recording -> Unit
            !hasMicPermission() -> showSetup("需要录音权限。点击按钮打开设置。")
            !ModelFiles.isInstalled(this) -> showSetup("首次使用需要准备语音模型。点击按钮打开设置。")
            engine == null -> loadEngine()
            else -> showReady()
        }
    }

    private fun showSetup(message: String) {
        status.text = message
        micButton.text = "打开设置"
        micButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun showReady() {
        status.text = "点击麦克风，说完一句话后会自动输入"
        micButton.text = "开始说话"
        micButton.setOnClickListener { toggle() }
    }

    private fun loadEngine() {
        if (loading) return
        loading = true
        status.text = "正在加载语音模型…"
        micButton.isEnabled = false
        decoder.execute {
            val startedAt = SystemClock.elapsedRealtime()
            val result = runCatching { VoiceEngine(this) }
            result.onSuccess { Log.i(TAG, "engine loaded in ${SystemClock.elapsedRealtime() - startedAt} ms") }
                .onFailure { Log.e(TAG, "engine failed to load", it) }
            main.post {
                loading = false
                micButton.isEnabled = true
                result.onSuccess { engine = it; showReady() }
                    .onFailure { status.text = "语音模型加载失败：${it.message}" }
            }
        }
    }

    private fun toggle() {
        if (recording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        val activeEngine = engine ?: return refreshState()
        if (!hasMicPermission()) return refreshState()
        val minBuffer = AudioRecord.getMinBufferSize(
            VoiceEngine.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            VoiceEngine.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, VoiceEngine.SAMPLE_RATE * 2 * 4),
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            status.text = "无法打开麦克风（可能被其他应用占用）"
            return
        }
        recording = true
        status.text = "正在听…"
        micButton.text = "停止"
        recordThread = Thread({ captureLoop(recorder, activeEngine) }, "tacit-voice-capture").also { it.start() }
    }

    private fun stopRecording() {
        if (!recording) return
        recording = false
        micButton.text = "开始说话"
        status.text = "正在处理最后一句…"
    }

    private fun captureLoop(recorder: AudioRecord, engine: VoiceEngine) {
        val pcm = ShortArray(VoiceEngine.WINDOW_SAMPLES)
        val window = FloatArray(VoiceEngine.WINDOW_SAMPLES)
        try {
            recorder.startRecording()
            while (recording) {
                val read = recorder.read(pcm, 0, pcm.size)
                if (read < pcm.size) continue
                for (i in pcm.indices) window[i] = pcm[i] / 32768f
                engine.accept(window).forEach(::decodeAndType)
            }
            engine.flush().forEach(::decodeAndType)
        } finally {
            recorder.stop()
            recorder.release()
            decoder.execute { main.post { if (!recording) showReady() } }
        }
    }

    private fun decodeAndType(samples: FloatArray) {
        decoder.execute {
            val startedAt = SystemClock.elapsedRealtime()
            val text = runCatching { engine?.transcribe(samples).orEmpty() }
                .onFailure { Log.e(TAG, "transcribe failed", it) }
                .getOrDefault("")
            Log.i(TAG, "decoded ${samples.size / 16} ms of audio in ${SystemClock.elapsedRealtime() - startedAt} ms")
            if (text.isNotEmpty()) main.post { currentInputConnection?.commitText(text, 1) }
        }
    }

    private fun pill(label: String, fill: Int, textColor: Int) = Button(this).apply {
        text = label
        textSize = 18f
        isAllCaps = false
        setTextColor(textColor)
        background = GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(24).toFloat()
        }
        stateListAnimator = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "TacitVoice"
        val BACKGROUND = Color.parseColor("#E2E4EA")
        val TEXT = Color.parseColor("#1B1B1F")
        val ACCENT = Color.parseColor("#6750A4")
        val ACCENT_DARK = Color.parseColor("#4A3B87")
        val FUNCTION = Color.parseColor("#E8DEF8")
    }
}
