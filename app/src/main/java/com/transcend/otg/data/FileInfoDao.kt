package com.transcend.otg.data

import androidx.room.*

@Dao
interface FileInfoDao {//data access object

    @Query("SELECT * FROM files WHERE fileType = :type ORDER BY title ASC, lastModifyTime ASC")
    fun getAllFilesByType(type: Int): List<FileInfo>

    @Query("SELECT * FROM files WHERE fileType = :type AND rootType = :root ORDER BY title ASC, lastModifyTime ASC")
    fun getMediaFiles(type: Int, root: Int): List<FileInfo>

    @Query("SELECT * FROM files WHERE fileType = :type AND title LIKE :searchText")
    fun getSearchFilesByType(searchText: String, type: Int): List<FileInfo>

    @Query("SELECT * FROM files WHERE fileType = :type AND path LIKE :src AND title LIKE :searchText")
    fun getSearchFilesByType(searchText: String, type: Int, src: String): List<FileInfo>

    @Query("SELECT * FROM files WHERE title LIKE :searchText AND parent = :folderPath")
    fun getSearchFilesAtFolder(searchText: String, folderPath: String): List<FileInfo>

    @Query("SELECT * FROM files WHERE parent = :parent ORDER BY title ASC")
    fun getAllFileInfos(parent: String): List<FileInfo>

    @Query("SELECT * FROM files WHERE parent = :parent and fileType = :type ORDER BY title ASC")
    fun getFiles(parent: String, type: Int): List<FileInfo>

    @Query("SELECT * FROM files WHERE path = :path")
    fun getFile(path: String): FileInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: FileInfo)

    @Delete
    fun delete(files: FileInfo)

    @Query("DELETE FROM files WHERE parent LIKE :folderPath")
    fun deleteAllFilesUnderFolderPath(folderPath: String)

    @Query("DELETE FROM files WHERE parent = :parentPath")
    fun deleteFilesFromParentPath(parentPath: String)

    @Query("DELETE FROM files WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM files WHERE rootType = :rootType")
    fun deleteAllFromRoot(rootType: Int)

    @Query("DELETE FROM files")
    fun deleteAll()

    @Query("DELETE FROM files WHERE fileType = :type")
    fun deleteAll(type: Int)

    @Query("UPDATE files SET title = :newName AND path = :newPath WHERE path = :oldPath")
    fun updateFileName(oldPath: String, newPath: String, newName: String)

    @Query("UPDATE files SET hasScanned = :scanned WHERE path = :path")
    fun setFolderScanned(path: String, scanned: Boolean)

    @Update
    fun updateFile(fileInfo:FileInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(fileInfos :List<FileInfo>)
}