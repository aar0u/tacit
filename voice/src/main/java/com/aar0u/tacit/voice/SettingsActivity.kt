// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.aar0u.tacit.voice

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/** One screen that gets the two prerequisites done: microphone permission and the model copy. */
class SettingsActivity : Activity() {
    private lateinit var summary: TextView
    private lateinit var permissionButton: Button
    private lateinit var modelButton: Button
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        column.addView(
            TextView(this).apply {
                text = "Tacit 语音"
                textSize = 24f
                setTextColor(Color.BLACK)
            },
        )
        summary = TextView(this).apply { textSize = 16f; setPadding(0, pad, 0, pad) }
        permissionButton = Button(this).apply {
            text = "授予录音权限"
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO) }
        }
        modelButton = Button(this).apply { text = "准备语音模型"; setOnClickListener { installModels() } }
        val imeButton = Button(this).apply {
            text = "打开输入法设置（启用 Tacit 语音）"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        listOf(summary, permissionButton, modelButton, imeButton).forEach {
            column.addView(it, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }

    private fun hasPermission() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        val permission = hasPermission()
        val models = ModelFiles.isInstalled(this)
        val bundled = ModelFiles.isBundled(this)
        permissionButton.isEnabled = !permission
        modelButton.isEnabled = !models && bundled
        summary.text = buildString {
            appendLine("录音权限：${if (permission) "已授予" else "未授予"}")
            appendLine("语音模型：${if (models) "已准备" else if (bundled) "未准备" else "此安装包不含模型"}")
            appendLine()
            append("录音和识别都在本机完成，本应用没有联网权限。")
        }
    }

    private fun installModels() {
        modelButton.isEnabled = false
        worker.execute {
            val result = runCatching {
                ModelFiles.install(this) { done, total ->
                    runOnUiThread { summary.text = "正在复制模型 $done/$total …" }
                }
            }
            runOnUiThread {
                result.onFailure { summary.text = "准备失败：${it.message}" }
                    .onSuccess { refresh() }
            }
        }
    }

    private companion object {
        const val REQUEST_AUDIO = 1
    }
}
