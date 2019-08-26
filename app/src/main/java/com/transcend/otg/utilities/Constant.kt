package com.transcend.otg.utilities

import android.os.Environment

object Constant{
    var DropDownMainTitle = ""
    val LOCAL_ROOT = Environment.getExternalStorageDirectory().absolutePath
    public var SD_ROOT = ""
    var Storage_Root_Path = "Storage/"

    val TYPE_DIR = 0
    val TYPE_IMAGE = 1
    val TYPE_MUSIC = 2
    val TYPE_VIDEO = 3
    val TYPE_OTHERS = 4

    val thumbnailCacheTail = "&thumbnail"

    fun getDeviceName():String{
        var deviceName = android.os.Build.MODEL;
        return deviceName
    }
}
