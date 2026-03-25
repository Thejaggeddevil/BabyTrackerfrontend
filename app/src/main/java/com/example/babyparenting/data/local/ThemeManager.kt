package com.example.babyparenting.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Theme Manager — persists user's theme choice across app restarts.
 *
 * Default: Light mode (false).
 * Once user changes to dark mode → it stays dark even after app is killed and reopened.
 * We NEVER reset to light automatically.
 *
 * Usage in MainActivity / App entry point:
 *   val themeManager = remember { ThemeManager(context) }
 *   var isDark by remember { mutableStateOf(themeManager.isDarkMode()) }
 *
 *   // When user toggles:
 *   themeManager.setDarkMode(!isDark)
 *   isDark = !isDark
 */
class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the saved theme preference.
     * Default = false (light mode) for first-time users.
     * After user changes it → their choice is remembered forever.
     */
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK, DEFAULT_IS_DARK)

    /**
     * Saves the user's theme choice persistently.
     * This survives app kills, process death, and phone restarts.
     */
    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, dark).apply()
    }

    /** Toggle convenience */
    fun toggleDarkMode(): Boolean {
        val newValue = !isDarkMode()
        setDarkMode(newValue)
        return newValue
    }

    companion object {
        private const val PREFS_NAME   = "app_theme_prefs"   // Separate prefs file for theme
        private const val KEY_DARK     = "is_dark_mode"
        private const val DEFAULT_IS_DARK = false             // Light mode by default
    }
}