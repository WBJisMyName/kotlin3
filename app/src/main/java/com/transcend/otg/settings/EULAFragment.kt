package com.transcend.otg.settings

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import com.transcend.otg.MainActivity
import com.transcend.otg.R
import com.transcend.otg.data.EULAOberableField
import com.transcend.otg.databinding.FragmentEulaBinding
import kotlinx.android.synthetic.main.fragment_eula.*

class EULAFragment : Fragment() {

    interface OnEulaClickListener {
        fun onEulaAgreeClick(v: View)
    }

    companion object {
        var observableField = EULAOberableField(View.GONE, ObservableInt(View.GONE), "")
        var mListener: OnEulaClickListener? = null
        fun newInstance(showButton: Boolean, eulaPath : String) : EULAFragment {
            observableField.buttonVisibility = if(showButton) View.VISIBLE else View.GONE
            observableField.url = eulaPath
            return EULAFragment()
        }
        fun setOnEulaClickListener(listener: OnEulaClickListener) {
            mListener = listener
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var binding = FragmentEulaBinding.inflate(inflater, container, false)
        binding.observable =
            observableField
        binding.onClickListener = View.OnClickListener {
            mListener?.onEulaAgreeClick(it)
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWebview(webView)
    }

    fun initWebview(webView: WebView){
        observableField.buttonVisibility = View.GONE
        observableField.url = "file:///android_asset/EULA.html"
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                observableField.progressVisibility.set(View.VISIBLE)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                observableField.progressVisibility.set(View.GONE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity == null)
            return
        (activity as MainActivity).setMidTitle(getString(
            R.string.about_enduser
        ))
    }
}
