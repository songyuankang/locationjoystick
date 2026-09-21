package com.locationjoystick.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark "deep-space blue" tech palette — the app's primary theme.
val LjBg = Color(0xFF08111F)
val LjSurface = Color(0xFF0E192A)
val LjSurfaceVariant = Color(0xFF111D30)
val LjCard = Color(0xFF142033)
val LjText = Color(0xFFF4F7FC)
val LjTextSecondary = Color(0xFF94A3B8)
val LjTextWeak = Color(0xFF65758B)
val LjAccent = Color(0xFF3284FF)
val LjAccentSoft = Color(0xFF65A5FF)
val LjAccentContainer = Color(0xFF1A2C47)
val LjError = Color(0xFFFF647C)
val LjErrorContainer = Color(0xFF3A1A26)
val LjSuccess = Color(0xFF3ED6A3)
val LjSuccessContainer = Color(0xFF14362C)
val LjInactive = Color(0xFF65758B)
val LjWarning = Color(0xFFFFB35C)
val LjWarningContainer = Color(0xFF3A2A17)
val LjDivider = Color(0x14FFFFFF)
val LjDividerStrong = Color(0x1AFFFFFF)

// Light theme — high-contrast variant for sunny/outdoor readability.
val LjLightBg = Color(0xFFF5F8FC)
val LjLightSurface = Color(0xFFFFFFFF)
val LjLightSurfaceVariant = Color(0xFFE8EEF6)
val LjLightText = Color(0xFF15202E)
val LjLightTextSecondary = Color(0xFF55677C)
val LjLightAccent = Color(0xFF1F6AE0)
val LjLightAccentContainer = Color(0xFFDCEAFF)
val LjLightOutlineVariant = Color(0xFFD3DDE9)

object LjMapColors {
    val ActiveButton = Color(0xFF3ED6A3)
    val PositionBlue = Color(0xFF3284FF)
    val RouteLine = Color(0xFF65A5FF)
    val PointStroke = Color(0xFFFFFFFF)
    val PendingTap = Color(0xFF3ED6A3)
}

val LjDarkColorScheme =
    darkColorScheme(
        primary = LjAccent,
        onPrimary = LjText,
        primaryContainer = LjAccentContainer,
        onPrimaryContainer = LjAccentSoft,
        secondary = LjAccentSoft,
        onSecondary = LjBg,
        secondaryContainer = LjAccentContainer,
        onSecondaryContainer = LjAccentSoft,
        tertiary = LjAccentSoft,
        onTertiary = LjBg,
        tertiaryContainer = LjSurfaceVariant,
        onTertiaryContainer = LjAccentSoft,
        error = LjError,
        onError = LjText,
        errorContainer = LjErrorContainer,
        onErrorContainer = LjError,
        background = LjBg,
        onBackground = LjText,
        surface = LjSurface,
        onSurface = LjText,
        surfaceVariant = LjSurfaceVariant,
        onSurfaceVariant = LjTextSecondary,
        outline = LjTextWeak,
        outlineVariant = LjDividerStrong,
        inverseSurface = LjText,
        inverseOnSurface = LjBg,
        inversePrimary = LjLightAccent,
        scrim = Color(0xB3000000),
    )

val LjLightColorScheme =
    lightColorScheme(
        primary = LjLightAccent,
        onPrimary = Color.White,
        primaryContainer = LjLightAccentContainer,
        onPrimaryContainer = LjLightAccent,
        secondary = LjLightAccent,
        onSecondary = Color.White,
        secondaryContainer = LjLightAccentContainer,
        onSecondaryContainer = LjLightAccent,
        tertiary = LjLightAccent,
        onTertiary = Color.White,
        tertiaryContainer = LjLightSurfaceVariant,
        onTertiaryContainer = LjLightAccent,
        error = LjError,
        onError = Color.White,
        errorContainer = Color(0xFFFFDDE3),
        onErrorContainer = Color(0xFF9E1B32),
        background = LjLightBg,
        onBackground = LjLightText,
        surface = LjLightSurface,
        onSurface = LjLightText,
        surfaceVariant = LjLightSurfaceVariant,
        onSurfaceVariant = LjLightTextSecondary,
        surfaceContainer = LjLightSurface,
        surfaceContainerHigh = LjLightSurface,
        outline = LjLightTextSecondary,
        outlineVariant = LjLightOutlineVariant,
        inverseSurface = LjLightText,
        inverseOnSurface = LjLightBg,
        inversePrimary = LjAccent,
        scrim = Color(0x80000000),
    )
