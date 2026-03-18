package com.example.app_mensagem.ui.theme

import android.content.Context

class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun saveTheme(theme: AppColorTheme) {
        prefs.edit().putString(KEY_THEME, theme.key).apply()
    }

    fun getTheme(): AppColorTheme {
        return AppColorTheme.fromKey(prefs.getString(KEY_THEME, AppColorTheme.BLUE.key))
    }

    companion object {
        private const val KEY_THEME = "selected_theme"
    }
}

