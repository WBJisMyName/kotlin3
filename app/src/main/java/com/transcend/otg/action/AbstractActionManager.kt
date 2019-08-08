package com.transcend.otg.action

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader

abstract class AbstractActionManager(
    var mContext: Context?,
    var mCallbacks: LoaderManager.LoaderCallbacks<*>?
) {

    protected fun createLoader(id: Int, args: Bundle?): Boolean {
        var args = args
        if (args == null)
            args = Bundle()

        if (id >= 0 && mContext != null && mCallbacks != null) {
            (mContext as AppCompatActivity).supportLoaderManager.restartLoader(id, args, mCallbacks!!).forceLoad()
            return true
        }
        return false
    }

    internal abstract fun onCreateLoader(id: Int, args: Bundle): AsyncTaskLoader<*>?
    internal abstract fun onLoadFinished(loader: AsyncTaskLoader<*>, success: Boolean?): Boolean
    internal abstract fun onLoaderReset(loader: AsyncTaskLoader<*>)
}
