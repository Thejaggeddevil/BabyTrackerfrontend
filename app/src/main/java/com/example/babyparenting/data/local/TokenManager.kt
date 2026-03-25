package com.example.babyparenting.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * JWT token aur user info phone mein save karta hai.
 * Login ke baad token yahan store hota hai.
 * Logout pe sab clear ho jaata hai.
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(email: String, name: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getName(): String  = prefs.getString(KEY_NAME, "")  ?: ""

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME  = "user_name"
    }
}