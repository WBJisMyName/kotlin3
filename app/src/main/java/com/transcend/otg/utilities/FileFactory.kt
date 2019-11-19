package com.transcend.otg.utilities

class FileFactory{
    private var mFileFactory: FileFactory? = null
    private val mMute = Any()
    private val mNotificationList: MutableList<String>

    init {
        mNotificationList = ArrayList()
    }

    fun getInstance(): FileFactory {
        synchronized(mMute) {
            if (mFileFactory == null)
                mFileFactory = FileFactory()
        }
        return mFileFactory!!
    }

    fun getNotificationID(): Int {
        var id = 1
        if (mNotificationList.size > 0) {
            val value = mNotificationList.get(mNotificationList.size - 1)
            id = Integer.parseInt(value) + 1
            mNotificationList.add(Integer.toString(id))
        } else {
            mNotificationList.add(Integer.toString(id))
        }
        return id
    }

    fun releaseNotificationID(id: Int) {
        val value = "" + id
        mNotificationList.remove(value)
    }
}