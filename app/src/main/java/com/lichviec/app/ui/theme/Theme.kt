package com.lichviec.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bảng màu tối giản Đen – Trắng – Xám hòa hợp (OLED Monochrome)
val BgBlack = Color(0xFF09090B)
val SurfaceDark = Color(0xFF131316)
val SurfaceElevated = Color(0xFF1C1C21)
val BorderGray = Color(0xFF27272A)
val BorderBright = Color(0xFF3F3F46)

val TextWhite = Color(0xFFFAFAFA)
val PureWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)
val TagGrayBg = Color(0xFF232329)

private val MonochromeDarkScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = BgBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = Color(0xFFD4D4D8),
    onSecondary = BgBlack,
    secondaryContainer = TagGrayBg,
    onSecondaryContainer = TextWhite,
    background = BgBlack,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextGray,
    outline = BorderGray,
    outlineVariant = BorderBright
)

@Composable
fun LichViecTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeDarkScheme,
        content = content
    )
}
