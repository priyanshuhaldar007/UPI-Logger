package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PolishDarkPrimary,
    onPrimary = PolishPrimaryDark,
    primaryContainer = PolishDarkPrimaryContainer,
    onPrimaryContainer = PolishPrimaryContainer,
    secondary = PolishDarkPrimary,
    onSecondary = PolishPrimaryDark,
    secondaryContainer = PolishDarkPrimaryContainer,
    onSecondaryContainer = PolishPrimaryContainer,
    tertiary = PolishMergedText,
    onTertiary = Color.White,
    background = PolishDarkBackground,
    surface = PolishDarkSurface,
    surfaceVariant = PolishDarkSurfaceVariant,
    onBackground = PolishDarkOnSurface,
    onSurface = PolishDarkOnSurface,
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = PolishDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = Color.White,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishPrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = PolishPrimaryContainer,
    onSecondaryContainer = PolishOnPrimaryContainer,
    tertiary = PolishMergedText,
    onTertiary = Color.White,
    background = PolishBackground,
    surface = PolishSurface,
    surfaceVariant = PolishSurfaceVariant,
    onBackground = PolishTextPrimary,
    onSurface = PolishTextPrimary,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishCardBorder
)

@Composable
fun UpiNoteLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = UpiNoteLoggerTheme(darkTheme, dynamicColor, content)
