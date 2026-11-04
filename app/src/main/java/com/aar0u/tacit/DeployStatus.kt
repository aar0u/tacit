// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit

import com.osfans.trime.core.RimeMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether Rime is compiling its dictionaries. The first deploy after an install takes a few
 * minutes, during which nothing can be typed in Chinese; the keyboard shows a notice meanwhile.
 * The flow is process-wide, so a keyboard that starts mid-deploy still sees the right value.
 */
object DeployStatus {
    private val mutableDeploying = MutableStateFlow(false)

    val deploying: StateFlow<Boolean> get() = mutableDeploying

    fun update(state: RimeMessage.DeployMessage.State) {
        mutableDeploying.value = state == RimeMessage.DeployMessage.State.Start
    }
}
