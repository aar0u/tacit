// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TextCleanupTest :
    StringSpec({
        "removes the space after Chinese punctuation" {
            cleanTranscript("好， 我非常荣幸。") shouldBe "好，我非常荣幸。"
        }

        "keeps spaces around Latin words" {
            cleanTranscript("谢谢 TED 的邀请") shouldBe "谢谢 TED 的邀请"
            cleanTranscript("  open the door ") shouldBe "open the door"
        }

        "blank input becomes empty" {
            cleanTranscript("   ") shouldBe ""
        }
    })
