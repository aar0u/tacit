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

        "every scheme colors functional keys, which the layouts no longer color one by one" {
            val needed = listOf("off_key_back_color", "off_key_text_color", "hilited_off_key_back_color")
            for (scheme in theme.presetColorSchemes.values.filter { it["author"] == "Tacit" }) {
                for (key in needed) (key in scheme) shouldBe true
            }
        }

        "no symbol tab has a key that no font can draw" {
            val liquid = theme.liquidKeyboard
            val tabs = liquid.getTagList().indices.filter { liquid.getTagList()[it].label != "更多" }
            val broken = tabs.flatMap { liquid.getDataByIndex(it) }.map { it.text }.filter { text ->
                text.any { Character.getType(it) == Character.PRIVATE_USE.toInt() } ||
                    text.firstOrNull()?.let { Character.getType(it) == Character.NON_SPACING_MARK.toInt() } == true
            }
            broken shouldBe emptyList()
        }

        "only Tacit color schemes are kept" {
            val own = theme.presetColorSchemes.filterValues { it["author"] == "Tacit" }
            own.size shouldBe 12
            own.keys.containsAll(listOf("default", "lavender_dark", "blue", "black_dark")) shouldBe true
        }
    })
