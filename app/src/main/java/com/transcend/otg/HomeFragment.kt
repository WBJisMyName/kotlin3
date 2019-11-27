package com.transcend.otg


import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.transcend.otg.databinding.FragmentHomeBinding
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.FileFactory
import com.transcend.otg.viewmodels.HomeViewModel

class HomeFragment : Fragment() {

    lateinit var mBinding: FragmentHomeBinding
    lateinit var mViewModel: HomeViewModel

    override fun onResume() {
        super.onResume()
        initHome()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        mViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        mBinding.viewModel = mViewModel

        mViewModel.localCapabilityText.set(getString(R.string.loading))
        mViewModel.sdCapabilityText.set(getString(R.string.loading))

        return mBinding.root
    }

    fun initHome(){
        mViewModel.sdLayoutVisible.set(Constant.SD_ROOT != null)

        val localUsedSize = FileFactory().getUsedStorageSize(Constant.LOCAL_ROOT)
        val localTotalSize = FileFactory().getStorageAllSizeLong(Constant.LOCAL_ROOT)
        mViewModel.localCapabilityText.set(Formatter.formatFileSize(activity, localUsedSize) + " / " + Formatter.formatFileSize(activity, localTotalSize))
        val thousandth = ((localUsedSize.toDouble() / localTotalSize.toDouble()) * 1000).toInt()
        mViewModel.localProgressThousandth.set(thousandth)

        if (Constant.SD_ROOT != null) {
            val sdPath = Constant.SD_ROOT!!
            val sdUsedSize = FileFactory().getUsedStorageSize(sdPath)
            val sdTotalSize = FileFactory().getStorageAllSizeLong(sdPath)
            mViewModel.sdCapabilityText.set(Formatter.formatFileSize(activity, sdUsedSize) + " / " + Formatter.formatFileSize(activity, sdTotalSize))
            val thousandth = ((sdUsedSize.toDouble() / sdTotalSize.toDouble())* 1000).toInt()
            mViewModel.sdProgressThousandth.set(thousandth)
        }

        setClickEvent()
    }

    fun setClickEvent(){
        mBinding.cardviewLocal.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToBrowser(R.id.browserFragment)
            }
        })

        mBinding.cardviewSD.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToBrowser(R.id.sdFragment)
            }
        })

        mBinding.fabPhoto.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToMediaTab(Constant.TYPE_IMAGE)
            }
        })

        mBinding.fabMusic.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToMediaTab(Constant.TYPE_MUSIC)
            }
        })

        mBinding.fabVideo.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToMediaTab(Constant.TYPE_VIDEO)
            }
        })

        mBinding.fabDoc.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                (activity as MainActivity).goToMediaTab(Constant.TYPE_DOC)
            }
        })
    }
}
