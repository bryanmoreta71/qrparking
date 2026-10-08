package com.qrparking.app

import android.content.Context

/** Configuración guardada en el teléfono. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("qrparking", Context.MODE_PRIVATE)

    var url: String
        get() = sp.getString(KEY_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_URL, value).apply()

    var selector: String
        get() = sp.getString(KEY_SELECTOR, "") ?: ""
        set(value) = sp.edit().putString(KEY_SELECTOR, value).apply()

    var autoSubmit: Boolean
        get() = sp.getBoolean(KEY_SUBMIT, false)
        set(value) = sp.edit().putBoolean(KEY_SUBMIT, value).apply()

    var digitsOnly: Boolean
        get() = sp.getBoolean(KEY_DIGITS, false)
        set(value) = sp.edit().putBoolean(KEY_DIGITS, value).apply()

    private companion object {
        const val KEY_URL = "url"
        const val KEY_SELECTOR = "selector"
        const val KEY_SUBMIT = "auto_submit"
        const val KEY_DIGITS = "digits_only"
    }
}
