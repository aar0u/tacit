// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ui.main.settings

import android.app.AlertDialog
import android.content.Context
import com.osfans.trime.R
import com.osfans.trime.data.theme.ColorManager
import com.osfans.trime.data.theme.ThemeManager
import com.osfans.trime.data.theme.ThemePrefs

/** Follow the system, or force light or dark. */
object DarkModeDialog {
    fun build(context: Context): AlertDialog {
        val modes = ThemePrefs.DarkMode.entries
        return AlertDialog
            .Builder(context)
            .setTitle(R.string.dark_mode)
            .setSingleChoiceItems(
                modes.map { context.getString(it.stringRes) }.toTypedArray(),
                modes.indexOf(ThemeManager.prefs.darkMode.getValue()),
            ) { dialog, which ->
                ThemeManager.prefs.darkMode.setValue(modes[which])
                ColorManager.refresh()
                dialog.dismiss()
            }.setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}
