package com.transcend.otg.browser

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.Loader
import com.transcend.otg.MainActivity
import com.transcend.otg.R
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.dialog.FileActionRenameDialog
import com.transcend.otg.action.loader.*
import com.transcend.otg.data.FileInfo
import com.transcend.otg.singleview.ImageActivity
import com.transcend.otg.utilities.*
import com.transcend.otg.viewmodels.MainActivityViewModel
import java.io.File
import kotlin.concurrent.thread

open class LocalFragment(root: String): BrowserFragment(root){

    override fun getFragmentActicity(): AppCompatActivity {
        return activity as MainActivity
    }

    override fun setFileActionManager() {
        mFileActionManager = FileActionManager(mContext!!, FileActionManager.FileActionServiceType.PHONE, this)   //action manager
    }

    override fun refreshView(){
        if (activity is MainActivity)   //更新toolbar
            (activity as MainActivity).setToolbarMode(MainActivityViewModel.TabMode.Browser)
        //更新下拉式選單
        setDropdownList(viewModel.mPath)
    }

    override fun setDropdownList(path: String){
        if (activity == null)
            return
        var path = path
        val mainViewModel: MainActivityViewModel = ViewModelProviders.of(activity as MainActivity).get(MainActivityViewModel::class.java)   //取得activity的viewmodel，用以更新Dropdown
        val localMainTitle = Constant.PhoneName
        val sdMainTitle = getString(R.string.nav_sd)
        val sdcardRoot = Constant.SD_ROOT
        if (path.startsWith(Constant.LOCAL_ROOT))   //置換本地根目錄的名稱
            path = path.replace(Constant.LOCAL_ROOT, localMainTitle)
        else if (sdcardRoot != null && path.startsWith(sdcardRoot)) //置換SD根目錄名稱
            path = path.replace(sdcardRoot, sdMainTitle)

        val list = path.split("/").reversed().filter {
            !it.equals("")  //過濾空字串
        }

        //根目錄時隱藏下拉箭頭
        val arrowVisibility = if(list.size == 1) View.GONE else View.VISIBLE
        mainViewModel.dropdownArrowVisibility.set(arrowVisibility)
        mainViewModel.mDropdownList.set(list)

        //監控Dropdown item click
        mainViewModel.mDropdownAdapter.setOnDropdownItemSelectedListener(object: DropDownAdapter.OnDropdownItemSelectedListener{
            override fun onDropdownItemSelected(path: String) {
                var selected_path = path
                if (viewModel.mPath.startsWith(Constant.LOCAL_ROOT))
                    selected_path = selected_path.replace(Constant.PhoneName, Constant.LOCAL_ROOT)
                else if (sdcardRoot != null && viewModel.mPath.startsWith(sdcardRoot))
                    selected_path = selected_path.replace(getString(R.string.nav_sd), sdcardRoot)
                doLoadFiles(selected_path)
            }
        })
    }

    override fun doLoadFiles(path: String){
        viewModel.doLoadFiles(path)
        thread {
            Thread.sleep(100)   //睡0.1秒，避免黑畫面發生
        }
    }

