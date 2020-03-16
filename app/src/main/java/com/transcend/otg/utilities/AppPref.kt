package com.transcend.otg.utilities

import android.content.Context
import com.transcend.otg.adapter.RecyclerViewAdapter

object AppPref {

    val SD_KEY = "String sd key"
    val SORT_BY = "Int sortBy key"
    val SORT_ORDER = "Int sortOrder key"
    val View_Type = "String view type"

    fun setSDKey(context: Context?, value: String) {
        if (context == null)
            return
        PrefUtil.write(context, SD_KEY, PrefUtil.KEY_STRING, value)
    }

    fun getSDKey(context: Context?): String {
        if (context == null)
            return ""
        return PrefUtil.read(context, SD_KEY, PrefUtil.KEY_STRING, "") ?: ""
    }

    fun setViewType(context: Context?, type: Int, value: Int) {
        if (context == null)
            return
        val key = View_Type + "-" + type
        PrefUtil.write(context, key, PrefUtil.KEY_INTEGER, value)
    }

    fun getViewType(context: Context?, type: Int): Int {
        if (context == null)
            return RecyclerViewAdapter.List
        val key = View_Type + "-" + type
        var default = RecyclerViewAdapter.List
        when(type){
            Constant.TYPE_IMAGE, Constant.TYPE_MUSIC, Constant.TYPE_VIDEO -> {
                default = RecyclerViewAdapter.Grid
            }
        }
        return PrefUtil.read(context, key, PrefUtil.KEY_INTEGER, default)
    }

    fun setSortBy(context: Context?, type: Int, value: Int){
        if (context == null)
            return
        val key = SORT_BY + "-" + type
        PrefUtil.write(context, key, PrefUtil.KEY_INTEGER, value)
    }

    fun getSortBy(context: Context?, type: Int): Int{
        if (context == null)
            return Constant.SORT_BY_DATE
        val key = SORT_BY + "-" + type
        return PrefUtil.read(context, key, PrefUtil.KEY_INTEGER, Constant.SORT_BY_DATE)
    }

    fun setSortOrder(context: Context?, type: Int, value: Int){
        if (context == null)
            return
        val key = SORT_ORDER + "-" + type
        PrefUtil.write(context, key, PrefUtil.KEY_INTEGER, value)
    }

    fun getSortOrder(context: Context?, type: Int): Int{
        if (context == null)
            return Constant.SORT_ORDER_AS
        val key = SORT_ORDER + "-" + type
        return PrefUtil.read(context, key, PrefUtil.KEY_INTEGER, Constant.SORT_ORDER_AS)
    }
}