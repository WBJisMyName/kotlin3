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

    var hasScanned = false  //用以判斷是否掃瞄過，主要用於資料夾
    var isSelected = false
    var defaultIcon: Int = R.drawable.ic_filelist_others_grey

    var uri: String? = null

    //List
    var subtitle: String? = null
    var infoIcon: Int = 0

    //Grid
    var smallMediaIconResId: Int = 0
    var isShowTitleLayout: Boolean = true
}