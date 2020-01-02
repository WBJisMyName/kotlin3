package com.transcend.otg.information

import androidx.room.*

@Dao
interface ImageInfoDao {//data access object

    @Query("SELECT * FROM image_info WHERE path = :path")
    fun getFile(path: String): ImageInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: ImageInfo)

    @Delete
    fun delete(files: ImageInfo)

    @Query("DELETE FROM image_info WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM image_info")
    fun deleteAll()
}