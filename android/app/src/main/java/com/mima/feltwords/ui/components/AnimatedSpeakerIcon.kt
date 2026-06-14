package com.mima.feltwords.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSpeakerIcon(
    isSpeaking: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val transition = rememberInfiniteTransition(label = "speaker")
    val wave1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave1",
    )
    val wave2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, delayMillis = 270, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave2",
    )
    val wave3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, delayMillis = 540, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave3",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.32f
        val cy = h * 0.5f

        // Speaker body (small rectangle + triangle)
        val bodyLeft = w * 0.08f
        val bodyRight = w * 0.22f
        val bodyTop = cy - h * 0.12f
        val bodyBottom = cy + h * 0.12f
        val strokeW = w * 0.08f

        drawRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = androidx.compose.ui.geometry.Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
        )
        // Cone
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(bodyRight, bodyTop)
            lineTo(cx + w * 0.06f, cy - h * 0.24f)
            lineTo(cx + w * 0.06f, cy + h * 0.24f)
            lineTo(bodyRight, bodyBottom)
            close()
        }
        drawPath(path, tint)

        if (isSpeaking) {
            val arcStroke = Stroke(width = strokeW * 0.7f, cap = StrokeCap.Round)
            val startAngle = -35f
            val sweepAngle = 70f

            fun drawWave(progress: Float, radius: Float) {
                val alpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                drawArc(
                    color = tint.copy(alpha = alpha.coerceIn(0.15f, 1f)),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = arcStroke,
                )
            }

            drawWave(wave1, w * 0.22f)
            drawWave(wave2, w * 0.32f)
            drawWave(wave3, w * 0.42f)
        } else {
            // Static: two small arcs
            val arcStroke = Stroke(width = strokeW * 0.65f, cap = StrokeCap.Round)
            drawArc(
                color = tint.copy(alpha = 0.6f),
                startAngle = -30f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.2f, cy - w * 0.2f),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, w * 0.4f),
                style = arcStroke,
            )
            drawArc(
                color = tint.copy(alpha = 0.35f),
                startAngle = -30f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.33f, cy - w * 0.33f),
                size = androidx.compose.ui.geometry.Size(w * 0.66f, w * 0.66f),
                style = arcStroke,
            )
        }
    }
}
