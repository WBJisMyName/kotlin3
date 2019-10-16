package com.transcend.otg.viewmodels

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant

class StartPermissionViewModel(val activity: Activity) : ViewModel(){

    private var myDialog: Dialog? = null

    fun onAllowClick(){
        checkPermission(Constant.onHOMEPERMISSIONS_REQUEST_WRITE_STORAGE)
    }

    private fun checkPermission(whereFrom: Int) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                initDialog(whereFrom)
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), whereFrom)
            }
        } else {//確定拿到權限後

        }
    }

    private fun initDialog(where: Int) {
        val alertDialog = AlertDialog.Builder(activity)
            .setMessage(activity.getString(R.string.runtimepermission))
            .setCancelable(false)
            .setPositiveButton(activity.getText(R.string.confirm),
                DialogInterface.OnClickListener { dialog, which ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        where
                    )
                })
            .setNegativeButton(activity.getText(R.string.cancel),
                DialogInterface.OnClickListener { dialog, which -> })

        myDialog?.dismiss()
        myDialog = alertDialog.show()
    }


}