package com.transcend.otg.action.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.github.mjdev.libaums.fs.UsbFile
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.MimeUtil
import com.transcend.otg.utilities.UsbUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*

abstract class ActionAbstractLoader(context: Context) : AsyncTaskLoader<Boolean>(context) {

    var TAG = ActionAbstractLoader::class.java.simpleName
    val repository = FileRepository(MainApplication())

    var srcRoot: Root = Root.Local
    var destRoot: Root = Root.Local
    enum class Root{
        Local, SD, OTG
    }

    protected fun createUniqueName(source: File, destination: String): String {
        val isDirectory = source.isDirectory
        val origin = source.name
        return createUniqueName(isDirectory, origin, destination)
    }

    protected fun createUniqueName(source: UsbFile, destination: String): String {
        val isDirectory = source.isDirectory
        val origin = source.name
        return createUniqueName(isDirectory, origin, destination)
    }

    protected fun createUniqueName(isDirectory: Boolean, origin_name: String, destination: String): String {
        val names = ArrayList<String>()
        var unique = origin_name

        when(destRoot) {
            Root.Local, Root.SD -> {
                val dir = File(destination)
                val files = dir.listFiles { pathname ->
                    pathname.isDirectory == isDirectory }
                if (files != null)
                    for (file in files)
                        names.add(file.name)

                val ext = FilenameUtils.getExtension(origin_name)
                val prefix = FilenameUtils.getBaseName(origin_name)
                val suffix = if (ext.isEmpty()) "" else String.format(".%s", ext)
                var index = 1
                while (names.contains(unique)) {
                    unique = String.format("$prefix (%d)$suffix", index++)
                }
            }
            Root.OTG -> {
                val dir = UsbUtils.usbFileSystem?.rootDirectory?.search(destination)
                val files = dir?.listFiles()
                if (files != null) {
                    for (file in files)
                        if ((isDirectory && file.isDirectory) || (!isDirectory && !file.isDirectory))
                            names.add(file.name)
                }

                val ext = FilenameUtils.getExtension(origin_name)
                val prefix = FilenameUtils.getBaseName(origin_name)
                val suffix = if (ext.isEmpty()) "" else String.format(".%s", ext)
                var index = 1
                while (names.contains(unique)) {
                    unique = String.format("$prefix (%d)$suffix", index++)
                }
            }
        }

        return unique
    }

    protected fun insertFile(file: File){
        val info = FileInfo()
        info.title = file.name
        info.path = file.path
        info.lastModifyTime = file.lastModified()
        info.size = file.length()
        if (file.parent != null)
            info.parent = file.parent
        else
            info.parent = ""

        info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.path)
        when(info.fileType){
            Constant.TYPE_DIR -> {
                info.defaultIcon = R.drawable.ic_filelist_folder_grey
                info.infoIcon = R.drawable.ic_brower_listview_filearrow
            }
            Constant.TYPE_IMAGE -> {
                info.defaultIcon = R.drawable.ic_filelist_pic_grey
            }
            Constant.TYPE_MUSIC -> {
                info.defaultIcon = R.drawable.ic_filelist_mp3_grey
            }
            Constant.TYPE_VIDEO -> {
                info.defaultIcon = R.drawable.ic_filelist_video_grey
            }
            else -> {
                info.defaultIcon = R.drawable.ic_filelist_others_grey
            }
        }
        repository.insert(info)
    }

    protected fun insertFile(file: UsbFile){
        val info = FileInfo()
        info.title = file.name
        info.path = file.absolutePath
        info.lastModifyTime = file.lastModified()
        info.size = if (file.isDirectory) 0 else file.length
        if (file.parent != null)
            info.parent = file.parent!!.absolutePath
        else
            info.parent = ""

        info.fileType = if (file.isDirectory) Constant.TYPE_DIR else MimeUtil.getFileType(file.absolutePath)
        when(info.fileType){
            Constant.TYPE_DIR -> {
                info.defaultIcon = R.drawable.ic_filelist_folder_grey
                info.infoIcon = R.drawable.ic_brower_listview_filearrow
            }
            Constant.TYPE_IMAGE -> {
                info.defaultIcon = R.drawable.ic_filelist_pic_grey
            }
            Constant.TYPE_MUSIC -> {
                info.defaultIcon = R.drawable.ic_filelist_mp3_grey
            }
            Constant.TYPE_VIDEO -> {
                info.defaultIcon = R.drawable.ic_filelist_video_grey
            }
            else -> {
                info.defaultIcon = R.drawable.ic_filelist_others_grey
            }
        }
        repository.insert(info)
    }

    fun deleteFile(file: File){
        repository.delete(file.absolutePath)
    }

    fun deleteFile(file: UsbFile){
        repository.delete(file.absolutePath)
    }
}
