package com.transcend.otg.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FileInfoDao {//data access object

    @Query("SELECT * FROM files ORDER BY title ASC")
    fun getAllFileInfos(): LiveData<List<FileInfo>>

    @Query("SELECT * FROM files WHERE parent = :parent ORDER BY title ASC")
    fun getAllFileInfos(parent: String): List<FileInfo>

    @Query("SELECT * FROM files WHERE parent = :parent and fileType = :type ORDER BY title ASC")
    fun getFiles(parent: String, type: Int): List<FileInfo>

    @Query("SELECT * FROM files WHERE path = :path")
    fun getFile(path: String): FileInfo

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: FileInfo)

    @Delete
    fun delete(files: FileInfo)

    @Query("DELETE FROM files WHERE parent LIKE :folderPath")
    fun deleteFilesUnderFolderPath(folderPath: String)

    @Query("DELETE FROM files WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM files")
    fun deleteAll()

    @Query("UPDATE files SET title = :newName AND path = :newPath WHERE path = :oldPath")
    fun updateFileName(oldPath: String, newPath: String, newName: String)

    @Update
    fun updateFile(fileInfo:FileInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(fileInfos :List<FileInfo>)
}