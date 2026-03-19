package com.example.app_mensagem.ui.theme

import androidx.compose.ui.graphics.Color

data class AppThemePalette(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color
)

object AppThemePalettes {
    val Blue = AppThemePalette(
        primary = Color(0xFF2196F3),
        primaryContainer = Color(0xFFE3F2FD),
        secondary = Color(0xFF1E88E5),
        background = Color(0xFFF5F5F5),
        surface = Color(0xFFFFFFFF)
    )

    val Red = AppThemePalette(
        primary = Color(0xFFC62828),
        primaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE53935),
        background = Color(0xFFFFF8F7),
        surface = Color(0xFFFFFFFF)
    )

    val Green = AppThemePalette(
        primary = Color(0xFF2E7D32),
        primaryContainer = Color(0xFFD8F5D3),
        secondary = Color(0xFF43A047),
        background = Color(0xFFF7FCF7),
        surface = Color(0xFFFFFFFF)
    )

    val Yellow = AppThemePalette(
        primary = Color(0xFFF9A825),
        primaryContainer = Color(0xFFFFF0B3),
        secondary = Color(0xFFFBC02D),
        background = Color(0xFFFFFDF5),
        surface = Color(0xFFFFFFFF)
    )

    val Purple = AppThemePalette(
        primary = Color(0xFF6A1B9A),
        primaryContainer = Color(0xFFF1D9FF),
        secondary = Color(0xFF8E24AA),
        background = Color(0xFFFCF8FF),
        surface = Color(0xFFFFFFFF)
    )

    val Pink = AppThemePalette(
        primary = Color(0xFFC2185B),
        primaryContainer = Color(0xFFFFD9E8),
        secondary = Color(0xFFD81B60),
        background = Color(0xFFFFF7FA),
        surface = Color(0xFFFFFFFF)
    )
}

enum class AppColorTheme(val key: String) {
    BLUE("blue"),
    RED("red"),
    GREEN("green"),
    YELLOW("yellow"),
    PURPLE("purple"),
    PINK("pink");

    companion object {
        fun fromKey(value: String?): AppColorTheme {
            return entries.firstOrNull { it.key == value } ?: BLUE
        }
    }
}

fun AppColorTheme.toPalette(): AppThemePalette {
    return when (this) {
        AppColorTheme.BLUE -> AppThemePalettes.Blue
        AppColorTheme.RED -> AppThemePalettes.Red
        AppColorTheme.GREEN -> AppThemePalettes.Green
        AppColorTheme.YELLOW -> AppThemePalettes.Yellow
        AppColorTheme.PURPLE -> AppThemePalettes.Purple
        AppColorTheme.PINK -> AppThemePalettes.Pink
    }
}
