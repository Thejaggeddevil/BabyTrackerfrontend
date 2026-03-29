package com.example.babyparenting.data.local

import android.content.Context
import java.util.UUID

object UserManager {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var userId = prefs.getString(KEY_USER_ID, null)

        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }

        return userId
    }
}