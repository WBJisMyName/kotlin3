package com.transcend.otg.browser

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.BrowserFragment
import com.transcend.otg.R
import com.transcend.otg.adapter.FileInfoAdapter
import com.transcend.otg.databinding.FragmentTabBinding
import com.transcend.otg.utilities.Constant
import com.transcend.otg.viewmodels.MediaFragment
import kotlinx.android.synthetic.main.dialog_folder_create.*
import kotlinx.android.synthetic.main.fragment_browser.*

class TabFragment: Fragment(){

    lateinit var mAdapter: TabPagerAdapter
    lateinit var mBinding: FragmentTabBinding
    val icons = intArrayOf(
        R.drawable.ic_browser_filetype_all,
        R.drawable.ic_browser_filetype_image,
        R.drawable.ic_browser_filetype_music,
        R.drawable.ic_browser_filetype_video,
        R.drawable.ic_browser_filetype_document)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)     //設定支援選單

        mBinding = FragmentTabBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = TabPagerAdapter(fragmentManager!!, context!!)
        mBinding.viewPager.adapter = mAdapter
        mBinding.viewPager.setCurrentItem(0)
        mBinding.viewPager.offscreenPageLimit = 1
        mBinding.tabLayout.setupWithViewPager(mBinding.viewPager)


        for (i in icons.indices) {
            //mTitleIcons[i]和mTitleNames[i]是放圖片和文字的資源的陣列
            mBinding.tabLayout.getTabAt(i)!!.setIcon(icons[i])//.setText(mTitleNames[i])
        }

        mBinding.swiperefresh.setColorSchemeResources(R.color.c_06)
        mBinding.swiperefresh.setOnRefreshListener {
            mBinding.swiperefresh.setRefreshing(false)
            //TODO
            (mBinding.viewPager.adapter as TabPagerAdapter).doRefresh(mBinding.viewPager.currentItem)
        }
    }

    class TabPagerAdapter(fm: FragmentManager, val context: Context): FragmentStatePagerAdapter(fm) {
        val Pager_Count = 5
        var allFilePage: BrowserFragment
        var imagePage: MediaFragment
        var musicPage: MediaFragment
        var videoPage: MediaFragment
        var docPage: MediaFragment

        val ALL_FILES_POS = 0
        val IMAGE_POS = 1
        val MUSIC_POS = 2
        val VIDEO_POS = 3
        val DOC_POS = 4

        init{
            allFilePage = BrowserFragment()
            imagePage = MediaFragment(Constant.TYPE_IMAGE)
            musicPage = MediaFragment(Constant.TYPE_MUSIC)
            videoPage = MediaFragment(Constant.TYPE_VIDEO)
            docPage = MediaFragment(Constant.TYPE_DOC)
        }

        override fun getItem(position: Int): Fragment {
            when(position){
                IMAGE_POS -> return imagePage
                MUSIC_POS -> return musicPage
                VIDEO_POS -> return videoPage
                DOC_POS -> return docPage
                else -> return allFilePage
            }
        }

        override fun getCount(): Int = Pager_Count

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return super.instantiateItem(container, position)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            super.destroyItem(container, position, `object`)
            println("position Destory$position")
        }

        fun doRefresh(position: Int){
            when(position){
                IMAGE_POS -> imagePage.doRefresh(Constant.TYPE_IMAGE)
                MUSIC_POS -> musicPage.doRefresh(Constant.TYPE_MUSIC)
                VIDEO_POS -> videoPage.doRefresh(Constant.TYPE_VIDEO)
                DOC_POS -> docPage.doRefresh(Constant.TYPE_DOC)
                else -> allFilePage.viewModel.doRefresh()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id){
            R.id.action_view_type -> changeViewType()   //List or Grid
//            R.id.action_select_mode -> (activity as AppCompatActivity).startSupportActionMode(this) //啟動action mode
            R.id.action_new_folder -> {
                val view = View.inflate(context, R.layout.dialog_folder_create, null)
                val textLayout = view.findViewById<TextInputLayout>(R.id.dialog_folder_create_name)

                AlertDialog.Builder(context!!)
                    .setTitle("New Folder")
                    .setIcon(R.drawable.ic_tab_newfolder_grey)
                    .setView(view)
                    .setPositiveButton("Confirm",{ dialog, whichButton ->
                        dialog_folder_create_name
                        val tmp = textLayout.editText?.text.toString()
//                        mFileActionManager.createFolder(viewModel.mPath, tmp)   //通知action manager執行createFolder
                    })
                    .setNegativeButton("Cancel", { dialog, whichButton ->
                        println("cancel")
                    })
                    .setCancelable(true)
                    .show()
            }
//            R.id.action_progress_test -> {
//                count = 0
//                mFloatingBtn.visibility = View.VISIBLE
//                mFloatingBtn.setProgressMax(max)
//                mBottomSheetFragment.setProgressMax(max)
//                handler.post(ProgressTest())
//            }
//            R.id.action_locate_test -> {
//                startLocateActivity()
//            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeViewType(){
        var adapter: FileInfoAdapter? = null

        when(mBinding.viewPager.currentItem){
            1 -> adapter = mAdapter.imagePage.adapter
            2 -> adapter = mAdapter.musicPage.adapter
            3 -> adapter = mAdapter.videoPage.adapter
            4 -> adapter = mAdapter.docPage.adapter
            else -> adapter = mAdapter.allFilePage.adapter
        }

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
}