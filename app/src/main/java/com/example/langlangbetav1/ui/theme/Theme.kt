package com.example.langlangbetav1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LangLangColorScheme = darkColorScheme(
    primary        = PurplePrimary,
    secondary      = RedMarker,
    background     = DarkBackground,
    surface        = SurfaceDark,
    onPrimary      = TextPrimary,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
)

private val LangLangTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 40.sp,
        color      = TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        color      = TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 18.sp,
        color      = TextPrimary,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp,
        color      = TextSecondary,
    ),
)

@Composable
fun LangLangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LangLangColorScheme,
        typography  = LangLangTypography,
        content     = content,
    )
}