    override var mRecyclerViewClickCallback = object : RecyclerViewClickCallback {
        override fun onClick(fileInfo: FileInfo) {
            if (mActionMode != null) {
                fileInfo.isSelected = fileInfo.isSelected.not()
                adapter?.notifyItemChanged(adapter?.mList?.indexOf(fileInfo)!!)
                updateActionTitle()
            } else {
                when(fileInfo.fileType){
                    Constant.TYPE_IMAGE -> {
                        val intent = Intent(activity, ImageActivity::class.java)
                        if (viewModel.mMediaType == -1) //-1為全瀏覽頁面，此時才季路目前路徑
                            intent.putExtra("folderPath", viewModel.mPath)
                        intent.putExtra("path", fileInfo.path)
                        intent.putExtra("title", fileInfo.title)
                        activity?.startActivity(intent)
                    }
                    Constant.TYPE_DIR -> doLoadFiles(fileInfo.path)
                    else -> {
                        MediaUtils.openIn(mContext, fileInfo)
                    }
                }
            }
        }

        override fun onLongClick(fileInfo: FileInfo) {
            startActionMode()
            fileInfo.isSelected = fileInfo.isSelected.not()
            adapter?.notifyItemChanged(adapter?.mList?.indexOf(fileInfo)!!)
            updateActionTitle()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileActionLocateActivity.REQUEST_CODE){
            if (resultCode == AppCompatActivity.RESULT_OK){
                val destPath = data?.getStringExtra("path")!!
                val action_id = data?.getIntExtra("action_id", -1)
                when(action_id){
                    R.id.action_copy -> mFileActionManager.copy(destPath, adapter?.getSelectedFilesPath()!!)
                    R.id.action_move -> mFileActionManager.move(destPath, adapter?.getSelectedFilesPath()!!)
                }
            }
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val id = item?.itemId
        when(id){
            R.id.action_delete -> {
                // setup dialog builder
                if(mContext != null) {
                    AlertDialog.Builder(mContext!!)
                        .setTitle("Delete")
                        .setPositiveButton("Confirm", { dialog, whichButton ->
                            mFileActionManager.delete(adapter?.getSelectedFilesPath()!!)
                        })
                        .setNegativeButton("Cancel", { dialog, whichButton ->
                            println("cancel")
                        })
                        .show()
                }
            }
            R.id.action_rename -> {
                if (adapter?.getSelectedFiles()?.size == 1) {
                    val fileInfo = adapter?.getSelectedFiles()!![0]
                    val fileList = adapter?.mList
                    if (fileList == null)
                        return false
                    val nameList: MutableList<String> = ArrayList<String>()
                    for (file in fileList) {
                        nameList.add(file.title.toLowerCase())
                    }

                    val isDirectory = (fileInfo.fileType == Constant.TYPE_DIR)
                    val newFolderDialog = object : FileActionRenameDialog(context!!, isDirectory, fileInfo.title, nameList) {
                        override fun onConfirm(newName: String) {
                            mFileActionManager.rename(fileInfo.path, newName)   //通知action manager執行createFolder
                        }
                    }
                }
            }
            R.id.action_selectAll -> {
                selectAll()
            }
            R.id.action_copy, R.id.action_move -> {
                startLocateActivity(id)
            }
            R.id.action_info -> {
                val list = adapter?.getSelectedFilesPath()
                if (list == null || list.size != 1)
                    return false
                else
                    startInfoActivity(list.get(0))
            }
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        if (mActionMode != null)
            onDestroyActionMode(mActionMode)
        when(id){
            LoaderID.FILE_DELETE -> return DeleteLoader(MainApplication.getInstance()!!.getContext(), args?.getStringArrayList("paths")!!)
            LoaderID.FILE_RENAME -> return RenameLoader(MainApplication.getInstance()!!.getContext(), args?.getString("path")!!, args.getString("name")!!)
            LoaderID.FILE_COPY -> return CopyLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            LoaderID.FILE_MOVE -> return MoveLoader(activity as MainActivity, args?.getStringArrayList("paths")!!, args?.getString("path")!!)
            else -> return NullLoader(MainApplication.getInstance()!!.getContext())
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, success: Boolean?) {
        (activity as MainActivity).actionFinishedReminder(R.string.done)
        doRefresh()
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {

    }

    //重新整理，刪除該資料夾下的檔案列表並重新掃描
    override fun doRefresh(){
        viewModel.isLoading.set(true)
        destroyActionMode()
        viewModel.doRefresh(true)
    }

    override fun onBackPressed(): Boolean {
        //如果檔案讀取中，則中斷讀取檔案
        if (viewModel.isLoading.get()) {
            viewModel.cancelScanTask()
            return false
        }

        if(viewModel.mPath.equals(mRoot))   //到了根目錄，回傳true
            return true
        else
            doLoadFiles(File(viewModel.mPath).parent ?: Constant.LOCAL_ROOT)  //讀取parent路徑
        return false
    }


}
