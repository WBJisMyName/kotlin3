package com.transcend.otg.information

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImageInfo::class], version = 1)
abstract class ImageInfoDatabase : RoomDatabase() {

    abstract fun imageInfoDao(): ImageInfoDao

    companion object {
        private var INSTANCE: ImageInfoDatabase? = null

        fun getInstance(context: Context): ImageInfoDatabase? {
            if (INSTANCE == null) {
                synchronized(ImageInfoDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        ImageInfoDatabase::class.java, "image_info")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }

}