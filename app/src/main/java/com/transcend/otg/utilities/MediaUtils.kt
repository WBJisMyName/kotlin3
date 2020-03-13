package com.transcend.otg.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.transcend.otg.BuildConfig
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import java.io.File

object MediaUtils {
    fun openIn(context: Context?, fileInfo: FileInfo) {
        if (context == null)
            return
        var uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", File(fileInfo.path))
        context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val type: String? = MimeUtil.getMimeType(fileInfo.path)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            if (fileInfo.path.toLowerCase().endsWith("apk")) uri =
                Uri.parse("file://" + fileInfo.path)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        intent.setDataAndType(uri, type)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.couldnotfindappsopen), Toast.LENGTH_LONG).show()
        }
    }

    fun openIn(context: Context?, uri: String) {
        if (context == null)
            return
        context.grantUriPermission(context.packageName, Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val type: String? = MimeUtil.getMimeType(uri)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        intent.setDataAndType(Uri.parse(uri), type)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.couldnotfindappsopen), Toast.LENGTH_LONG).show()
        }
    }
}