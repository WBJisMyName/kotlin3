package com.transcend.otg.singleview

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.transcend.otg.R
import com.transcend.otg.data.FileInfo
import com.transcend.otg.databinding.SingleViewImageBinding
import com.transcend.otg.utilities.UiHelper
import com.transcend.otg.viewmodels.ImageViewModel

class ImageActivity : AppCompatActivity(){

    var mFolderPath: String? = null
    var mFilePath: String? = null
    var mTitle: String? = null
    var mPosition : Int = 0

    val imageDataBinding : SingleViewImageBinding by lazy{
        DataBindingUtil.setContentView<SingleViewImageBinding>(this, R.layout.single_view_image)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFolderPath = intent.getStringExtra("folderPath")
        mFilePath = intent.getStringExtra("path")
        mTitle = intent.getStringExtra("title")

        UiHelper.setSystemBarTranslucent(this)
        var viewModel = ViewModelProviders.of(this).get(ImageViewModel::class.java)
        if (mFolderPath == null)
            viewModel.loadAllImageList(mFilePath!!)
        else
            viewModel.loadImageList(mFolderPath!!)

        viewModel.title.set(mTitle ?: "N/A")
        imageDataBinding.viewModel = viewModel

        imageDataBinding.photoViewPager.adapter = ViewPagerAdapter(this)
        imageDataBinding.photoViewPager.offscreenPageLimit = 1

        viewModel.items.observe(this, Observer<List<FileInfo>> { value ->
            value?.let {
                (imageDataBinding.photoViewPager.adapter as ViewPagerAdapter).update(it)
                imageDataBinding.photoViewPager.setCurrentItem(getCurrentImagePosition(it), false)
            }
        })

        imageDataBinding.photoViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                mPosition = position
                viewModel.title.set((imageDataBinding.photoViewPager.adapter as ViewPagerAdapter).getItemTitle(mPosition))
            }
        })

        setSupportActionBar(imageDataBinding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getCurrentImagePosition(list: List<FileInfo>): Int{
        if (list.size > 0){
            var count = 0
            for (file in list){
                if (file.path.equals(mFilePath))
                    return count
                count++
            }
        }
        return 0
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.e(ImageActivity::class.java.simpleName, "Focus")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus)
            Log.e(ImageActivity::class.java.simpleName, "Focus")
    }
}