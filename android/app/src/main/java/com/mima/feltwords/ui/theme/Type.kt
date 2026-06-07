package com.mima.feltwords.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 圆润粗体基调（对应 iOS 的 .rounded 设计）。后续可替换为内置圆体字体资源。
private val Rounded = FontFamily.Default

val FeltTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 16.sp),
)
