package com.transcend.otg

import android.content.Context
import com.transcend.otg.utilities.PrefUtil

object AppPref {

    val SD_KEY = "STRING sd key"

    fun setSDKey(context: Context?, value: String) {
        if (context == null)
            return
        PrefUtil.write(context, SD_KEY, PrefUtil.KEY_STRING, value)
    }

    fun getSdKey(context: Context?): String {
        var key = ""
        if (context != null) {
            key = PrefUtil.read(context, SD_KEY, PrefUtil.KEY_STRING, "") ?: ""
        }
        return key
    }
}