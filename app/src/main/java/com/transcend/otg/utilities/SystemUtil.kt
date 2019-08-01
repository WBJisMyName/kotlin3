package com.transcend.otg.utilities

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.transcend.otg.R
import java.io.File
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class SystemUtil {

    fun getSDLocation(context: Context): String? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val TYPE_PUBLIC = 0
            var file: File? = null
            var sdPath: String? = null
            val mStorageManager = context.getSystemService(StorageManager::class.java)
            var mVolumeInfo: Class<*>? = null
            var mDiskInfo: Class<*>? = null
            try {
                mDiskInfo = Class.forName("android.os.storage.DiskInfo")
                val method_isSd = mDiskInfo!!.getMethod("isSd")
                val method_isUsb = mDiskInfo.getMethod("isUsb")

                mVolumeInfo = Class.forName("android.os.storage.VolumeInfo")
                val getVolumes = mStorageManager.javaClass.getMethod("getVolumes")
                val volType = mVolumeInfo!!.getMethod("getType")
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

        } else {
            val mStorageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            var storageVolumeClazz: Class<*>? = null
            try {
                storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
                var getVolumeList: Method? = null
                var getPath: Method? = null
                var isRemovable: Method? = null
                var getState: Method? = null
                var getSubSystem: Method? = null
                try {
                    getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                    getPath = storageVolumeClazz!!.getMethod("getPath")
                    isRemovable = storageVolumeClazz.getMethod("isRemovable")
                    getState = storageVolumeClazz.getMethod("getState")
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
                    val mState = getState!!.invoke(storageVolumeElement) as String
                    var subSystem: String? = null
                    if (Build.BRAND == context.resources.getString(R.string.samsung) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        subSystem = getSubSystem!!.invoke(storageVolumeElement) as String
                    if (removable && path != null && mState == "mounted") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Build.BRAND == context.resources.getString(R.string.samsung)) {
                                return if (subSystem != null && subSystem.contains("sd"))
                                    path
                                else
                                    continue
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    val description =
                                        (storageVolumeElement as StorageVolume).getDescription(context).toLowerCase()
                                    return if (description.contains("sd") && !description.contains("usb")) {
                                        path
                                    } else if (description.contains("usb") && !description.contains("sd")) {
                                        continue
                                    } else {
                                        path
                                    }
                                } else {
                                    return path
                                }
                            }
                        } else if (path.toLowerCase().contains("sd")) {
                            return path
                        }
                    }
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }

        }
        return null
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }
}