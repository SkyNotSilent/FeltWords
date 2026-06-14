package com.mima.feltwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mima.feltwords.ui.theme.FeltTheme

/**
 * 毛玻璃卡片 — 对齐 iOS GlassCard。
 *
 * 使用半透明高光、细描边与柔和阴影模拟 iOS 毛玻璃。
 * 不对容器自身调用 blur，否则文字和图标也会一起变糊。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val felt = FeltTheme.colors
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = felt.ink.copy(alpha = 0.08f))
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        felt.surface.copy(alpha = 0.7f),
                        felt.surface.copy(alpha = 0.5f),
                    )
                )
            )
            .padding(20.dp),
        content = content,
    )
}

/**
 * 简化版毛玻璃：用于嵌套场景，不需要额外 padding。
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit,
) {
    val felt = FeltTheme.colors
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(felt.surface.copy(alpha = alpha)),
        content = content,
    )
}
