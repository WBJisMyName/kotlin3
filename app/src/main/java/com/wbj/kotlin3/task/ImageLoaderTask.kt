package com.wbj.kotlin3.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.os.OperationCanceledException
import android.provider.MediaStore
import android.util.Log
import com.wbj.kotlin3.utilities.Constant

abstract class ImageLoaderTask(internal val mPath: String,
                      internal val mType : Int,
                      internal val mThumbSize: Point
) : AsyncTask<String, Unit, Bitmap>() {

    abstract fun onFinished(bitmap: Bitmap?)

    override fun doInBackground(vararg strings: String): Bitmap? {
        if (isCancelled)
            return null

        var result: Bitmap? = null
        try {
            if (mType == Constant.TYPE_IMAGE) run {
                result = decodeFullScreenBitmapFromPath(mPath, mThumbSize.x, mThumbSize.y)
            } else if (mType == Constant.TYPE_VIDEO)
                result = ThumbnailUtils.createVideoThumbnail(mPath, MediaStore.Video.Thumbnails.MINI_KIND)
            else if (mType == Constant.TYPE_MUSIC) run { result = loadAlbumThumbnail(mPath) }
        } catch (e: Exception) {
            if (e !is OperationCanceledException) {
//                Log.d(TAG, "Failed to load thumbnail for $mPath: $e")
            }
        }

        return result
    }

    override fun onPostExecute(result: Bitmap?) {
        onFinished(result)
    }

    fun decodeFullScreenBitmapFromPath(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        // Calculate inSampleSize
        var scale = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight
            val halfWidth = options.outWidth
            while (halfHeight / scale > reqHeight && halfWidth / scale > reqWidth) {
                scale *= 2
            }
        }
        options.inSampleSize = scale

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeFile(path, options)
        } catch (e: OutOfMemoryError) {

        }

        return bitmap
    }

    fun loadAlbumThumbnail(path: String): Bitmap? {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(path)
        } catch (e: Exception) {
            Log.d("IconUtils", e.message)
        }

        val artBytes = mmr.embeddedPicture
        return if (artBytes != null) {
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
        } else null

    }
}
