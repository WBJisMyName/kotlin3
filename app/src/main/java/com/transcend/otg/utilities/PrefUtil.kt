package com.transcend.otg.utilities

import android.content.Context

object PrefUtil {

    val KEY_BOOLEAN = "key access boolean"
    val KEY_INTEGER = "key access integer"
    val KEY_STRING = "key access string"

    fun write(context: Context, name: String, key: String, b: Boolean) {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val ed = sp.edit()
        ed.putBoolean(key, b)
        ed.apply()
    }

    fun write(context: Context, name: String, key: String, id: Int) {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val ed = sp.edit()
        ed.putInt(key, id)
        ed.apply()
    }

    fun write(context: Context, name: String, key: String, data: String) {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val ed = sp.edit()
        ed.putString(key, data)
        ed.apply()
    }

    fun read(context: Context, name: String, key: String, def: Boolean): Boolean {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getBoolean(key, def)
    }

    fun read(context: Context, name: String, key: String, def: Int): Int {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getInt(key, def)
    }

    fun read(context: Context, name: String, key: String, data: String): String? {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getString(key, data)
    }

    fun clear(context: Context, name: String) {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val ed = sp.edit()
        ed.clear().apply()
    }
}
