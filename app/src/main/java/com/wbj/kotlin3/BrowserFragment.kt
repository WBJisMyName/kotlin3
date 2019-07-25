package com.wbj.kotlin3

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.wbj.kotlin3.adapter.FileInfoAdapter
import com.wbj.kotlin3.data.FileInfo
import com.wbj.kotlin3.databinding.BrowserFragmentBinding
import com.wbj.kotlin3.utilities.*
import com.wbj.kotlin3.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.browser_fragment.*
import java.io.File


class BrowserFragment : Fragment(), BackpressCallback {

    var mContext = context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onBackPressed(): Boolean {
        if(viewModel.mPath.equals(Constant.LOCAL_ROOT))
            return true
        else
            afterBackpress(File(viewModel.mPath).absolutePath)
        return false
    }

    fun afterBackpress(currentPath:String?){
        if(currentPath != null){
            val currentFile = File(currentPath)
            val currentFileParentString = currentFile.parent
            viewModel.doLoadFiles(currentFileParentString)
        }
    }

    companion object {
        fun newInstance() = BrowserFragment()
    }
    private lateinit var viewModel: BrowserViewModel
    private lateinit var adapter: FileInfoAdapter
    var mBinding: BrowserFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        setHasOptionsMenu(true)

        mBinding = BrowserFragmentBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)

        if (mContext == null)
            mContext = activity

        if (!PermissionHandle.isReadPermissionGranted(mContext!!))
            PermissionHandle.requestReadPermission(activity!!)

        viewModel = ViewModelProviders.of(this).get(BrowserViewModel::class.java)
        viewModel.items.observe(this, Observer { fileInfo->
            adapter.submitList(fileInfo)
            viewModel.isLoading.set(false)
        })

        viewModel.doLoadFiles(Constant.LOCAL_ROOT)
        mBinding?.viewModel = viewModel  //Bind view and view model

        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            viewModel.doLoadFiles(fileInfo.path)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
            R.id.action_view_type -> {
                changeViewType()
            }
            R.id.action_select_mode -> {
//                changeSelectMode()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeViewType(){   //更改顯示模式
        if (adapter.itemCount > 0) {
            var currentViewType = adapter.getItemViewType(0)
            when (currentViewType) {
                FileInfoAdapter.Grid -> {
                    val listLayoutManager = LinearLayoutManager(mContext)
                    recyclerView.layoutManager = listLayoutManager
                    adapter.setViewType(FileInfoAdapter.List)
                }
                FileInfoAdapter.List -> {
                    val gridLayoutManager = GridLayoutManager(mContext, getGridColCount())
                    recyclerView.layoutManager = gridLayoutManager
                    adapter.setViewType(FileInfoAdapter.Grid)
                }
            }
        }
    }

    fun getGridColCount() : Int{    //平板一行6個；手機一行3個
        if (MainApplication().isPad())
            return 6
        return 3
    }
}
