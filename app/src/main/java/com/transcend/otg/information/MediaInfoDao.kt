package com.transcend.otg.information

import androidx.room.*

@Dao
interface MediaInfoDao {//data access object

    @Query("SELECT * FROM media_info WHERE path = :path")
    fun getFile(path: String): MediaInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: MediaInfo)

    @Delete
    fun delete(files: MediaInfo)

    @Query("DELETE FROM media_info WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM media_info")
    fun deleteAll()
}