package com.example.myapplication.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val SafetyOrange = Color(0xFFFF6700)
val CrispWhite = Color(0xFFFFFFFF)
val SpaceGray = Color(0xFF242526)
val DeepSpace = Color(0xFF121212)

val WearColorScheme: ColorScheme = ColorScheme(
    primary = SafetyOrange,
    onPrimary = Color.Black,
    secondary = SpaceGray,
    onSecondary = CrispWhite,
    tertiary = SafetyOrange.copy(alpha = 0.5f),
    onTertiary = CrispWhite,
    background = DeepSpace,
    onBackground = CrispWhite,
    surfaceContainer = SpaceGray,
    onSurface = CrispWhite,
    onSurfaceVariant = Color.LightGray,
    outline = SpaceGray
)
