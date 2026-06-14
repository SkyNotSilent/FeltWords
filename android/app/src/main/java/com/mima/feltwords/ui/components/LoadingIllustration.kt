package com.mima.feltwords.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mima.feltwords.ui.theme.FeltTheme

/**
 * 生成中/识别中的可复用加载视觉组件。
 * 毛毡风格：暖色转圈 + 文案。
 * 对齐 iOS IllustrationLoadingView（简化版）。
 */
@Composable
fun LoadingIllustration(
    caption: String,
    modifier: Modifier = Modifier,
) {
    val felt = FeltTheme.colors

    // 旋转动画，营造毛毡感
    val infiniteTransition = rememberInfiniteTransition(label = "loadingRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(felt.cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = felt.orange,
                modifier = Modifier
                    .size(40.dp)
                    .rotate(rotation),
                strokeWidth = 4.dp,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = felt.secondary,
        )
    }
}
