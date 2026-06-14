package com.example.myapplication.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import android.graphics.Typeface

val RoundedFontFamily = FontFamily(Typeface.create("sans-serif-rounded", Typeface.NORMAL))

val WearTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
    ),
)
