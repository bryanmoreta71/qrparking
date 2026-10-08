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

    var username: String
        get() = sp.getString(KEY_USER, "") ?: ""
        set(value) = sp.edit().putString(KEY_USER, value).apply()

    /** Se guarda cifrada con [Crypto]. */
    var password: String
        get() = Crypto.decrypt(sp.getString(KEY_PASS, "") ?: "")
        set(value) = sp.edit().putString(KEY_PASS, Crypto.encrypt(value)).apply()

    var autoLogin: Boolean
        get() = sp.getBoolean(KEY_AUTO_LOGIN, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_LOGIN, value).apply()

    private companion object {
        const val KEY_URL = "url"
        const val KEY_SELECTOR = "selector"
        const val KEY_SUBMIT = "auto_submit"
        const val KEY_DIGITS = "digits_only"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
        const val KEY_AUTO_LOGIN = "auto_login"
    }
}
