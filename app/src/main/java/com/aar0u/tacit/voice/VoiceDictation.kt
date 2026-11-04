// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.osfans.trime.R
import com.osfans.trime.ime.core.TrimeInputMethodService
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/**
 * Offline dictation inside the keyboard process. Loading the engine takes a few seconds,
 * so it is loaded once, as soon as the keyboard shows, and kept until the system asks
 * for memory back. After that the voice key and the space bar start recording at once
 * and every sentence is typed after a short pause.
 *
 * All entry points run on the main thread. Audio capture and decoding run on their own
 * threads and hand text back through [main].
 */
object VoiceDictation {
    private const val TAG = "TacitVoice"

    /** Toggle name that voice keys use in the theme; its state is [isRecording], not a Rime option. */
    const val TOGGLE_OPTION = "_voice_input"

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "tacit-voice-worker") }

    // Written on the main thread, read by the capture and decoding threads.
    @Volatile private var engine: VoiceEngine? = null
    @Volatile private var recording = false

    private var loading = false
    private var startWhenReady = false
    private var holding = false
    private var captureThread: Thread? = null
    private var target = WeakReference<TrimeInputMethodService>(null)

    val isRecording: Boolean get() = recording

    /** Called on the main thread whenever recording starts or stops, so the keys can redraw. */
    var onStateChanged: ((Boolean) -> Unit)? = null

    private fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Loads the engine in the background when everything it needs is there. */
    fun preload(context: Context) {
        val app = context.applicationContext
        if (engine != null || loading || !hasPermission(app) || !ModelFiles.isBundled(app)) return
        loading = true
        worker.execute {
            val startedAt = SystemClock.elapsedRealtime()
            val result = runCatching {
                if (!ModelFiles.isInstalled(app)) ModelFiles.install(app)
                VoiceEngine(app)
            }
            result.onSuccess { Log.i(TAG, "engine loaded in ${SystemClock.elapsedRealtime() - startedAt} ms") }
                .onFailure { Log.e(TAG, "engine failed to load", it) }
            main.post { onEngineLoaded(app, result) }
        }
    }

    private fun onEngineLoaded(context: Context, result: Result<VoiceEngine>) {
        loading = false
        result.onSuccess { loaded ->
            engine = loaded
            val service = target.get()
            if (startWhenReady && service != null) startRecording(service)
        }.onFailure { toast(context, R.string.voice_failed, it.message.orEmpty()) }
        startWhenReady = false
    }

    /** The voice key: start or stop dictation. */
    fun toggle(service: TrimeInputMethodService) {
        target = WeakReference(service)
        when {
            recording -> {
                stop()
                toast(service, R.string.voice_stopped)
            }
            !hasPermission(service) -> service.startActivity(
                Intent(service, VoicePermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            !ModelFiles.isBundled(service) -> toast(service, R.string.voice_no_models)
            engine == null -> {
                startWhenReady = true
                toast(service, R.string.voice_loading)
                preload(service)
            }
            else -> startRecording(service)
        }
    }

    /** Long press on the space bar: record while the key is held. */
    fun startHold(service: TrimeInputMethodService) {
        holding = true
        if (!recording) toggle(service)
    }

    /** The space bar was released after [startHold]. A no-op for any other long press. */
    fun releaseHold() {
        if (holding) stop()
    }

    /** Ends recording; the sentence still being spoken is finished and typed. */
    fun stop() {
        startWhenReady = false
        holding = false
        setRecording(false)
    }

    /** Frees the engine (about half a gigabyte) when the system is short of memory. */
    fun release() {
        stop()
        val thread = captureThread
        val old = engine
        engine = null
        worker.execute {
            thread?.join(2000)
            old?.release()
        }
    }

    private fun setRecording(on: Boolean) {
        if (recording == on) return
        recording = on
        onStateChanged?.invoke(on)
    }

    private fun startRecording(service: TrimeInputMethodService) {
        val active = engine ?: return
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
            toast(service, R.string.voice_mic_busy)
            return
        }
        setRecording(true)
        toast(service, R.string.voice_listening)
        captureThread = Thread({ capture(recorder, active) }, "tacit-voice-capture").also { it.start() }
    }

    private fun capture(recorder: AudioRecord, active: VoiceEngine) {
        val pcm = ShortArray(VoiceEngine.WINDOW_SAMPLES)
        val window = FloatArray(VoiceEngine.WINDOW_SAMPLES)
        try {
            recorder.startRecording()
            while (recording) {
                if (recorder.read(pcm, 0, pcm.size) < pcm.size) continue
                for (i in pcm.indices) window[i] = pcm[i] / 32768f
                active.accept(window).forEach(::typeLater)
            }
            active.flush().forEach(::typeLater)
        } finally {
            recorder.stop()
            recorder.release()
        }
    }

    private fun typeLater(samples: FloatArray) {
        worker.execute {
            val startedAt = SystemClock.elapsedRealtime()
            val text = runCatching { engine?.transcribe(samples).orEmpty() }
                .onFailure { Log.e(TAG, "transcribe failed", it) }
                .getOrDefault("")
            Log.i(TAG, "decoded ${samples.size / 16} ms of audio in ${SystemClock.elapsedRealtime() - startedAt} ms")
            if (text.isNotEmpty()) main.post { target.get()?.commitText(text) }
        }
    }

    private fun toast(context: Context, @StringRes message: Int, vararg args: Any) =
        Toast.makeText(context.applicationContext, context.getString(message, *args), Toast.LENGTH_SHORT).show()
}
