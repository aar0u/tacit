// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanupTest {
    @Test
    fun removesSpaceAfterChinesePunctuation() {
        assertEquals("好，我非常荣幸。", cleanTranscript("好， 我非常荣幸。"))
    }

    @Test
    fun keepsSpacesAroundLatinWords() {
        assertEquals("谢谢 TED 的邀请", cleanTranscript("谢谢 TED 的邀请"))
        assertEquals("open the door", cleanTranscript("  open the door "))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", cleanTranscript("   "))
    }
}
