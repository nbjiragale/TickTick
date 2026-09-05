package com.niranjan.ticktick.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TickTickColors {
    val Background = Color(0xFFF3F4F9)
    val Surface = Color.White
    val Text = Color(0xFF202124)
    val SecondaryText = Color(0xFF96989C)
    val Accent = Color(0xFF4778FF)
    val DueTime = Color(0xFF557EB9)
    val TodayChip = Color(0xFFE0E7F9)
    val Checkbox = Color(0xFFB1B3B5)
    val NavigationInactive = Color(0xFF929497)
    val Orange = Color(0xFFFF7900)
    val DrawerTop = Color(0xFFEAEDFC)
}

object TickTickDimensions {
    val PageInset = 22.dp
    val TaskHeight = 44.dp
    val CardRadius = 12.dp
    val HeaderHeight = 56.dp
    val BottomBarHeight = 64.dp
    val FabSize = 56.dp
}

private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

private val AppTypography = Typography(
    titleLarge = textStyle(20, 26, FontWeight.Bold),
    titleMedium = textStyle(16, 22, FontWeight.Bold),
    bodyLarge = textStyle(16, 22),
    bodyMedium = textStyle(14, 20),
    bodySmall = textStyle(12, 16),
    labelLarge = textStyle(14, 20, FontWeight.Medium),
)

@Composable
fun TickTickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TickTickColors.Accent,
            onPrimary = Color.White,
            secondary = TickTickColors.DueTime,
            background = TickTickColors.Background,
            onBackground = TickTickColors.Text,
            surface = TickTickColors.Surface,
            onSurface = TickTickColors.Text,
            surfaceVariant = TickTickColors.Background,
            onSurfaceVariant = TickTickColors.SecondaryText,
            outline = TickTickColors.Checkbox,
            surfaceTint = Color.Transparent,
        ),
        typography = AppTypography,
        content = content,
    )
}
