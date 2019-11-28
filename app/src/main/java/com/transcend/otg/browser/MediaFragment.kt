package com.transcend.otg.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.BrowserFragment
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.ScanMediaFiles
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

class MediaFragment(val mType: Int): BrowserFragment(Constant.LOCAL_ROOT){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (activity != null && Constant.mediaScanState[mType] == Constant.ScanState.NONE) {  //初次讀取檔案，或需重讀檔案
            createScanTask(mType)
        } else
            viewModel.doLoadMediaFiles(mType)


        startLoadingView()  //在這裡呼叫以避免tab切換中load到其他頁面
    }

    fun createScanTask(type: Int){
        val scanTask = object:ScanMediaFiles(activity!!.application){
            override fun onFinished(list: List<FileInfo>) {
                val finalList = viewModel.sort(list)
                when(type){
                    Constant.TYPE_IMAGE -> viewModel.imageItems.postValue(finalList)
                    Constant.TYPE_MUSIC -> viewModel.musicItems.postValue(finalList)
                    Constant.TYPE_VIDEO -> viewModel.videoItems.postValue(finalList)
                    Constant.TYPE_DOC -> viewModel.docItems.postValue(finalList)
                }
            }
        }
        scanTask.scanFileList(type)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.isLoading.set(true)

        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)
    }

    override fun refreshView(){
        super.setDropdownList(Constant.LOCAL_ROOT)
    }

    fun startLoadingView() {
        viewModel.mMediaType = mType
        when(mType){
            Constant.TYPE_IMAGE -> {
                viewModel.imageItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(null)
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                    viewModel.isEmpty.set(fileList.size == 0)
                })
            }
            Constant.TYPE_MUSIC -> {
                viewModel.musicItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(null)
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                    viewModel.isEmpty.set(fileList.size == 0)
                })
            }
            Constant.TYPE_VIDEO -> {
                viewModel.videoItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(null)
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                    viewModel.isEmpty.set(fileList.size == 0)
                })
            }
            Constant.TYPE_DOC -> {
                viewModel.docItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(null)
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                    viewModel.isEmpty.set(fileList.size == 0)
                })
            }
        }

        mBinding?.viewModel = viewModel  //Bind view and view model
    }

    override fun doRefresh() {
        doRefresh(mType)
    }

    fun doRefresh(type: Int){
        viewModel.isLoading.set(true)
        viewModel.doRefresh(type)
    }

    fun doReload(type: Int){
        viewModel.isLoading.set(true)
        destroyActionMode()
        val thread = Thread(Runnable {
            if (activity != null)
                createScanTask(type)
            Thread.sleep(200)
        })
        thread.start()
    }
}