package com.transcend.otg.utilities

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

object PermissionHandle {

    val REQUEST_ALL = 0
    val REQUEST_ACCESS_FINE_LOCATION = 1
    val REQUEST_READ_WRITE_EXTERNAL_STORAGE = 2

    //*取得權限後的動作
    enum class ActionAfterGettingPermission {
        Nothing, Download, Share, Upload
    }

    private fun isLocationPermissionGranted(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
        else
            true
    }

    fun isReadPermissionGranted(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) === PackageManager.PERMISSION_GRANTED
        else
            true
    }

    fun requestAllPermission(activity: Activity) {
        val permissionList = ArrayList<String>()

        if (!isReadPermissionGranted(activity)) {
            permissionList.add(READ_EXTERNAL_STORAGE)
            permissionList.add(WRITE_EXTERNAL_STORAGE)
        }

        if (permissionList.size == 0)
            return

        val permissionArray = permissionList.toTypedArray()

        //未取得權限，向使用者要求允許權限
        ActivityCompat.requestPermissions(activity, permissionArray, REQUEST_ALL)
    }

    fun requestLocationPermission(activity: Activity): Boolean {
        if (!isLocationPermissionGranted(activity)) {
            //未取得權限，向使用者要求允許權限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_FINE_LOCATION
            )

            return false
        }

        return true
    }

    fun requestReadPermission(activity: Activity): Boolean {
        if (!isReadPermissionGranted(activity)) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(activity)
                    .setTitle("錯誤")
                    .setMessage("請至設定調整權限")
                    .setPositiveButton(
                        "前往設定",
                        DialogInterface.OnClickListener { dialog, which ->
                            //引導用戶至設置頁手動授權
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", activity.applicationContext.packageName, null)
                            intent.data = uri
                            activity.startActivity(intent)
                        })
                    .setNegativeButton(
                        "取消",
                        DialogInterface.OnClickListener { dialog, which ->
                            //引導用戶至設置頁手動授權，權限請求失敗
                        })
                    .setOnCancelListener(DialogInterface.OnCancelListener {
                        //引導用戶至設置頁手動授權，權限請求失敗
                    })
                    .show()

                return false
            } else {
                //未取得權限，向使用者要求允許權限
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                    REQUEST_READ_WRITE_EXTERNAL_STORAGE
                )

                return false
            }
        }
        return true
    }
}
