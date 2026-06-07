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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mima.feltwords.ui.theme.FeltTheme

/**
 * 统一卡片容器 — 对齐 iOS 风格。
 *
 * 标准圆角 24dp，表面色背景，柔和阴影。
 * 所有卡片类 UI 统一使用此组件，避免重复写 shadow + background + clip。
 */
@Composable
fun FeltCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 6.dp,
    padding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val felt = FeltTheme.colors
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = felt.ink.copy(alpha = 0.06f))
            .clip(shape)
            .background(felt.surface)
            .padding(padding),
        content = content,
    )
}

/**
 * 带渐变背景的卡片。
 */
@Composable
fun FeltGradientCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 10.dp,
    colors: List<androidx.compose.ui.graphics.Color> = listOf(
        FeltTheme.colors.orange.copy(alpha = 0.3f),
        FeltTheme.colors.cream,
    ),
    content: @Composable BoxScope.() -> Unit,
) {
    val felt = FeltTheme.colors
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = felt.ink.copy(alpha = 0.08f))
            .clip(shape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = colors),
                shape = shape,
            ),
        content = content,
    )
}
