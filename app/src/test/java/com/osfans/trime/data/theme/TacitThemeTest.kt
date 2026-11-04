// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.theme

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class TacitThemeTest :
    StringSpec({
        val theme = ThemeTestSupport.decodeBuiltinTheme("tacit.trime.yaml")

        "every letter layout Trime may fall back to is the Tacit layout" {
            val tacit = theme.presetKeyboards.getValue("rime_ice")
            tacit.keys.shouldNotBeEmpty()
            for (id in listOf("default", "qwerty", "qwerty_", "qwerty0")) {
                theme.presetKeyboards.getValue(id).keys shouldBe tacit.keys
            }
        }

        "only Tacit color schemes are kept" {
            val own = theme.presetColorSchemes.filterValues { it["author"] == "Tacit" }
            own.size shouldBe 12
            own.keys.containsAll(listOf("default", "lavender_dark", "blue", "black_dark")) shouldBe true
        }
    })
