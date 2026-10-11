package com.payfriend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    secondary = Teal800,
    tertiary = Amber500,
    background = SurfaceLight,
    surface = SurfaceLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Teal100,
    onPrimary = Color.Black,
    secondary = Teal600,
    tertiary = Amber500,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = Color(0xFFF2B8B5)
)

@Composable
fun PayFriendTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PayFriendTypography,
        content = content
    )
}
