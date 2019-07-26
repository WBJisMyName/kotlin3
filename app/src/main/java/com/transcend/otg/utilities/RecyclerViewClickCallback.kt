package com.transcend.otg.utilities

import com.transcend.otg.data.FileInfo


interface RecyclerViewClickCallback {
    fun onClick(fileInfo : FileInfo)
}
