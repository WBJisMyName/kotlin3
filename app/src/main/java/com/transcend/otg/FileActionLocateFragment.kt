package com.transcend.otg

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.loader.LocalFolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.FragmentBrowserBinding
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.LoaderID
import com.transcend.otg.utilities.RecyclerViewClickCallback
import com.transcend.otg.viewmodels.ActionLocateViewModel
import kotlinx.android.synthetic.main.dialog_folder_create.*
import kotlinx.android.synthetic.main.fragment_browser.*
import java.io.File

class FileActionLocateFragment : Fragment(),
    BackpressCallback,
    LoaderCallbacks<Boolean>{

    lateinit var mContext: Context
    lateinit var mFileActionManager: FileActionManager  //action manager
    private var mRoot = Constant.LOCAL_ROOT //根目錄，本地 or SD

    private lateinit var viewModel: ActionLocateViewModel
    private lateinit var adapter: FileInfoAdapter
    var mBinding: FragmentBrowserBinding? = null    //共用browser_fragment.xml

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

        mFileActionManager = FileActionManager(mContext, FileActionManager.FileActionServiceType.PHONE, this)   //action manager

        if (arguments != null) {
            arguments!!.getString("root").let {
                if (it != null) //設定根目錄路徑
                    mRoot = it
            }
        }

        mBinding = FragmentBrowserBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)

        viewModel = ViewModelProviders.of(activity as FileActionLocateActivity).get(ActionLocateViewModel::class.java)
        viewModel.items.observe(this, Observer {    //觀察列表變化
            fileInfo->
                adapter.submitList(fileInfo)
        })

        viewModel.doLoadFiles(mRoot)    //讀取根目錄
        mBinding?.viewModel = viewModel  //Bind view and view model

        adapter = FileInfoAdapter(mRecyclerViewClickCallback, viewModel)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_locate_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
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
        }
        return super.onOptionsItemSelected(item)
    }

    val mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            viewModel.doLoadFiles(fileInfo.path)
        }

        override fun onLongClick(fileInfo: FileInfo) {
            onClick(fileInfo)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id){
            LoaderID.LOCAL_NEW_FOLDER -> return LocalFolderCreateLoader(mContext, args?.getString("path")!!)
            else -> return NullLoader(mContext)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        if (loader is LocalFolderCreateLoader){
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

    fun getPath(): String{
        return viewModel.mPath
    }

    fun getFileList(): List<FileInfo>?{
        return  viewModel.items.value
    }

    fun doReload(){
        viewModel.doRefresh()
    }
}
