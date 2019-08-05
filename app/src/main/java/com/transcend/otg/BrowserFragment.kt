package com.transcend.otg

import android.os.Bundle
import android.view.*
import android.widget.GridLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.BrowserFragmentBinding
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.browser_fragment.*
import java.io.File

class BrowserFragment : Fragment(), BackpressCallback, ActionMode.Callback {

    var mActionMode: ActionMode? = null
    var mActionModeView: RelativeLayout? = null
    lateinit var mActionModeTitle: TextView

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
//            getFolderFolderFile(currentFileParentString)
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

        viewModel = ViewModelProviders.of(activity as MainActivity).get(BrowserViewModel::class.java)
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
                (activity as AppCompatActivity).startSupportActionMode(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo, position: Int) {
            if (mActionMode != null) {
                fileInfo.isSelected = fileInfo.isSelected.not()
                adapter.notifyItemChanged(position)
            } else
                viewModel.doLoadFiles(fileInfo.path)
        }

        override fun onLongClick(fileInfo: FileInfo, position: Int) {
            if (mActionMode == null) {
                (activity as AppCompatActivity).startSupportActionMode(this@BrowserFragment)
            }
            fileInfo.isSelected = fileInfo.isSelected.not()
            adapter.notifyItemChanged(position)
        }
    }

    fun changeViewType(){
        if (adapter.itemCount > 0){
            val currentItemType = adapter.getItemViewType(0)
            when(currentItemType){
                FileInfoAdapter.Grid -> {
                    val listLayoutManager = LinearLayoutManager(context)
                    recyclerView.layoutManager = listLayoutManager
                    adapter.setViewType(FileInfoAdapter.List)
                }
                FileInfoAdapter.List -> {
                    val gridLayoutManager = GridLayoutManager(context, 3)
                    recyclerView.layoutManager = gridLayoutManager
                    adapter.setViewType(FileInfoAdapter.Grid)
                }
            }
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val id = item?.itemId
        when(id){
            R.id.action_delete -> {

            }
            R.id.action_rename -> {

            }
            R.id.action_new_folder -> {

            }
            R.id.action_selectAll -> {
                adapter.selectAll()
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mActionMode = mode
        mode?.getMenuInflater()?.inflate(R.menu.menu_actionmode, menu)
        mActionModeView = LayoutInflater.from(activity).inflate(R.layout.action_mode_custom, null) as RelativeLayout
        mActionModeTitle = (mActionModeView as RelativeLayout).findViewById(R.id.action_mode_custom_title) as TextView
        mActionMode?.setCustomView(mActionModeView)
        viewModel.isOnSelectMode.set(true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        mActionMode = null
        viewModel.isOnSelectMode.set(false)
        adapter.deselectAll()
        mode?.finish()
    }
}
