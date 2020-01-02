package com.transcend.otg.information

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediaInfo::class], version = 1)
abstract class MediaInfoDatabase : RoomDatabase() {

    abstract fun mediaInfoDao(): MediaInfoDao

    companion object {
        private var INSTANCE: MediaInfoDatabase? = null

        fun getInstance(context: Context): MediaInfoDatabase? {
            if (INSTANCE == null) {
                synchronized(MediaInfoDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        MediaInfoDatabase::class.java, "media_info")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }

}