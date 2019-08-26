package com.transcend.otg.sdcard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class ExternalStorageController(private val mContext: Context) {
    private val mInstance: AbstractExternalStorage

    private val instance: AbstractExternalStorage   //最低版本設定在23，故必為6.0以上
        get() {
            val instance: AbstractExternalStorage
            val version = Build.VERSION.SDK_INT
            Log.d(TAG, "sdk version: $version")

            instance = ExternalStorageLollipop(mContext)
            return instance
        }

    val isWritePermissionNotGranted: Boolean
        get() = mInstance.isWritePermissionNotGranted

    init {
        mInstance = instance
    }

    fun getSDFileUri(path: String): Uri {
        return mInstance.getSDFileUri(path)
    }

    //The followings are waiting for override
    fun onActivityResult(activity: AppCompatActivity, data: Intent) {
        mInstance.onActivityResult(activity, data)
    }

    fun isWritePermissionRequired(vararg path: String): Boolean {
        return mInstance.isWritePermissionRequired(*path)
    }

    fun handleWriteOperationFailed() {
        mInstance.handleWriteOperationFailed()
    }

    companion object {
        private val TAG = ExternalStorageController::class.java.simpleName
    }
}
