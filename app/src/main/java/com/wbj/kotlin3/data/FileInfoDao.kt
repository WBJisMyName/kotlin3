package com.wbj.kotlin3.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FileInfoDao {//data access object

    @Query("SELECT * FROM files ORDER BY title ASC")
    fun getAllFileInfos(): LiveData<List<FileInfo>>

    @Query("SELECT * FROM files WHERE parent = :parent ORDER BY title ASC")
    fun getAllFileInfos(parent: String): List<FileInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: FileInfo)

    @Delete
    fun delete(files: FileInfo)

    @Query("DELETE FROM files")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(fileInfos :List<FileInfo>)


}