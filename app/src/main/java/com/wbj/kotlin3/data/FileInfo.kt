package com.wbj.kotlin3.data

import android.view.View
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

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
    @ColumnInfo(name = "size")
    var size:Long,
    @ColumnInfo(name = "lastModifyTime")
    var lastModifyTime: Long,
    @ColumnInfo(name = "fileType")
    var fileType:Int


/*
public String groupTitle;


    public int type;


    public int mediaIconResourceId = -1;
    public int defaultIconResourceId = -1;

    public String uri;
    public int storage;

    public boolean showTitleLayout;
    public boolean isChecked;


    public String title;
    public String path;
    public long size;
    public String subtitle;
 */

)
{
    constructor() : this(null, "", "", null, 0, 0, 0)
}