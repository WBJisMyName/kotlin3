package com.transcend.otg.utilities

import org.apache.commons.io.FilenameUtils

/**
 * Created by silverhsu on 16/1/25.
 */
object MimeUtil {

    private val IMAGE = "image"
    private val VIDEO = "video"
    private val AUDIO = "audio"

    fun getFileType(path: String) : Int{
        if (isPhoto(path))
            return Constant.TYPE_IMAGE
        else if (isMusic(path))
            return Constant.TYPE_MUSIC
        else if (isVideo(path))
            return Constant.TYPE_VIDEO
        else
            return Constant.TYPE_OTHERS
    }

    fun isPhoto(path: String): Boolean {
        val mime = getMimeType(path) ?: return false
        return mime.contains(IMAGE)
    }

    fun isVideo(path: String): Boolean {
        val mime = getMimeType(path) ?: return false
        return mime.contains(VIDEO)
    }

    fun isMusic(path: String): Boolean {
        val mime = getMimeType(path) ?: return false
        return mime.contains(AUDIO)
    }

    fun getMimeType(path: String): String? {
        val ext = FilenameUtils.getExtension(path)
        return if (ext != null) {
            MimeTypeMapExt.getSingleton().getMimeTypeFromExtension(ext.toLowerCase())
        } else null
    }

    fun getMimeTypeDetail(path: String): String? {
        val ext = FilenameUtils.getExtension(path)
        var detail: String? = null
        if (ext != null) {
            val a = MimeTypeMapExt.getSingleton().getMimeTypeFromExtension(ext.toLowerCase())
            detail = MimeTypeMapExt.getExtensionFromMimeType(a)
        }
        return detail
    }

}
