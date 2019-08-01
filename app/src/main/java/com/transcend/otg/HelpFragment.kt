package com.transcend.otg

import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.transcend.otg.databinding.HelpFragmentBinding
import com.transcend.otg.viewmodels.HelpViewModel
import kotlinx.android.synthetic.main.help_fragment.*


class HelpFragment : Fragment() {

    companion object {
        var path = ""
        fun newInstance(helpPath : String) : HelpFragment {
            path = helpPath
            val fragment = HelpFragment()
            return fragment
        }
    }

    private lateinit var viewModel: HelpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var binding = HelpFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProviders.of(this).get(HelpViewModel::class.java)
        binding.viewModel = viewModel

        setValues()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWebview(webViewHelp)
    }

    fun initWebview(webView: WebView){
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                viewModel.progressVisibility.set(View.VISIBLE)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.progressVisibility.set(View.GONE)
            }
        }
    }

    fun setValues(){
        viewModel.webViewUrl.value = "https://help.transcendcloud.com/Elite/Android/TW"
    }
}
