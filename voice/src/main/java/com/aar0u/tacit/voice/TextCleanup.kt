// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

// CJK ideographs plus CJK and full-width punctuation.
private const val CJK = "\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef"
private val SPACE_BETWEEN_CJK = Regex("(?<=[$CJK])\\s+(?=[$CJK])")

/** X-ASR puts a space after each punctuation mark; Chinese text should not have it. */
internal fun cleanTranscript(raw: String): String = raw.trim().replace(SPACE_BETWEEN_CJK, "")
