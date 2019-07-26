package com.transcend.otg.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.transcend.otg.R

@Entity(tableName = "files")
data class FileInfo(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "path")
    var path:String,

    @ColumnInfo(name = "title")
    var title:String,

    @ColumnInfo(name = "parent")
    var parent:String?,

    @ColumnInfo(name = "size")
    var size:Long,

    @ColumnInfo(name = "lastModifyTime")
    var lastModifyTime: Long,

    @ColumnInfo(name = "fileType")
    var fileType:Int
)
{
    constructor() : this("", "", "", 0, 0, 0)

    var isSelected = false

    var subtitle: String? = null
    var defaultIcon: Int = R.drawable.ic_filelist_others_grey
    var infoIcon: Int = 0
}