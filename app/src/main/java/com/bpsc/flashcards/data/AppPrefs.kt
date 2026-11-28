package com.bpsc.flashcards.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLang { EN, UR }

enum class ThemeMode { LIGHT, DARK }

class AppPrefs(context: Context, systemDark: Boolean) {

    private val prefs = context.applicationContext.getSharedPreferences("bpsc_prefs", Context.MODE_PRIVATE)

    private val _lang = MutableStateFlow(readLang())
    val lang: StateFlow<AppLang> = _lang.asStateFlow()

    private val _theme = MutableStateFlow(readTheme(systemDark))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private fun readLang(): AppLang =
        if (prefs.getString(KEY_LANG, "en") == "ur") AppLang.UR else AppLang.EN

    private fun readTheme(systemDark: Boolean): ThemeMode =
        prefs.getString(KEY_THEME, null)?.let { if (it == "dark") ThemeMode.DARK else ThemeMode.LIGHT }
            ?: if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT

    private fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun setLang(lang: AppLang) {
        write(KEY_LANG, if (lang == AppLang.UR) "ur" else "en")
        _lang.value = lang
    }

    fun setLangNow(lang: AppLang) = setLang(lang)

    fun toggleLang() = setLang(if (_lang.value == AppLang.UR) AppLang.EN else AppLang.UR)

    fun toggleTheme() {
        val next = if (_theme.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        write(KEY_THEME, if (next == ThemeMode.DARK) "dark" else "light")
        _theme.value = next
    }

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_THEME = "theme"
    }
}