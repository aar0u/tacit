// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.content.Context
import java.io.File

/**
 * The models ship inside the APK (script/fetch_voice_models.py puts them in
 * assets/voice/models before the build). sherpa-onnx wants real file paths, so
 * they are copied to private storage once per model version.
 */
internal object ModelFiles {
    const val VERSION = "x-asr-2026-06-03+silero-vad"

    const val ENCODER = "xasr/encoder.int8.onnx"
    const val DECODER = "xasr/decoder.onnx"
    const val JOINER = "xasr/joiner.int8.onnx"
    const val TOKENS = "xasr/tokens.txt"
    const val BPE = "xasr/bpe.model"
    const val VAD = "silero_vad.onnx"

    private val ALL = listOf(ENCODER, DECODER, JOINER, TOKENS, BPE, VAD)
    private const val MARKER = "installed-version"
    private const val ASSET_ROOT = "voice/models"

    fun dir(context: Context) = File(context.filesDir, "voice-models")

    fun file(context: Context, name: String) = File(dir(context), name)

    fun isInstalled(context: Context): Boolean =
        File(dir(context), MARKER).takeIf { it.isFile }?.readText() == VERSION &&
            ALL.all { file(context, it).isFile }

    /** True when the APK carries the models; false for a build made without them. */
    fun isBundled(context: Context): Boolean =
        runCatching { ALL.all { name -> context.assets.open("$ASSET_ROOT/$name").use { true } } }.getOrDefault(false)

    fun install(context: Context, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }) {
        check(isBundled(context)) { "This build does not contain the speech models" }
        val root = dir(context)
        File(root, MARKER).delete()
        ALL.forEachIndexed { index, name ->
            onProgress(index, ALL.size)
            val target = file(context, name)
            target.parentFile?.mkdirs()
            val partial = File(target.path + ".part")
            context.assets.open("$ASSET_ROOT/$name").use { input ->
                partial.outputStream().use { output -> input.copyTo(output, 1 shl 16) }
            }
            check(partial.renameTo(target)) { "Cannot write ${target.path}" }
        }
        File(root, MARKER).writeText(VERSION)
        onProgress(ALL.size, ALL.size)
    }
}
