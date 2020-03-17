package com.transcend.otg.browser

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.transcend.otg.R
import com.transcend.otg.action.FileActionManager
import com.transcend.otg.action.dialog.FileActionNewFolderDialog
import com.transcend.otg.action.loader.FolderCreateLoader
import com.transcend.otg.action.loader.NullLoader
import com.transcend.otg.adapter.RecyclerViewAdapter
import com.transcend.otg.databinding.FragmentTabBinding
import com.transcend.otg.utilities.AppPref
import com.transcend.otg.utilities.BackpressCallback
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.LoaderID
import kotlinx.android.synthetic.main.fragment_browser.*

class TabFragment: Fragment(), BackpressCallback, LoaderManager.LoaderCallbacks<Boolean> {

    lateinit var mAdapter: TabPagerAdapter
    lateinit var mBinding: FragmentTabBinding
    val icons = intArrayOf(
        R.drawable.ic_browser_filetype_all,
        R.drawable.ic_browser_filetype_image,
        R.drawable.ic_browser_filetype_music,
        R.drawable.ic_browser_filetype_video,
        R.drawable.ic_browser_filetype_document)
    lateinit var mMenu: Menu
    lateinit var mSearchView: SearchView
    private var mRoot = Constant.LOCAL_ROOT
    lateinit var mFileActionManager: FileActionManager  //action manager

