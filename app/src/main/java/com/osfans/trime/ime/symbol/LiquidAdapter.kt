/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.symbol

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.data.theme.model.LiquidKeyboard
import com.osfans.trime.ime.core.AutoScaleTextView
import splitties.dimensions.dp
import splitties.views.gravityCenter

class LiquidAdapter(
    private val scope: ThemeScope,
    private val onItemClick: LiquidKeyboard.KeyItem.(Int) -> Unit,
) : BaseQuickAdapter<LiquidKeyboard.KeyItem, LiquidAdapter.ViewHolder>() {
    inner class ViewHolder(
        val ui: LiquidItemUi,
    ) : RecyclerView.ViewHolder(ui.root)

    private var itemWidth = 0

    /**
     * Stretches the items so whole columns fill [available] pixels, with [gap] pixels between
     * neighbours; the theme's single_width is the narrowest an item may get.
     */
    fun fitWidth(available: Int, gap: Int, minWidth: Int) {
        val columns = (available / (minWidth + gap)).coerceAtLeast(1)
        val width = available / columns - gap
        if (width == itemWidth) return
        itemWidth = width
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val ui = LiquidItemUi(context, scope)
        ui.mainText.apply {
            scaleMode = AutoScaleTextView.Mode.Proportional
            gravity = gravityCenter
        }
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        item: LiquidKeyboard.KeyItem?,
    ) {
        item ?: return
        holder.ui.refreshColors()
        holder.ui.mainText.updateLayoutParams {
            width = itemWidth.takeIf { it > 0 } ?: holder.ui.ctx.dp(scope.theme.liquidKeyboard.singleWidth)
        }
        // Single symbols and emoji keep the key size; words such as the tab names use the long-label size.
        val style = scope.theme.style
        holder.ui.mainText.textSize =
            if (item.text.codePointCount(0, item.text.length) > 1) style.keyLongTextSize else style.keyTextSize
        holder.ui.mainText.text = item.text
        holder.ui.root.setOnClickListener {
            onItemClick.invoke(item, position)
        }
    }
}
