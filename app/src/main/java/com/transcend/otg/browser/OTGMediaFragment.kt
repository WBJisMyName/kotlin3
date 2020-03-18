package com.transcend.otg.browser

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.MainActivity
import com.transcend.otg.adapter.RecyclerViewAdapter
import com.transcend.otg.utilities.AppPref
import com.transcend.otg.utilities.UiHelper
import com.transcend.otg.viewmodels.BrowserViewModel
import com.transcend.otg.viewmodels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_browser.*

class OTGMediaFragment(val mType: Int, val mediaRoot: String): OTGFragment(){

    override fun onResume() {
        viewModel.scanOTGFiles()
        super.onResume()
    }

    override fun refreshView(){
        if (activity is MainActivity)   //更新toolbar
            (activity as MainActivity).setToolbarMode(MainActivityViewModel.TabMode.Browser)
        setDropdownList(mediaRoot)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.mPath = mRoot
        viewModel.isLoading.set(true)

        val lm = LinearLayoutManager(context)
        adapter = RecyclerViewAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm)
        recyclerView.setHasFixedSize(true)

        startLoadingView()

        val viewType = AppPref.getViewType(context, viewModel.mMediaType)
        when(viewType){
            RecyclerViewAdapter.List -> {
                val listLayoutManager = LinearLayoutManager(context)
                recyclerView.layoutManager = listLayoutManager
                adapter?.setViewType(RecyclerViewAdapter.List)
            }
            RecyclerViewAdapter.Grid -> {
                val gridColCount = if(UiHelper.isPad()) 6 else 3
                val gridLayoutManager = GridLayoutManager(context, gridColCount)
                recyclerView.layoutManager = gridLayoutManager
                adapter?.setViewType(RecyclerViewAdapter.Grid)
            }
        }
    }

    fun startLoadingView() {
        viewModel.mMediaType = mType
        viewModel.items.observe(this@OTGMediaFragment, Observer { fileList ->
            adapter?.submitList(fileList)
            viewModel.isLoading.set(false)
            viewModel.isEmpty.set(fileList.size == 0)
        })
        mBinding?.viewModel = viewModel  //Bind view and view model
    }

    override fun doRefresh() {
        viewModel.isLoading.set(true)
        destroyActionMode()
        val thread = Thread(Runnable {
            viewModel.scanMediaFiles(mType, mediaRoot)
        })
        thread.start()
    }
}