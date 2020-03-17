package com.transcend.otg.utilities

import android.content.Context
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class FileFactory{
    private val mNotificationList: MutableList<Int> = ArrayList()

    companion object {
        private val mMute = Object()
        private var INSTANCE: FileFactory? = null
        fun getInstance(): FileFactory {
            if (INSTANCE == null) {
                synchronized(mMute) {
                    INSTANCE = FileFactory()
                }
            }
            return INSTANCE!!
        }
    }

    fun getNotificationID(): Int {  //統一管理Notification ID
        var id = 1
        if (mNotificationList.size > 0) {
            val value = mNotificationList.get(mNotificationList.size - 1)
            id = value + 1
            mNotificationList.add(id)
        } else {
            mNotificationList.add(id)
        }
        return id
    }

    fun releaseNotificationID(id: Int) {
        mNotificationList.remove(id)
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

    fun isLocalPath(path: String): Boolean{
        return path.startsWith(Constant.LOCAL_ROOT)
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

    fun getSDCardFileName(context: Context): ArrayList<String>? {
        val mPath = getSdCardPath(context)
        val sdName = ArrayList<String>()
        if (mPath != null) {
            val dir = File(mPath)
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isHidden) continue
                    val name = file.name
                    sdName.add(name)
                }
            }
        }
        return sdName
    }

    fun doFileNameCompare(tmpDFile: kotlin.Array<DocumentFile>, tmpFile: ArrayList<String>): Boolean {
        var fileCount = 0
        for (fi in tmpFile.indices) {
            val name = tmpFile[fi]
            for (df in tmpDFile.indices) {
                if (name == tmpDFile[df].name) {
                    fileCount++
                    break
                }
            }
        }
        return if (fileCount == tmpFile.size) {
            true
        } else {
            false
        }
    }

    fun isSamsungStyle(mContext: Context): Boolean {
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var storageVolumeClazz: Class<*>? = null
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            var getVolumeList: Method? = null
            var getSubSystem: Method? = null
            try {
                getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                getSubSystem = storageVolumeClazz.getMethod("getSubSystem")
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                return false
            }
            val result = getVolumeList.invoke(mStorageManager)
            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val subSystem = getSubSystem.invoke(storageVolumeElement) as String
                return true
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return false
    }

    fun getSamsungStyleOuterStoragePath(mContext: Context): String? {
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var storageVolumeClazz: Class<*>? = null
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            var getVolumeList: Method? = null
            var getPath: Method? = null
            var isRemovable: Method? = null
            var getSubSystem: Method? = null
            try {
                getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                getPath = storageVolumeClazz.getMethod("getPath")
                isRemovable = storageVolumeClazz.getMethod("isRemovable")
                getSubSystem = storageVolumeClazz.getMethod("getSubSystem")
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
            val result = getVolumeList!!.invoke(mStorageManager)
            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val path = getPath!!.invoke(storageVolumeElement) as String
                val removable = isRemovable!!.invoke(storageVolumeElement) as Boolean
                val subSystem = getSubSystem!!.invoke(storageVolumeElement) as String
                if (removable) {
                    return if (subSystem.contains("sd")) path else continue
                }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }
}