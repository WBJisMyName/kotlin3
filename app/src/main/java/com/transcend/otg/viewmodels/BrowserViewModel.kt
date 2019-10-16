package com.transcend.otg.viewmodels

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.task.FileLoaderTask
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.SystemUtil
import java.io.File
import kotlin.concurrent.thread


open class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    var mPath = Constant.LOCAL_ROOT //記錄當前路徑
    var livePath = MutableLiveData<String>().apply {
        this.value = ""
    }
    var isLoading = ObservableBoolean(false)
    var isEmpty = ObservableBoolean(true)

    val repository = FileRepository(application)
    var items = MutableLiveData<List<FileInfo>>().apply {
        this.value = ArrayList<FileInfo>()
    }
    var progress = ObservableInt(View.GONE)

    var imageItems = repository.getAllFilesByType(Constant.TYPE_IMAGE)
    var musicItems = repository.getAllFilesByType(Constant.TYPE_MUSIC)
    var videoItems = repository.getAllFilesByType(Constant.TYPE_VIDEO)
    var docItems = repository.getAllFilesByType(Constant.TYPE_DOC)

    var isOnSelectMode = ObservableBoolean(false)

    fun getAllFileInfos(): LiveData<List<FileInfo>> {
        return repository.getAllFileInfos()
    }

    fun getAllFileInfos(parent: String?){
        if (parent != null) {
            thread {
                Thread.sleep(200)   //需等待，否則取出的數據不正確
                val list = sort(repository.getAllFileInfos(parent))
                items.postValue(list)
                isEmpty.set(list.size == 0)
            }
        }
    }

    fun insert(fileInfo: FileInfo) {
        repository.insert(fileInfo)
    }

    fun delete(fileInfo: FileInfo) {
        repository.delete(fileInfo)
    }

    fun deleteFilesUnderFolderPath(folderPath: String) {
        repository.deleteFilesUnderFolderPath(folderPath)
    }

    fun deleteAll(){
        repository.deleteAll()
    }

    fun insertAll(fileInfos:List<FileInfo>){
        repository.insertAll(fileInfos)
    }

    open fun doLoadFiles(path: String){
        mPath = path
        livePath.postValue(mPath)
        isLoading.set(true)

        thread {
            val list =
                sort(repository.getAllFileInfos(path))
            if (list.size == 0){    //無資料就進task處理
                val task = FileLoaderTask(this)
                task.execute(path)
            } else { //有資料則直接post上去
                items.postValue(list)
                isEmpty.set(false)
            }
        }
    }

    fun doRefresh(){
        getAllFileInfos(mPath)
    }

    fun sort(list: List<FileInfo>): List<FileInfo>{
        return list.sortedWith(compareBy({it.fileType != 0}, {it.title}))   //先排資料夾，再照字母排
    }

    fun scanLocalAllImage(context: Context) {
        try {
            val proj = arrayOf(
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED
            )

            var select = "(" + MediaStore.Files.FileColumns.DATA + " LIKE '" + Constant.LOCAL_ROOT + "%')"
            val orderBy = MediaStore.Images.Media.DATE_MODIFIED
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
                    val picName = imagecursor.getString(nameColumnIndex)
                    val picTime = 1000 * imagecursor.getLong(timeColumnIndex)
                    val picSize = imagecursor.getLong(sizeColumnIndex)
                    val picFile = File(picPath)
                    if (picFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = picPath
                        fileInfo.title = picName
                        fileInfo.lastModifyTime = picTime
                        fileInfo.fileType = Constant.TYPE_IMAGE
                        fileInfo.size = picSize
                        fileInfo.uri = imageUri.toString()

                        insert(fileInfo)
                    }
                }
                imagecursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanLocalAllMusics(context: Context){
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
                    val musicName = musiccursor.getString(nameColumnIndex)
                    val musicTime = 1000 * musiccursor.getLong(timeColumnIndex)
                    val musicSize = musiccursor.getLong(sizeColumnIndex)

                    //                    long albumId = musiccursor.getInt(musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                    val musicFile = File(musicPath)
                    if (musicFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = musicPath
                        fileInfo.title = musicName
                        fileInfo.lastModifyTime = musicTime
                        fileInfo.fileType = Constant.TYPE_MUSIC
                        //                        fileInfo.album_id = albumId;
                        fileInfo.size = musicSize
                        insert(fileInfo)
                    }
                }
                musiccursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanLocalAllVideos(context: Context){
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
                    val videoName = videocursor.getString(nameColumnIndex)
                    val videoTime = 1000 * videocursor.getLong(timeColumnIndex)
                    val videoSize = videocursor.getLong(sizeColumnIndex)

                    val videoFile = File(videoPath)
                    if (videoFile.exists()) {
                        val fileInfo = FileInfo()
                        fileInfo.path = videoPath
                        //videoName = videoName.substring(0, videoName.lastIndexOf("."));
                        fileInfo.title = videoName
                        fileInfo.lastModifyTime = videoTime
                        fileInfo.fileType = Constant.TYPE_VIDEO
                        fileInfo.size = videoSize
                        insert(fileInfo)
                    }
                }
                videocursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanLocalAllDocs(context: Context) {
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
                    val path = docscursor.getString(pathColumnIndex)

                    val sdcardPath = SystemUtil().getSDLocation(context)
                    if (!path.contains("/.")) {
                        val name = path.substring(path.lastIndexOf('/') + 1)
                        val check_file = File(path)
                        if (!check_file.exists() || check_file.isDirectory())
                            continue
                        val fileInfo = FileInfo()
                        fileInfo.path = path
                        fileInfo.title = name
                        fileInfo.lastModifyTime = docTime
                        fileInfo.size = docSize
                        fileInfo.fileType = Constant.TYPE_DOC
                        insert(fileInfo)
                    }
                }
                docscursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
