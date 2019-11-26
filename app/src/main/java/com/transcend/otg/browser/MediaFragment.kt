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
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.ScanMediaFiles
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

class MediaFragment(val mType: Int): BrowserFragment(Constant.LOCAL_ROOT){

    lateinit var searchAdapter: FileInfoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (activity != null && Constant.mediaScanState[mType] == Constant.ScanState.NONE)  //初次讀取檔案
            ScanMediaFiles(activity!!.application).scanFileList(mType)

        startLoadingView()  //在這裡呼叫以避免tab切換中load到其他頁面
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.isLoading.set(true)

        val lm = LinearLayoutManager(context)
        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        searchAdapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
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
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_MUSIC -> {
                viewModel.musicItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_VIDEO -> {
                viewModel.videoItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
            Constant.TYPE_DOC -> {
                viewModel.docItems.observe(this@MediaFragment, Observer { fileList ->
                    adapter?.submitList(fileList)
                    viewModel.isLoading.set(false)
                })
            }
        }

        viewModel.searchItems.observe(this@MediaFragment, Observer {
                fileList ->
            searchAdapter.submitList(fileList)
            viewModel.isLoading.set(false)
            viewModel.isEmpty.set(fileList.size == 0)
        })

        mBinding?.viewModel = viewModel  //Bind view and view model
    }

    override fun doRefresh() {
        doRefresh(mType)
    }

    fun doRefresh(type: Int){
        viewModel.isLoading.set(true)
        destroyActionMode()
        val thread = Thread(Runnable {
            if (activity != null)
                ScanMediaFiles(activity!!.application).scanFileList(type)
            Thread.sleep(200)
        })
        thread.start()
    }
}