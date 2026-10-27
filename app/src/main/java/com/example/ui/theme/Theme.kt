package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BotanicalGreenDarkPrimary,
    onPrimary = BotanicalGreenDarkOnPrimary,
    primaryContainer = BotanicalGreenDarkContainer,
    onPrimaryContainer = BotanicalGreenDarkOnContainer,
    secondary = HarvestAmberDarkSecondary,
    onSecondary = HarvestAmberDarkOnSecondary,
    secondaryContainer = HarvestAmberDarkContainer,
    onSecondaryContainer = HarvestAmberDarkOnContainer,
    tertiary = SlateBlueDarkTertiary,
    onTertiary = SlateBlueDarkOnTertiary,
    tertiaryContainer = SlateBlueDarkContainer,
    onTertiaryContainer = SlateBlueDarkOnContainer,
    background = MarketDarkBackground,
    onBackground = MarketDarkOnSurface,
    surface = MarketDarkSurface,
    onSurface = MarketDarkOnSurface,
    surfaceVariant = MarketDarkSurfaceVariant,
    onSurfaceVariant = MarketDarkOnSurfaceVariant,
    error = LossCoralRed,
    errorContainer = LossCoralContainer
)

private val LightColorScheme = lightColorScheme(
    primary = BotanicalGreenPrimary,
    onPrimary = BotanicalGreenOnPrimary,
    primaryContainer = BotanicalGreenContainer,
    onPrimaryContainer = BotanicalGreenOnContainer,
    secondary = HarvestAmberSecondary,
    onSecondary = HarvestAmberOnSecondary,
    secondaryContainer = HarvestAmberContainer,
    onSecondaryContainer = HarvestAmberOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = SlateBlueOnTertiary,
    tertiaryContainer = SlateBlueContainer,
    onTertiaryContainer = SlateBlueOnContainer,
    background = MarketCreamBackground,
    onBackground = MarketOnSurface,
    surface = MarketCreamSurface,
    onSurface = MarketOnSurface,
    surfaceVariant = MarketSurfaceVariant,
    onSurfaceVariant = MarketOnSurfaceVariant,
    error = LossCoralRed,
    errorContainer = LossCoralContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
