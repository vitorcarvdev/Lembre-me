package com.vitor.melembre.localweb

import android.content.Context

object LocalPin {
    const val DEFAULT = "0001"

    private const val KEY_PIN = "pin"

    fun isValid(pin: String?): Boolean {
        if (pin == null || pin.length != 4) return false
        return pin.all { it in '0'..'9' }
    }

    fun normalize(stored: String?): String {
        val value = stored?.trim()
        return if (isValid(value)) value!! else DEFAULT
    }

    fun read(context: Context): String = read(SharedPinPreferences(context))

    fun save(context: Context, pin: String): Boolean = save(SharedPinPreferences(context), pin)

    internal fun read(preferences: PinPreferences): String {
        return normalize(preferences.getString(KEY_PIN))
    }

    internal fun save(preferences: PinPreferences, pin: String): Boolean {
        val value = pin.trim()
        if (!isValid(value)) return false
        preferences.putString(KEY_PIN, value)
        return true
    }
}

internal interface PinPreferences {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

private class SharedPinPreferences(context: Context) : PinPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(
        "local_access_pin",
        Context.MODE_PRIVATE,
    )

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
}
