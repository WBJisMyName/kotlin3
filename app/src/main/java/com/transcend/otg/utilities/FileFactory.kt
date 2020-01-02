package com.transcend.otg.utilities

import android.content.Context
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File
import java.lang.reflect.InvocationTargetException

class FileFactory{
    private var mFileFactory: FileFactory? = null
    private val mMute = Any()
    private val mNotificationList: MutableList<String>

    init {
        mNotificationList = ArrayList()
    }

    fun getInstance(): FileFactory {
        synchronized(mMute) {
            if (mFileFactory == null)
                mFileFactory = FileFactory()
        }
        return mFileFactory!!
    }

    fun getNotificationID(): Int {
        var id = 1
        if (mNotificationList.size > 0) {
            val value = mNotificationList.get(mNotificationList.size - 1)
            id = Integer.parseInt(value) + 1
            mNotificationList.add(Integer.toString(id))
        } else {
            mNotificationList.add(Integer.toString(id))
        }
        return id
    }

    fun releaseNotificationID(id: Int) {
        val value = "" + id
        mNotificationList.remove(value)
    }

    fun getUsedStorageSize(filePath: String): Long {
        try {
            val stat = StatFs(filePath)
            val bytesAvailable = stat.blockSizeLong * stat.blockCountLong
            val bytesLeftAvailable = stat.blockSizeLong * stat.availableBlocksLong
            return bytesAvailable - bytesLeftAvailable
        } catch (e: Exception) {
            return 0
        }
    }

    fun getStorageAllSizeLong(filePath: String): Long {
        try {
            val stat = StatFs(filePath)
            return stat.blockSizeLong * stat.blockCountLong
        } catch (e: Exception) {
            return 0
        }

    }

    fun isSDCardPath(context: Context, path: String): Boolean {
        val location = getSdCardPath(context)
        return location != null && path.contains(location!!)
    }

    fun getSdCardPath(context: Context): String? {
        val TYPE_PUBLIC = 0
        var file: File? = null
        var sdPath: String? = null
        val mStorageManager = context.getSystemService(StorageManager::class.java)
        var mVolumeInfo: Class<*>? = null
        var mDiskInfo: Class<*>? = null
        try {
            mDiskInfo = Class.forName("android.os.storage.DiskInfo")
            val method_isSd = mDiskInfo.getMethod("isSd")
            val method_isUsb = mDiskInfo.getMethod("isUsb")

            mVolumeInfo = Class.forName("android.os.storage.VolumeInfo")
            val getVolumes = mStorageManager!!.javaClass.getMethod("getVolumes")
            val volType = mVolumeInfo.getMethod("getType")
            val isMount = mVolumeInfo.getMethod("isMountedReadable")
            val getPath = mVolumeInfo.getMethod("getPath")
            val getDisk = mVolumeInfo.getMethod("getDisk")

            val mListVolumeinfo = getVolumes.invoke(mStorageManager) as List<Any>

            for (i in mListVolumeinfo.indices) {
                val mType = volType.invoke(mListVolumeinfo[i]) as Int
                var isSd = false
                var isUsb = false
                val diskInfo = getDisk.invoke(mListVolumeinfo[i])

                if (diskInfo != null) {
                    isSd = method_isSd.invoke(diskInfo) as Boolean
                    isUsb = method_isUsb.invoke(diskInfo) as Boolean
                }

                if (mType == TYPE_PUBLIC && isSd && !isUsb) {
                    val misMount = isMount.invoke(mListVolumeinfo[i]) as Boolean
                    if (misMount) {
                        file = getPath.invoke(mListVolumeinfo[i]) as File
                        if (file != null) {
                            sdPath = file.absolutePath
                            return sdPath
                        }
                    }
                }
            }

        } catch (e: ClassNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return null
    }
}