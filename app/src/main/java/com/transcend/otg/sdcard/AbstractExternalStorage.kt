package com.transcend.otg.sdcard

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.transcend.otg.BuildConfig

import java.io.File

abstract class AbstractExternalStorage(protected val context: Context) {

    //The followings are waiting for override!
    open val isWritePermissionNotGranted: Boolean
        get() = false

    open fun getSDFileUri(path: String): Uri {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", File(path))
    }

    open fun isWritePermissionRequired(vararg path: String): Boolean {
        return false
    }

    fun handleWriteOperationFailed() {

    }

    open fun onActivityResult(activity: AppCompatActivity, data: Intent) {

    }
}
