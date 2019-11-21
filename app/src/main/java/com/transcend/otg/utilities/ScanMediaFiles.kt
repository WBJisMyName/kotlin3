package com.transcend.otg.utilities

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import java.io.File

class ScanMediaFiles(application: Application){
    val repository = FileRepository(application)

    fun scanFileList(type: Int){
        if (Constant.mediaScanState.get(type) == Constant.ScanState.SCANNING)   //防呆，避免重複任務，但掃過了可以再掃一遍直接覆蓋過去
            return

        val thread = Thread(Runnable {
            when (type) {
                Constant.TYPE_IMAGE -> scanLocalAllImage(MainApplication.mContext)
                Constant.TYPE_MUSIC -> scanLocalAllMusics(MainApplication.mContext)
                Constant.TYPE_VIDEO -> scanLocalAllVideos(MainApplication.mContext)
                Constant.TYPE_DOC -> scanLocalAllDocs(MainApplication.mContext)
            }
        })
        thread.start()
    }

    fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    private fun scanLocalAllImage(context: Context) {
        var count = 0
        Constant.mediaScanState.set(Constant.TYPE_IMAGE, Constant.ScanState.SCANNING)
        try {
            val proj = arrayOf(
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED
            )

            var select = "(" + MediaStore.Files.FileColumns.DATA + " LIKE '" + Constant.LOCAL_ROOT + "%')"
            val orderBy = MediaStore.Images.Media.DISPLAY_NAME
            val order = " ASC"
            val imagecursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
                select, null, orderBy + order
            )
            if (imagecursor != null) {
                while (imagecursor.moveToNext()) {
                    val pathColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val nameColumnIndex =
                        imagecursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val timeColumnIndex =
                        imagecursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                    val sizeColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        imagecursor.getInt(imagecursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)).toLong()
                    )

                    val picPath = imagecursor.getString(pathColumnIndex)
                    var picName = imagecursor.getString(nameColumnIndex)
                    val picTime = 1000 * imagecursor.getLong(timeColumnIndex)
                    val picSize = imagecursor.getLong(sizeColumnIndex)
                    val picFile = File(picPath)

                    if (picName == null) {    //處理getString可能為空的問題
                        if (picPath == null)
                            continue
                        else {
                            val start = picPath.lastIndexOf("/") + 1
                            if (start < picPath.length)
                                picName = picPath.substring(start, picPath.length)
                            else
                                picName = "Unknow"
                        }
                    }

                    if (picFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = picPath
                        fileInfo.title = picName
                        fileInfo.lastModifyTime = picTime
                        fileInfo.fileType = Constant.TYPE_IMAGE
                        fileInfo.size = picSize
                        fileInfo.uri = imageUri.toString()
                        fileInfo.defaultIcon = R.drawable.ic_filelist_pic_grey
                        fileInfo.parent = picPath.replace(picName, "")

                        insert(fileInfo)
                        count++
                    }
                }
                imagecursor.close()
                Constant.mediaScanState.set(Constant.TYPE_IMAGE, Constant.ScanState.SCANNED)
                Thread.sleep(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Constant.mediaScanState.set(Constant.TYPE_IMAGE, Constant.ScanState.SCANNED)
        }
    }

    private fun scanLocalAllMusics(context: Context){
        var count = 0
        Constant.mediaScanState.set(Constant.TYPE_MUSIC, Constant.ScanState.SCANNING)
        try {
            val proj = arrayOf(
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                //                    MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATE_MODIFIED
            )
            val select = "(" + MediaStore.Audio.Media.DURATION + " > 10000)"
            var orderBy = MediaStore.Audio.Media.DATE_MODIFIED
            val order = " ASC"
            val musiccursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                select, null, orderBy + order
            )
            if (musiccursor != null) {
                while (musiccursor.moveToNext()) {
                    val pathColumnIndex = musiccursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val nameColumnIndex =
                        musiccursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val timeColumnIndex =
                        musiccursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                    val sizeColumnIndex = musiccursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                    val musicPath = musiccursor.getString(pathColumnIndex)
                    var musicName = musiccursor.getString(nameColumnIndex)
                    val musicTime = 1000 * musiccursor.getLong(timeColumnIndex)
                    val musicSize = musiccursor.getLong(sizeColumnIndex)

                    //                    long albumId = musiccursor.getInt(musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                    if (musicName == null) {    //處理getString可能為空的問題
                        if (musicPath == null)
                            continue
                        else {
                            val start = musicPath.lastIndexOf("/") + 1
                            if (start < musicPath.length)
                                musicName = musicPath.substring(start, musicPath.length)
                            else
                                musicName = "Unknow"
                        }
                    }

                    val musicFile = File(musicPath)
                    if (musicFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = musicPath
                        fileInfo.title = musicName
                        fileInfo.lastModifyTime = musicTime
                        fileInfo.fileType = Constant.TYPE_MUSIC
                        //                        fileInfo.album_id = albumId;
                        fileInfo.size = musicSize
                        fileInfo.defaultIcon = R.drawable.ic_filelist_mp3_grey
                        fileInfo.parent = musicPath.replace(musicName, "")
                        insert(fileInfo)
                        count++
                    }
                }
                musiccursor.close()
                Constant.mediaScanState.set(Constant.TYPE_MUSIC, Constant.ScanState.SCANNED)
                Thread.sleep(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Constant.mediaScanState.set(Constant.TYPE_MUSIC, Constant.ScanState.SCANNED)
        }
    }

    private fun scanLocalAllVideos(context: Context){
        var count = 0
        Constant.mediaScanState.set(Constant.TYPE_VIDEO, Constant.ScanState.SCANNING)
        try {
            val videoTypes = arrayOf(
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED
            )
            var orderBy = MediaStore.Video.Media.DATE_MODIFIED
            val order = " ASC"

            val videocursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoTypes, null, null, orderBy + order
            )

            if (videocursor != null) {
                while (videocursor.moveToNext()) {
                    val pathColumnIndex = videocursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameColumnIndex =
                        videocursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val timeColumnIndex =
                        videocursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                    val sizeColumnIndex = videocursor.getColumnIndex(MediaStore.Video.Media.SIZE)

                    val videoPath = videocursor.getString(pathColumnIndex)
                    var videoName = videocursor.getString(nameColumnIndex)
                    val videoTime = 1000 * videocursor.getLong(timeColumnIndex)
                    val videoSize = videocursor.getLong(sizeColumnIndex)

                    if (videoName == null) {    //處理getString可能為空的問題
                        if (videoPath == null)
                            continue
                        else {
                            val start = videoPath.lastIndexOf("/") + 1
                            if (start < videoPath.length)
                                videoName = videoPath.substring(start, videoPath.length)
                            else
                                videoName = "Unknow"
                        }
                    }

                    val videoFile = File(videoPath)
                    if (videoFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = videoPath
                        //videoName = videoName.substring(0, videoName.lastIndexOf("."));
                        fileInfo.title = videoName
                        fileInfo.lastModifyTime = videoTime
                        fileInfo.fileType = Constant.TYPE_VIDEO
                        fileInfo.size = videoSize
                        fileInfo.defaultIcon = R.drawable.ic_filelist_video_grey
                        fileInfo.parent = videoPath.replace(videoName, "")
                        insert(fileInfo)
                        count++
                    }
                }
                videocursor.close()
                Constant.mediaScanState.set(Constant.TYPE_VIDEO, Constant.ScanState.SCANNED)
                Thread.sleep(200)
            }
        } catch (e: Exception){
            e.printStackTrace()
            Constant.mediaScanState.set(Constant.TYPE_VIDEO, Constant.ScanState.SCANNED)
        }
    }

    private fun scanLocalAllDocs(context: Context) {
        var count = 0
        Constant.mediaScanState.set(Constant.TYPE_DOC, Constant.ScanState.SCANNING)
        try {
            val proj = arrayOf(
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.SIZE
            )

            var orderBy = MediaStore.Files.FileColumns.DATE_MODIFIED
            val order = " ASC"
            val select = ("(" + MediaStore.Files.FileColumns.DATA + " LIKE '%.doc'" + " or "
                    + MediaStore.Files.FileColumns.DATA + " LIKE '%.docx'" + " or "
                    + MediaStore.Files.FileColumns.DATA + " LIKE '%.xls'" + " or "
                    + MediaStore.Files.FileColumns.DATA + " LIKE '%.ppt'" + " or "
                    + MediaStore.Files.FileColumns.DATA + " LIKE '%.pdf'" + " or "
                    + MediaStore.Files.FileColumns.DATA + " LIKE '%.txt'" + ")")

            val docscursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"), proj, select, null, orderBy + order
            )
            if (docscursor != null) {
                while (docscursor.moveToNext()) {
                    val mimeColumnIndex =
                        docscursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val pathColumnIndex =
                        docscursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    val timeColumnIndex =
                        docscursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val sizeColumnIndex =
                        docscursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                    val docPath = docscursor.getString(pathColumnIndex)
                    val docTime = 1000 * docscursor.getLong(timeColumnIndex)
                    val docSize = docscursor.getLong(sizeColumnIndex)

                    val mimeType = docscursor.getString(mimeColumnIndex)
                    if (mimeType != null && mimeType.contains("directory"))
                        continue

                    if (!docPath.contains("/.")) {
                        val name = docPath.substring(docPath.lastIndexOf('/') + 1)
                        val check_file = File(docPath)
                        if (!check_file.exists() || check_file.isDirectory())
                            continue
                        val fileInfo = FileInfo()
                        fileInfo.path = docPath
                        fileInfo.title = name
                        fileInfo.lastModifyTime = docTime
                        fileInfo.size = docSize
                        fileInfo.fileType = Constant.TYPE_DOC
                        insert(fileInfo)
                        count++
                    }
                }
                docscursor.close()
                Constant.mediaScanState.set(Constant.TYPE_DOC, Constant.ScanState.SCANNED)
                Thread.sleep(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Constant.mediaScanState.set(Constant.TYPE_DOC, Constant.ScanState.SCANNED)
        }
    }
}