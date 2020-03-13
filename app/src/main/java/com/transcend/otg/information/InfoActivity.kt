package com.transcend.otg.information

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.R
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.databinding.ActivityFileInfoBinding
import com.transcend.otg.utilities.*
import java.io.File

class InfoActivity: AppCompatActivity(), LoaderManager.LoaderCallbacks<Boolean> {

    lateinit var mBinding: ActivityFileInfoBinding
    lateinit var mViewModel: InfoViewModel

    lateinit var mAdapter: InfoAdapter
    lateinit var mPath: String
    var mType: Int = Constant.TYPE_IMAGE
    var isLocal: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mPath = intent.getStringExtra("path")!!
        if (!FileFactory().isLocalPath(mPath) && !FileFactory().isSDCardPath(this, mPath))
            isLocal = false

        if (isLocal) {
            val file = File(mPath)
            if (file.isDirectory)
                mType = Constant.TYPE_DIR
            else
                mType = MimeUtil.getFileType(mPath)
        } else {
            val file = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
            if (file != null){
                if (file.isDirectory)
                    mType = Constant.TYPE_DIR
                else
                    mType = MimeUtil.getFileType(mPath)
            }
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_file_info)
        mViewModel = ViewModelProviders.of(this).get(InfoViewModel::class.java)
        mBinding.viewModel = mViewModel
        mViewModel.midTitle = getString(R.string.info_title)

        val lm = LinearLayoutManager(this)
        mAdapter = InfoAdapter(this)
        mBinding.infomationRecyclerView.adapter = mAdapter
        mBinding.infomationRecyclerView.setLayoutManager(lm)
        mBinding.infomationRecyclerView.setHasFixedSize(true)

        mBinding.toolbar.setNavigationIcon(R.drawable.ic_navi_back_white)
        mBinding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        LoaderManager.getInstance(this).restartLoader(LoaderID.FILE_INFORMATION, null, this).forceLoad()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.FILE_INFORMATION -> {
                mBinding.mainProgressView.visibility = View.VISIBLE
                return InfoLoader(mViewModel.app, mPath, mType)
            }
            else -> return NullLoader(this)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean) {
        if (loader is InfoLoader) {
            when(mType){
                Constant.TYPE_IMAGE -> {
                    val info = loader.imageInfo
                    mAdapter.setData(info)
                    mBinding.mainProgressView.visibility = View.GONE
                }
                Constant.TYPE_MUSIC, Constant.TYPE_VIDEO, Constant.TYPE_DIR, Constant.TYPE_OTHERS -> {
                    mAdapter.setData(loader.mediaInfo!!, mType)
                    mBinding.mainProgressView.visibility = View.GONE
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {

    }
}