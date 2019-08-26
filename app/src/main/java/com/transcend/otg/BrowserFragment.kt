package com.transcend.otg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.loader.LocalFileDeleteLoader
import com.transcend.otg.action.loader.LocalFolderCreateLoader
import com.transcend.otg.action.loader.LocalRenameLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.BrowserFragmentBinding
import com.transcend.otg.floatingbtn.BottomSheetFragment
import com.transcend.otg.floatingbtn.ProgressFloatingButton
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.LoaderID
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.BrowserViewModel
import kotlinx.android.synthetic.main.browser_fragment.*
import kotlinx.android.synthetic.main.dialog_folder_create.*
import java.io.File

class BrowserFragment : Fragment(),
    BackpressCallback,
    ActionMode.Callback,
    LoaderCallbacks<Boolean>{

    //action mode controller
    var mActionMode: ActionMode? = null
    var mActionModeView: RelativeLayout? = null
    lateinit var mActionModeTitle: TextView

    lateinit var mContext: Context
    lateinit var mFileActionManager: FileActionManager  //action manager
    lateinit var mBottomSheetFragment: BottomSheetFragment   //底部進度視窗
    lateinit var mFloatingBtn: ProgressFloatingButton   //Custom floating btn
    private var mRoot = Constant.LOCAL_ROOT //根目錄，本地 or SD

    private lateinit var viewModel: BrowserViewModel
    private lateinit var adapter: FileInfoAdapter
    var mBinding: BrowserFragmentBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        if (mContext == null)   //避免未進入onAttach而造成的Null
            mContext = activity as Context

        setHasOptionsMenu(true)     //設定支援選單
        setBottomSheetFragment()    //設定底部進度視窗

        mFileActionManager = FileActionManager(mContext, FileActionManager.FileActionServiceType.PHONE, this)   //action manager

        if (arguments != null)
            mRoot = arguments!!.getString("root")    //設定根目錄路徑

        mBinding = BrowserFragmentBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)

        viewModel = ViewModelProviders.of(activity as MainActivity).get(BrowserViewModel::class.java)
        viewModel.items.observe(this, Observer {    //觀察列表變化
            fileInfo->
                adapter.submitList(fileInfo)
                viewModel.isLoading.set(false)
        })

        viewModel.doLoadFiles(mRoot)    //讀取根目錄
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
            R.id.action_view_type -> changeViewType()   //List or Grid
            R.id.action_select_mode -> (activity as AppCompatActivity).startSupportActionMode(this) //啟動action mode
            R.id.action_new_folder -> {
                val view = View.inflate(mContext, R.layout.dialog_folder_create, null)
                val textLayout = view.findViewById<TextInputLayout>(R.id.dialog_folder_create_name)

                 AlertDialog.Builder(mContext)
                    .setTitle("New Folder")
                    .setIcon(R.drawable.ic_tab_newfolder_grey)
                    .setView(view)
                    .setPositiveButton("Confirm",{ dialog, whichButton ->
                        dialog_folder_create_name
                        val tmp = textLayout.editText?.text.toString()
                        mFileActionManager.createFolder(viewModel.mPath, tmp)   //通知action manager執行createFolder
                    })
                    .setNegativeButton("Cancel", { dialog, whichButton ->
                        println("cancel")
                    })
                    .setCancelable(true)
                    .show()
            }
            R.id.action_progress_test -> {
                count = 0
                mFloatingBtn.visibility = View.VISIBLE
                mFloatingBtn.setProgressMax(max)
                mBottomSheetFragment.setProgressMax(max)
                handler.post(ProgressTest())
            }
            R.id.action_locate_test -> {
                startLocateActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun startLocateActivity() {
        val args = Bundle()
        args.putString("path", viewModel.mPath)
        val intent = Intent()
        intent.setClass(activity, FileActionLocateActivity::class.java)
        intent.putExtras(args)
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE)
    }

    fun setBottomSheetFragment(){
        mBottomSheetFragment = BottomSheetFragment()
        mFloatingBtn = (activity as MainActivity).findViewById(R.id.progress_floating_btn)
        mFloatingBtn.visibility = View.GONE
        mFloatingBtn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                if (activity != null) {
                    mBottomSheetFragment.show((activity as MainActivity).supportFragmentManager, "TAG")
                    if (count >= max) {
                        Toast.makeText(activity, "讀取完成!", Toast.LENGTH_SHORT)
                        mFloatingBtn.visibility = View.GONE
                    }
                }
            }
        })
    }

    //TODO 測試底部進度視窗
    private val handler = Handler()
    private var count = 0
    private var max = 100
    inner private class ProgressTest : Runnable {
        override fun run() {
            count += 20
            if (count <= 100) {
                mFloatingBtn.setText("$count / $max")
                mFloatingBtn.setProgressValue(count)
                mBottomSheetFragment.setProcessText("$count / $max")
                mBottomSheetFragment.setProgressValue(count)
                handler.postDelayed(this, 1000)
            } else if (count > 100){
                mFloatingBtn.setProgressValue(0)
                mBottomSheetFragment.setProgressValue(0)
                mFloatingBtn.setText("Finished")
            }
        }
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            if (mActionMode != null) {
                fileInfo.isSelected = fileInfo.isSelected.not()
                adapter.notifyItemChanged(adapter.currentList.indexOf(fileInfo))
                updateActionTitle()
            } else
                viewModel.doLoadFiles(fileInfo.path)
        }

        override fun onLongClick(fileInfo: FileInfo) {
            if (mActionMode == null) {
                (activity as AppCompatActivity).startSupportActionMode(this@BrowserFragment)
            }
            fileInfo.isSelected = fileInfo.isSelected.not()
            adapter.notifyItemChanged(adapter.currentList.indexOf(fileInfo))
            updateActionTitle()
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
                // setup dialog builder
                AlertDialog.Builder(mContext)
                    .setTitle("Delete")
                    .setPositiveButton("Confirm",{ dialog, whichButton ->
                        mFileActionManager.delete(adapter.getSelectedFilesPath())
                    })
                    .setNegativeButton("Cancel", { dialog, whichButton ->
                        println("cancel")
                    })
                    .show()
            }
            R.id.action_rename -> {
                if (adapter.getSelectedFiles().size == 1) {
                    val fileInfo = adapter.getSelectedFiles()[0]

                    val view = View.inflate(mContext, R.layout.dialog_folder_create, null)
                    val textLayout = view.findViewById<TextInputLayout>(R.id.dialog_folder_create_name)
                    textLayout.editText?.setText(fileInfo.title)
                    AlertDialog.Builder(mContext)
                        .setTitle("Rename")
                        .setIcon(R.drawable.ic_tab_rename_grey)
                        .setView(view)
                        .setPositiveButton("Confirm", { dialog, whichButton ->
                            val name = textLayout.editText?.text.toString()
                            mFileActionManager.rename(fileInfo.path, name)
                        })
                        .setNegativeButton("Cancel", { dialog, whichButton ->
                            println("cancel")
                        })
                        .setCancelable(true)
                        .show()
                }
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
        updateActionTitle()
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

    fun updateActionTitle(){
        val count = adapter.getSelectedFiles().size
        val format = resources.getString(if (count <= 1) R.string.msg_file_selected else R.string.msg_files_selected)
        mActionModeTitle.setText(String.format(format, count))
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.LOCAL_FILE_DELETE -> return LocalFileDeleteLoader(mContext, args?.getStringArrayList("paths")!!)
            LoaderID.LOCAL_NEW_FOLDER -> return LocalFolderCreateLoader(mContext, args?.getString("path")!!)
            LoaderID.LOCAL_FILE_RENAME -> return LocalRenameLoader(mContext, args?.getString("path")!!, args.getString("name")!!)
            else -> return NullLoader(mContext)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)

        if (loader is LocalFileDeleteLoader){
            viewModel.doRefresh()
            adapter.notifyDataSetChanged()
        } else if (loader is LocalFolderCreateLoader){
            viewModel.doRefresh()
        } else if (loader is LocalRenameLoader){
            viewModel.doRefresh()
        }
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBackPressed(): Boolean {
        if(viewModel.mPath.equals(mRoot))   //到了根目錄，回傳true
            return true
        else
            viewModel.doLoadFiles(File(viewModel.mPath).parent)  //讀取parent路徑
        return false
    }
}
