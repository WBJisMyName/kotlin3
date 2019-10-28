package com.transcend.otg.utilities

import android.os.Environment

object Constant{
    var DropDownMainTitle = ""
    val LOCAL_ROOT = Environment.getExternalStorageDirectory().absolutePath
    var SD_ROOT = ""
    var Storage_Root_Path = "Storage/"
    var LocalBrowserMainPageTitle = SystemUtil().getDeviceName()
    var SDBrowserMainPageTitle = "SD Card"

    val TYPE_DIR = 0
    val TYPE_IMAGE = 1
    val TYPE_MUSIC = 2
    val TYPE_VIDEO = 3
    val TYPE_DOC = 4
    val TYPE_OTHERS = 5

    //用以判斷是否已讀取過，因為有件資料庫不須重新讀取
    var hasLoadedTab: MutableList<Boolean> = mutableListOf(true, false, false, false, false)   //對應TYPE，分別為全部(此處理論上不會用到)、圖片、音樂、影片、文件

    val SORT_BY_DATE = 0
    val SORT_BY_NAME = 1
    val SORT_BY_SIZE = 2
    val SORT_ORDER_AS = 0
    val SORT_ORDER_DES = 1

    val thumbnailCacheTail = "&thumbnail"

    fun getDeviceName():String{
        var deviceName = android.os.Build.MODEL
        return deviceName
    }

    val onHOMEPERMISSIONS_REQUEST_WRITE_STORAGE = 86
}
