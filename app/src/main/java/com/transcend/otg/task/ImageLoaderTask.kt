package com.transcend.otg.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.AsyncTask
import android.os.Build
import android.os.OperationCanceledException
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileStreamFactory
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.UsbUtils
import java.io.ByteArrayOutputStream


open class ImageLoaderTask(internal val mPath: String,
                           internal val mImageView: ImageView,
                           internal val mType : Int,
                           internal val mThumbSize: Point
) : AsyncTask<String, Unit, Bitmap>() {

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
            try {   //landscape 轉portrait
                if (mType == Constant.TYPE_IMAGE) {
                    run {
                        val source = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
                        if (source == null)
                            return null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            result = decodeInputStream(source, mThumbSize.x, mThumbSize.y)
                        else
                            result = decodeStream(source, mThumbSize.x, mThumbSize.y)
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
        if (result != null) {
            MainApplication.thumbnailsCache?.put(mPath + Constant.thumbnailCacheTail, result)  //將thumbnail記錄於Cache
            mImageView.scaleType = ImageView.ScaleType.CENTER_CROP  //設定顯示格式
            mImageView.setImageBitmap(result)
        }
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
        options.inSampleSize =  scale
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeFile(path, options)
        } catch (e: OutOfMemoryError) {

        }

        return bitmap
    }

    fun decodeStream(file: UsbFile, reqWidth: Int, reqHeight: Int): Bitmap? {
        var inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
        var bitmap: Bitmap? = null
        var baos: ByteArrayOutputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            baos = ByteArrayOutputStream()
            val buffer = ByteArray(256)
            var len: Int = 0
            while (inputStream.read(buffer, 0, buffer.size).also({ len = it }) > 0) {
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
            options.inSampleSize  = scale
            options.inJustDecodeBounds = false
            inputStream.close()
            inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            baos?.close()
            inputStream.close()
        }
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun decodeInputStream(file: UsbFile, reqWidth: Int, reqHeight: Int): Bitmap? {
        var inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
        var bitmap: Bitmap? = null
        var baos: ByteArrayOutputStream? = null
        try {
            val exif = ExifInterface(inputStream)
            val rotation: Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationInDegrees: Int = exifToDegrees(rotation)
            val matrix = Matrix()
            if (rotation != 0) {
                matrix.preRotate(rotationInDegrees.toFloat())
            }
            inputStream.close()

            inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            baos = ByteArrayOutputStream()
            val buffer = ByteArray(256)
            var len: Int = 0
            while (inputStream.read(buffer, 0, buffer.size).also({ len = it }) > 0) {
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
            options.inSampleSize  = scale
            options.inJustDecodeBounds = false
            inputStream.close()

            inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            if (bitmap != null)
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            baos?.close()
            inputStream.close()
        }
        return bitmap
    }

    fun exifToDegrees(exifOrientation: Int): Int {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
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
