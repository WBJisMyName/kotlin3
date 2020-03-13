package com.transcend.otg.utilities

import android.os.Environment

object Constant{
    var DropDownMainTitle = ""
    val LOCAL_ROOT = Environment.getExternalStorageDirectory().absolutePath
    var SD_ROOT: String? = null
    var OTG_ROOT: String? = null
    var Storage_Device_Root = "Storage/"
    var PhoneName = SystemUtil().getDeviceName()

    val TYPE_DIR = 0
    val TYPE_IMAGE = 1
    val TYPE_MUSIC = 2
    val TYPE_VIDEO = 3
    val TYPE_DOC = 4
    val TYPE_OTHERS = 5

    val STORAGEMODE_LOCAL = 0
    val STORAGEMODE_SD = 1
    val STORAGEMODE_OTG = 2

    //用以判斷是否已讀取過，因為有件資料庫不須重新讀取
    var localMediaScanState: MutableList<ScanState> = mutableListOf(ScanState.SCANNED,   //全部(此處理論上不會用到)
        ScanState.NONE, //圖片
        ScanState.NONE, //音樂
        ScanState.NONE, //影片
        ScanState.NONE) //文件
    var sdMediaScanState: MutableList<ScanState> = mutableListOf(ScanState.SCANNED,   //全部(此處理論上不會用到)
        ScanState.NONE, //圖片
        ScanState.NONE, //音樂
        ScanState.NONE, //影片
        ScanState.NONE) //文件
    enum class ScanState{
        NONE, SCANNING, SCANNED
    }

    val SORT_BY_DATE = 0
    val SORT_BY_NAME = 1
    val SORT_BY_SIZE = 2
    val SORT_ORDER_AS = 0
    val SORT_ORDER_DES = 1

    val thumbnailCacheTail = "&thumbnail"

    val onHOMEPERMISSIONS_REQUEST_WRITE_STORAGE = 86
}
