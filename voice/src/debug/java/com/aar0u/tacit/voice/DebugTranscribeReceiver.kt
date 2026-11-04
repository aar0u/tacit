// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Debug builds only: runs the whole VAD + ASR pipeline on a 16 kHz mono 16-bit WAV
 * and logs the text and timings, so accuracy and speed can be checked without a microphone.
 *
 *   adb shell am broadcast -n com.aar0u.tacit.voice/.DebugTranscribeReceiver --es wav /sdcard/test.wav
 *   adb logcat -s TacitVoiceDebug
 */
class DebugTranscribeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra("wav") ?: return
        val app = context.applicationContext
        val pending = goAsync()
        thread(name = "tacit-voice-debug") {
            try {
                run(app, File(path))
            } catch (error: Throwable) {
                Log.e(TAG, "failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private fun run(context: Context, wav: File) {
        if (!ModelFiles.isInstalled(context)) ModelFiles.install(context)
        val bytes = wav.readBytes()
        // Canonical 44-byte header; the files used for this check are produced by ffmpeg.
        val pcm = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val audio = FloatArray(pcm.remaining()) { pcm.get() / 32768f }

        val loadStart = SystemClock.elapsedRealtime()
        val engine = VoiceEngine(context)
        Log.i(TAG, "engine loaded in ${SystemClock.elapsedRealtime() - loadStart} ms")

        val segments = ArrayList<FloatArray>()
        val start = SystemClock.elapsedRealtime()
        var offset = 0
        while (offset + VoiceEngine.WINDOW_SAMPLES <= audio.size) {
            segments += engine.accept(audio.copyOfRange(offset, offset + VoiceEngine.WINDOW_SAMPLES))
            offset += VoiceEngine.WINDOW_SAMPLES
        }
        segments += engine.flush()
        val text = segments.joinToString("") { engine.transcribe(it) }
        val took = SystemClock.elapsedRealtime() - start
        val seconds = audio.size / VoiceEngine.SAMPLE_RATE.toFloat()
        Log.i(TAG, "audio=%.1fs segments=%d took=%d ms rtf=%.3f".format(seconds, segments.size, took, took / 1000f / seconds))
        Log.i(TAG, "text=$text")
        engine.release()
    }

    private companion object {
        const val TAG = "TacitVoiceDebug"
    }
}
