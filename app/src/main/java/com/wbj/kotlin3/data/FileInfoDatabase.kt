package com.wbj.kotlin3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FileInfo::class], version = 9)
abstract class FileInfoDatabase : RoomDatabase() {

    abstract fun fileInfoDao(): FileInfoDao

    companion object {
        private var INSTANCE: FileInfoDatabase? = null

        fun getInstance(context: Context): FileInfoDatabase? {
            if (INSTANCE == null) {
                synchronized(FileInfoDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        FileInfoDatabase::class.java, "files")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }

}