    var mStartTab = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)     //設定選單

        if (arguments != null) {
            if (arguments!!.getString("root") != null)
                mRoot = arguments!!.getString("root")  //設定根目錄路徑
            if (arguments!!.getInt("media_type") != 0)  //設定初始顯示頁面
                mStartTab = arguments!!.getInt("media_type")
        }

        if (mRoot.startsWith(Constant.SD_ROOT ?: "Sdcard")){
            mFileActionManager = FileActionManager(context!!, FileActionManager.FileActionServiceType.SD, this)   //action manager
        } else if (mRoot.startsWith(Constant.LOCAL_ROOT)){
            mFileActionManager = FileActionManager(context!!, FileActionManager.FileActionServiceType.PHONE, this)   //action manager
        }

        mBinding = FragmentTabBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = TabPagerAdapter(activity!!, mRoot)
        mBinding.viewPager.adapter = mAdapter
        mBinding.viewPager.post(Runnable { mBinding.viewPager.setCurrentItem(mStartTab, false) })
        mBinding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(mBinding.tabLayout, mBinding.viewPager, object : TabLayoutMediator.OnConfigureTabCallback {
            override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                // Styling each tab here
                tab.setIcon(icons[position])
            }
        }).attach()

        mBinding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val pos = tab?.position
                when(pos){
                    Constant.TYPE_IMAGE -> mAdapter.imagePage.destroyActionMode()
                    Constant.TYPE_MUSIC -> mAdapter.musicPage.destroyActionMode()
                    Constant.TYPE_VIDEO -> mAdapter.videoPage.destroyActionMode()
                    Constant.TYPE_DOC -> mAdapter.docPage.destroyActionMode()
                    else -> mAdapter.allFilePage.destroyActionMode()
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position
                when(pos){
                    Constant.TYPE_IMAGE -> mAdapter.imagePage.refreshView()
                    Constant.TYPE_MUSIC -> mAdapter.musicPage.refreshView()
                    Constant.TYPE_VIDEO -> mAdapter.videoPage.refreshView()
                    Constant.TYPE_DOC -> mAdapter.docPage.refreshView()
                    else -> mAdapter.allFilePage.refreshView()
                }
            }
        })
    }

    fun doRefresh(){
        mAdapter.doRefresh(mBinding.viewPager.currentItem)
    }

    class TabPagerAdapter(fragmentActivity: FragmentActivity, root: String): FragmentStateAdapter(fragmentActivity) {
        val Pager_Count = 5
        var allFilePage: LocalFragment
        var imagePage: MediaFragment
        var musicPage: MediaFragment
        var videoPage: MediaFragment
        var docPage: MediaFragment

        init{
            allFilePage = LocalFragment(root)
            imagePage = MediaFragment(Constant.TYPE_IMAGE, root)
            musicPage = MediaFragment(Constant.TYPE_MUSIC, root)
            videoPage = MediaFragment(Constant.TYPE_VIDEO, root)
            docPage = MediaFragment(Constant.TYPE_DOC, root)
        }

        override fun getItemCount(): Int = Pager_Count

        override fun createFragment(position: Int): Fragment {
            when(position){
                Constant.TYPE_IMAGE -> return imagePage
                Constant.TYPE_MUSIC -> return musicPage
                Constant.TYPE_VIDEO -> return videoPage
                Constant.TYPE_DOC -> return docPage
                else -> return allFilePage
            }
        }

        fun doRefresh(position: Int){
            when(position){
                Constant.TYPE_IMAGE -> imagePage.doRefresh()
                Constant.TYPE_MUSIC -> musicPage.doRefresh()
                Constant.TYPE_VIDEO -> videoPage.doRefresh()
                Constant.TYPE_DOC -> docPage.doRefresh()
                else -> allFilePage.doRefresh()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        mMenu = menu

        mSearchView = menu.findItem(R.id.action_search).actionView as SearchView
        val search_editText = mSearchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        search_editText.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorWhite))
        search_editText.setTextColor(ContextCompat.getColor(context!!, R.color.c_02))
        search_editText.setHintTextColor(ContextCompat.getColor(context!!, R.color.c_04))
        mSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText == null)
                    return false
                if (newText.equals("")){
                    when(mBinding.viewPager.currentItem){
                        Constant.TYPE_IMAGE -> mAdapter.imagePage.viewModel.doReload()
                        Constant.TYPE_MUSIC -> mAdapter.musicPage.viewModel.doReload()
                        Constant.TYPE_VIDEO -> mAdapter.videoPage.viewModel.doReload()
                        Constant.TYPE_DOC -> mAdapter.docPage.viewModel.doReload()
                        else -> mAdapter.allFilePage.viewModel.doReload()
                    }
                } else {
                    when(mBinding.viewPager.currentItem){
                        Constant.TYPE_IMAGE -> mAdapter.imagePage.viewModel.doSearch(newText, Constant.TYPE_IMAGE)
                        Constant.TYPE_MUSIC -> mAdapter.musicPage.viewModel.doSearch(newText, Constant.TYPE_MUSIC)
                        Constant.TYPE_VIDEO -> mAdapter.videoPage.viewModel.doSearch(newText, Constant.TYPE_VIDEO)
                        Constant.TYPE_DOC -> mAdapter.docPage.viewModel.doSearch(newText, Constant.TYPE_DOC)
                        else -> mAdapter.allFilePage.viewModel.doSearch(newText, Constant.TYPE_DIR)
                    }
                }
                return true
            }
        })

        menu.findItem(R.id.action_search).setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                menu.findItem(R.id.action_more).setVisible(false)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                menu.findItem(R.id.action_more).setVisible(true)
                when(mBinding.viewPager.currentItem){
                    Constant.TYPE_IMAGE -> mAdapter.imagePage.doRefresh()
                    Constant.TYPE_MUSIC -> mAdapter.musicPage.doRefresh()
                    Constant.TYPE_VIDEO -> mAdapter.videoPage.doRefresh()
                    Constant.TYPE_DOC -> mAdapter.docPage.doRefresh()
                    else -> mAdapter.allFilePage.doRefresh()
                }
                return true
            }
        })

        menu.findItem(R.id.action_more).setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
            override fun onMenuItemClick(p0: MenuItem?): Boolean {
                //設定顯示模式 list or grid
                var adapter: RecyclerViewAdapter?
                when(mBinding.viewPager.currentItem){
                    Constant.TYPE_IMAGE -> adapter = mAdapter.imagePage.adapter
                    Constant.TYPE_MUSIC -> adapter = mAdapter.musicPage.adapter
                    Constant.TYPE_VIDEO -> adapter = mAdapter.videoPage.adapter
                    Constant.TYPE_DOC -> adapter = mAdapter.docPage.adapter
                    else -> adapter = mAdapter.allFilePage.adapter
                }
                if (adapter?.mViewType == RecyclerViewAdapter.List)
                    menu.findItem(R.id.action_view_type).setTitle(R.string.view_by_icons)
                else
                    menu.findItem(R.id.action_view_type).setTitle(R.string.view_by_list)
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
            R.id.action_view_type -> changeViewType()   //List or Grid
            R.id.action_select_mode -> {
                when(mBinding.viewPager.currentItem){
                    Constant.TYPE_IMAGE -> mAdapter.imagePage.startActionMode()
                    Constant.TYPE_MUSIC -> mAdapter.musicPage.startActionMode()
                    Constant.TYPE_VIDEO -> mAdapter.videoPage.startActionMode()
                    Constant.TYPE_DOC -> mAdapter.docPage.startActionMode()
                    else -> mAdapter.allFilePage.startActionMode()
                }
            }
            R.id.action_new_folder -> {
                val fileList = mAdapter.allFilePage.adapter?.mList
                if (fileList == null)
                    return false
                val nameList: MutableList<String> = ArrayList<String>()
                for (fileInfo in fileList){
                    nameList.add(fileInfo.title.toLowerCase())
                }

                val newFolderDialog = object: FileActionNewFolderDialog(context!!, nameList){
                    override fun onConfirm(newName: String) {
                        if(mBinding.viewPager.currentItem == Constant.TYPE_DIR){    //Tab在第一個(全檔案)時才能執行新增資料夾
                            val path = mAdapter.allFilePage.getPath()
                            mFileActionManager.createFolder(path, newName)   //通知action manager執行createFolder
                        }
                    }
                }
            }
            R.id.action_selectAll -> {
                when(mBinding.viewPager.currentItem){
                    Constant.TYPE_IMAGE -> mAdapter.imagePage.selectAll()
                    Constant.TYPE_MUSIC -> mAdapter.musicPage.selectAll()
                    Constant.TYPE_VIDEO -> mAdapter.videoPage.selectAll()
                    Constant.TYPE_DOC -> mAdapter.docPage.selectAll()
                    else -> mAdapter.allFilePage.selectAll()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeViewType(){
        var adapter: RecyclerViewAdapter? = null
        var recyclerview: RecyclerView? = null
        when(mBinding.viewPager.currentItem){
            Constant.TYPE_IMAGE -> {
                adapter = mAdapter.imagePage.adapter
                recyclerview = mAdapter.imagePage.recyclerView
            }
            Constant.TYPE_MUSIC ->{
                adapter = mAdapter.musicPage.adapter
                recyclerview = mAdapter.musicPage.recyclerView
            }

            Constant.TYPE_VIDEO ->{
                adapter = mAdapter.videoPage.adapter
                recyclerview = mAdapter.videoPage.recyclerView
            }
            Constant.TYPE_DOC ->{
                adapter = mAdapter.docPage.adapter
                recyclerview = mAdapter.docPage.recyclerView
            }
            else ->{
                adapter = mAdapter.allFilePage.adapter
                recyclerview = mAdapter.allFilePage.recyclerView
            }
        }

        if (adapter?.itemCount ?: 0 > 0){
            val currentItemType = adapter?.getItemViewType(0)
            when(currentItemType){
                RecyclerViewAdapter.Grid -> {
                    val listLayoutManager = LinearLayoutManager(context)
                    recyclerview?.layoutManager = listLayoutManager
                    adapter?.setViewType(RecyclerViewAdapter.List)
                    AppPref.setViewType(context, mBinding.viewPager.currentItem, RecyclerViewAdapter.List)
                    mMenu.findItem(R.id.action_view_type).setTitle(R.string.view_by_icons)
                }
                RecyclerViewAdapter.List -> {
                    val gridLayoutManager = GridLayoutManager(context, 3)
                    recyclerview?.layoutManager = gridLayoutManager
                    adapter?.setViewType(RecyclerViewAdapter.Grid)
                    AppPref.setViewType(context, mBinding.viewPager.currentItem, RecyclerViewAdapter.Grid)
                    mMenu.findItem(R.id.action_view_type).setTitle(R.string.view_by_list)
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        when(mBinding.viewPager.currentItem){
            Constant.TYPE_IMAGE, Constant.TYPE_MUSIC, Constant.TYPE_VIDEO, Constant.TYPE_DOC ->
                mBinding.viewPager.setCurrentItem(0, false) //回到第一個tab
            else -> return mAdapter.allFilePage.onBackPressed()
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Boolean> {
        when(id) {
            LoaderID.NEW_FOLDER -> return FolderCreateLoader(
                context!!,
                args?.getString("path")!!
            )
            else -> return NullLoader(context!!)
        }
    }

    override fun onLoadFinished(loader: Loader<Boolean>, data: Boolean?) {
        if (loader is FolderCreateLoader){
            mAdapter.doRefresh(Constant.TYPE_DIR)
        }
    }

    override fun onLoaderReset(loader: Loader<Boolean>) {

    }
}