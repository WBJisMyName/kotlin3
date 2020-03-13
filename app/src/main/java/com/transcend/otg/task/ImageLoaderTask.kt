package com.transcend.otg.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.os.OperationCanceledException
import android.provider.MediaStore
import android.util.Log
import com.github.mjdev.libaums.fs.UsbFileStreamFactory
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.UsbUtils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


abstract class ImageLoaderTask(internal val mPath: String,
                      internal val mType : Int,
                      internal val mThumbSize: Point
) : AsyncTask<String, Unit, Bitmap>() {

    abstract fun onFinished(bitmap: Bitmap?)

    override fun doInBackground(vararg strings: String): Bitmap? {
        if (isCancelled)
            return null

        var result: Bitmap? = null
        if (mPath.startsWith(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && mPath.startsWith(Constant.SD_ROOT!!))) {
            try {   //landscape 轉portrait
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
        } else {
            //TODO OTG
            try {   //landscape 轉portrait
                if (mType == Constant.TYPE_IMAGE) {
                    run {
                        val source = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
                        if (source == null)
                            return null
                        val inputStream: InputStream = UsbFileStreamFactory.createBufferedInputStream(source, UsbUtils.usbFileSystem!!)
                        result = decodeStream(inputStream, mThumbSize.x, mThumbSize.y)
                    }
                }
            } catch (e: Exception) {
                if (e !is OperationCanceledException) {
//                Log.d(TAG, "Failed to load thumbnail for $mPath: $e")
                }
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

    fun decodeStream(inputStream: InputStream, reqWidth: Int, reqHeight: Int): Bitmap? {
        var bitmap: Bitmap? = null
        var bis: BufferedInputStream? = null
        var baos: ByteArrayOutputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            bis = BufferedInputStream(inputStream)
            baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int = 0
            while (bis.read(buffer, 0, buffer.size).also({ len = it }) > 0) {
                baos.write(buffer, 0, len)
            }
            val imageData: ByteArray = baos.toByteArray()
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
            //获取imageview想要显示的宽和高
            var scale = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight
                val halfWidth = options.outWidth
                while (halfHeight / scale > reqHeight && halfWidth / scale > reqWidth) {
                    scale *= 2
                }
            }
            options.inJustDecodeBounds = false
            bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            bis?.close()
            baos?.close()
            inputStream.close()
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
