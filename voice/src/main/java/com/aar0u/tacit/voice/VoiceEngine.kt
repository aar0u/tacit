// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig

/**
 * Silero VAD cuts the microphone stream into sentences; X-ASR transcribes each
 * sentence in one pass. Whole-sentence decoding was clearly more accurate than
 * streaming models in our desktop comparison, at the cost of text appearing
 * after a pause instead of while speaking.
 *
 * [accept] and [flush] belong to the audio thread, [transcribe] to a decoder
 * thread; the two halves share no state.
 */
internal class VoiceEngine(context: Context) {
    private val vad: Vad
    private val recognizer: OfflineRecognizer

    init {
        val app = context.applicationContext
        fun path(name: String) = ModelFiles.file(app, name).path
        vad = Vad(
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = path(ModelFiles.VAD),
                    threshold = VAD_THRESHOLD,
                    minSilenceDuration = MIN_SILENCE_SECONDS,
                    minSpeechDuration = MIN_SPEECH_SECONDS,
                    windowSize = WINDOW_SAMPLES,
                    maxSpeechDuration = MAX_SPEECH_SECONDS,
                ),
                sampleRate = SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
            ),
        )
        recognizer = try {
            OfflineRecognizer(
                config = OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(
                            encoder = path(ModelFiles.ENCODER),
                            decoder = path(ModelFiles.DECODER),
                            joiner = path(ModelFiles.JOINER),
                        ),
                        tokens = path(ModelFiles.TOKENS),
                        modelingUnit = "cjkchar+bpe",
                        bpeVocab = path(ModelFiles.BPE),
                        numThreads = 2,
                        provider = "cpu",
                    ),
                    decodingMethod = "greedy_search",
                ),
            )
        } catch (error: Throwable) {
            vad.release()
            throw error
        }
    }

    /** Feeds one window of audio and returns the sentences that just ended. */
    fun accept(window: FloatArray): List<FloatArray> {
        vad.acceptWaveform(window)
        return drain()
    }

    /** Ends the recording: returns the sentence that was still open, if any. */
    fun flush(): List<FloatArray> {
        vad.flush()
        val rest = drain()
        vad.reset()
        return rest
    }

    fun transcribe(samples: FloatArray): String {
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            cleanTranscript(recognizer.getResult(stream).text)
        } finally {
            stream.release()
        }
    }

    fun release() {
        vad.release()
        recognizer.release()
    }

    private fun drain(): List<FloatArray> = buildList {
        while (!vad.empty()) {
            add(vad.front().samples)
            vad.pop()
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val WINDOW_SAMPLES = 512

        private const val VAD_THRESHOLD = 0.5f
        private const val MIN_SILENCE_SECONDS = 0.6f
        private const val MIN_SPEECH_SECONDS = 0.25f
        private const val MAX_SPEECH_SECONDS = 20f
    }
}
