package com.transcend.otg.information

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_info")
data class ImageInfo(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "path")
    var path:String,

    @ColumnInfo(name = "name_title")
    var name_title: String
) {
    var name_subtitle: String? = null
    var time_title: String? = null
    var time_subtitle: String? = null
    var device_title: String? = null
    var device_subtitle: String? = null
    var location_title: String? = null
    var location_subtitle: String? = null
}