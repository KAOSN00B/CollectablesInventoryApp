package com.lasallecollegevancouver.gameinventoryapp.config

import android.content.Context

// Single place to read/write the public collection code stored in SharedPreferences
object PrefsHelper {

    private const val PREFS_NAME = "collectos_prefs"
    private const val KEY_PUBLIC_CODE = "public_code"

    // Returns the saved public code, or null if onboarding hasn't been completed
    fun getPublicCode(context: Context): String? {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PUBLIC_CODE, null)
    }

    fun savePublicCode(context: Context, code: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PUBLIC_CODE, code)
            .apply()
    }

    fun clearPublicCode(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PUBLIC_CODE)
            .apply()
    }
}
