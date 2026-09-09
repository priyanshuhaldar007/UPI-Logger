package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

// Composition local for accessibility Reduce Motion setting
val LocalReduceMotion = compositionLocalOf { false }

object AppleSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}

object AppleRadius {
    val badge = 6.dp
    val chip = 12.dp
    val card = 20.dp
    val sheet = 28.dp
}

object AppleMotion {
    // Standard interactive transitions (sheets, cards expanding, button presses)
    fun <T> standardSpring(reduceMotion: Boolean = false): FiniteAnimationSpec<T> {
        return if (reduceMotion) {
            tween(durationMillis = 150)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    }

    // Functional transitions that shouldn't visibly overshoot (text field focus rings, state toggles)
    fun <T> functionalSpring(reduceMotion: Boolean = false): FiniteAnimationSpec<T> {
        return if (reduceMotion) {
            tween(durationMillis = 120)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    }

    // Responsive swipe gesture spring back
    fun <T> swipeSpring(reduceMotion: Boolean = false): FiniteAnimationSpec<T> {
        return if (reduceMotion) {
            tween(durationMillis = 180)
        } else {
            spring(
                dampingRatio = 0.82f,
                stiffness = 380f
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = UpiNoteLoggerTheme(darkTheme, dynamicColor, content)

