package com.example.myapplication.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val OrangeAccent = Color(0xFFFF6A00)
val AMOLEDBlack = Color(0xFF000000)
val DarkGray = Color(0xFF1C1C1C)
val LightGray = Color(0xFFB0B0B0)

val WearColorScheme: ColorScheme = ColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.Black,
    secondary = DarkGray,
    onSecondary = Color.White,
    tertiary = OrangeAccent.copy(alpha = 0.5f),
    onTertiary = Color.White,
    background = AMOLEDBlack,
    onBackground = Color.White,
    surfaceContainer = DarkGray,
    onSurface = Color.White,
    onSurfaceVariant = LightGray,
    outline = Color.DarkGray
)
