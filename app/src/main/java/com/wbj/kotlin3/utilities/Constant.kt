package com.wbj.kotlin3.utilities

import android.os.Environment

object Constant{
    val LOCAL_ROOT = Environment.getExternalStorageDirectory().absolutePath

    val TYPE_DIR = 0
    val TYPE_IMAGE = 1
    val TYPE_MUSIC = 2
    val TYPE_VIDEO = 3
    val TYPE_OTHERS = 4

    val thumbnailCacheTail = "&thumbnail"
}
