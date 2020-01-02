package com.transcend.otg.information

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_info")
data class MediaInfo(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "path")
    var path:String,

    @ColumnInfo(name = "name")
    var name: String
) {
    var parent:String? = null

    //Orientation 參照
    //    var rotation = {
    //            1: 'rotate(0deg)',
    //            2: 'scale(-1, 1)',
    //            3: 'rotate(180deg)',
    //            4: 'scale(1, -1)',
    //            6: 'rotate(90deg)',
    //            8: 'rotate(270deg)'
    //    };
    var date_time: String? = null   //format: yyyy:MM:dd HH:mm:ss
    var width = -1
    var height = -1
    var size: Long = -1

    var model: String? = null   //相機型號
    var make: String? = null //製造商
    var f_number = -1.0    //光圈值

    //曝光時間，以分子顯示(1/15)
    var exposure_time_numerator: Int = -1   //分子
    var exposure_time_denominator: Int = -1 //分母

    var focal_length = -1.0 //焦距，單位mm
    var iso_speed_rating = -1   //ISO
    var orientation = -1

    var latitude = -1.0
    var longitude = -1.0

    var modified_time = -1.0
    var add_time = -1.0
    var software: String? = null

    //Video
    var format: String? = null
    var duration: String? = null
    var frame_rate: String? = null

    //Music
    var album: String? = null
    var artist: String? = null
    var genre: String? = null
    var release_date: String? = null

    //Folder
    var folder_num: Long = 0
    var file_num: Long = 0
    var last_modify: Long = -1
}