package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.app_mensagem.ui.theme.AppColorTheme
import com.example.app_mensagem.ui.theme.ThemePreferences

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ThemePreferences(application)

    private val _selectedTheme = mutableStateOf(prefs.getTheme())
    val selectedTheme: State<AppColorTheme> = _selectedTheme

    fun setTheme(theme: AppColorTheme) {
        prefs.saveTheme(theme)
        _selectedTheme.value = theme
    }
}

