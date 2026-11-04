/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.theme

import com.charleskorn.kaml.YamlMap
import com.osfans.trime.util.mapping
import java.io.File

/**
 * Decodes a theme from its source file, expanding the librime DSL subset the same way
 * [ThemeLoader] does but skipping the librime deploy step, which needs rime_jni and is
 * unavailable in JVM unit tests. Cross-file references resolve to files in the shared
 * assets directory. Paths are relative to the app module directory (the unit-test
 * working directory).
 */
object ThemeTestSupport {
    /** The parser themes are read with in production, so fixtures see the same configuration. */
    val yaml = ThemeYaml.parser

    fun decodeThemeFile(relativePath: String): Theme = themeAndNode(relativePath).first

    /** Decodes [relativePath] and also returns the expanded node it came from. */
    fun themeAndNode(relativePath: String): Pair<Theme, YamlMap> {
        // Themes that Tacit no longer ships live in test resources and keep their coverage.
        val file = File(relativePath).takeIf { it.isFile } ?: File("src/test/resources/themes", File(relativePath).name)
        check(file.isFile) { "Theme fixture not found: $relativePath (cwd=${File(".").absolutePath})" }
        val node = yaml.parseToYamlNode(file.readText())
        val shared = File("src/main/assets/shared")
        val expanded = ThemeDslExpander.expand(file.nameWithoutExtension, node) { id ->
            File(shared, "$id.yaml").takeIf { it.isFile }?.let { yaml.parseToYamlNode(it.readText()) }
        }
        val mapping = expanded.mapping
            ?: error("$relativePath: YAML root is not a mapping")
        return yaml.decodeFromYamlNode<Theme>(mapping) to mapping
    }

    /** Decodes a built-in theme source file (app/src/main/assets/shared/). */
    fun decodeBuiltinTheme(fileName: String): Theme = decodeThemeFile("src/main/assets/shared/$fileName")
}
