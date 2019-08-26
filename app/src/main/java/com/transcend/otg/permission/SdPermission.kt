package com.transcend.otg.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.transcend.otg.utilities.SystemUtil
import java.util.*

object SdPermission {
    private val TAG = SdPermission::class.java.simpleName

    fun isEditable(context: Context, path: String?): Boolean {
        if (path == null)
            return false
        if (isApplied(context, path)) {
            val doc = SdPermission.toDocumentFile(context, path)
            if (null == doc || !doc!!.exists())
                return false
        }
        return true
    }

    //判斷是否有SD卡
    fun isApplied(context: Context, path: String): Boolean {
        val sd_root = SystemUtil().getSDLocation(context) ?: return false
        return if (path.startsWith(sd_root)) true else false
    }

    fun openPicker(context: Context, reqCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        if (context is Activity) {
            context.startActivityForResult(intent, reqCode)
            Log.e(TAG, "[Open Picker]")
        }
    }

    fun add(context: Context, treeUri: Uri) {
        val resolver = context.contentResolver
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        Log.e(TAG, "[Add] treeUri: $treeUri")
    }

    fun remove(context: Context, treeUri: Uri) {
        val resolver = context.contentResolver
        resolver.releasePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        Log.e(TAG, "[Remove] treeUri: $treeUri")
    }

    fun clear(context: Context) {
        val resolver = context.contentResolver
        val perms = resolver.persistedUriPermissions
        for (perm in perms) remove(context, perm.uri)
    }

    fun toDocumentFile(context: Context, path: String): DocumentFile? {
        val hierarchy = Arrays.asList(*path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        val resolver = context.contentResolver
        val perms = resolver.persistedUriPermissions
        if (!perms.isEmpty()) {
            for (perm in perms) {
                var doc = DocumentFile.fromTreeUri(context, perm.uri)
                if (doc!!.exists()) {
                    var name = doc!!.getName()
                    if (hierarchy.contains(name)) {
                        val start = hierarchy.indexOf(name) + 1
                        val size = hierarchy.size
                        for (i in start until size) {
                            name = hierarchy[i]
                            doc = doc!!.findFile(name)
                            if (doc == null) break
                        }
                    } else {
                        return null
                    }
                    if (doc != null) return doc
                } else
                    remove(context, perm.uri)
            }
        }
        return null
    }
}
