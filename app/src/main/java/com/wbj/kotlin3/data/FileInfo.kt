package com.wbj.kotlin3.data

import android.view.View
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileInfo(
    @PrimaryKey(autoGenerate = true)
    var id:Int?,
    @ColumnInfo(name = "name")
    var name:String,
    @ColumnInfo(name = "path")
    var path:String,
    @ColumnInfo(name = "parent")
    var parent:String?,
    @ColumnInfo(name = "visibility")
    var visibility:Int
)
{
    constructor() : this(null, "", "", null, View.VISIBLE)
}