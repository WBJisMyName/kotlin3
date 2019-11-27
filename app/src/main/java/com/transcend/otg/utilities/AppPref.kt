package com.transcend.otg.utilities

import android.content.Context

object AppPref {

    val SD_KEY = "STRING sd key"
    val SORT_BY = "Int sortBy key"
    val SORT_ORDER = "Int sortOrder key"

    fun setSDKey(context: Context, value: String) {
        PrefUtil.write(context,
            SD_KEY, PrefUtil.KEY_STRING, value)
    }

    fun getSdKey(context: Context): String {
        return PrefUtil.read(context,
            SD_KEY, PrefUtil.KEY_STRING, "") ?: ""
    }

    fun setSortBy(context: Context, type: Int, value: Int){
        val key = SORT_BY + "-" + type
        PrefUtil.write(context, key, PrefUtil.KEY_INTEGER, value)
    }

    fun getSortBy(context: Context, type: Int): Int{
        val key = SORT_BY + "-" + type
        return PrefUtil.read(context, key, PrefUtil.KEY_INTEGER, Constant.SORT_BY_DATE)
    }

    fun setSortOrder(context: Context, type: Int, value: Int){
        val key = SORT_ORDER + "-" + type
        PrefUtil.write(context, key, PrefUtil.KEY_INTEGER, value)
    }

    fun getSortOrder(context: Context, type: Int): Int{
        val key = SORT_ORDER + "-" + type
        return PrefUtil.read(context, key, PrefUtil.KEY_INTEGER, Constant.SORT_ORDER_AS)
    }
}