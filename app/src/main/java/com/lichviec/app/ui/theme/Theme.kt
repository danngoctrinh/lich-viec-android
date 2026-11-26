package com.lichviec.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Bảng màu tối giản Đen tuyền – Xám kiến trúc – Trắng tinh khiết
val CanvasBlack = Color(0xFF000000)
val RailDark = Color(0xFF0D0D0F)
val SurfaceMuted = Color(0xFF161619)
val SurfacePressed = Color(0xFF202025)
val RuleGray = Color(0xFF26262B)
val RuleStrong = Color(0xFF3A3A42)

val InkWhite = Color(0xFFFFFFFF)
val InkSoftWhite = Color(0xFFE4E4E7)
val InkSecondary = Color(0xFF9A9AA4)
val InkMuted = Color(0xFF5C5C66)

// Alias tương thích cho các thành phần phụ
val BgBlack = CanvasBlack
val SurfaceDark = RailDark
val SurfaceElevated = SurfaceMuted
val BorderGray = RuleGray
val BorderBright = RuleStrong
val PureWhite = InkWhite
val TextWhite = InkSoftWhite
val TextGray = InkSecondary
val TextMuted = InkMuted

private val EditorialTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum"
    )
)

private val MonochromeScheme = darkColorScheme(
    primary = InkWhite,
    onPrimary = CanvasBlack,
    primaryContainer = SurfaceMuted,
    onPrimaryContainer = InkWhite,
    secondary = InkSoftWhite,
    onSecondary = CanvasBlack,
    secondaryContainer = SurfacePressed,
    onSecondaryContainer = InkWhite,
    background = CanvasBlack,
    onBackground = InkSoftWhite,
    surface = RailDark,
    onSurface = InkSoftWhite,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = RuleGray,
    outlineVariant = RuleStrong
)

@Composable
fun LichViecTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeScheme,
        typography = EditorialTypography,
        content = content
    )
}
