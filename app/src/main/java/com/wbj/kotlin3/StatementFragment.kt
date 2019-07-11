package com.wbj.kotlin3

import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import com.wbj.kotlin3.data.Url
import com.wbj.kotlin3.databinding.StatementFragmentBinding
import com.wbj.kotlin3.utilities.InjectorUtils
import com.wbj.kotlin3.viewmodels.StatementViewModel
import kotlinx.android.synthetic.main.statement_fragment.*


class StatementFragment : Fragment() {
    val realurl = Url(url)

    companion object {
        var url = ""
        fun newInstance(gogo : String) : StatementFragment{
            url = gogo
            return StatementFragment()
        }
    }

    private lateinit var viewModel: StatementViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        var binding = StatementFragmentBinding.inflate(inflater, container, false)
        var view = binding.root
        val factory = InjectorUtils.provideStatementViewModelFactory()
        viewModel = ViewModelProviders.of(this, factory).get(StatementViewModel::class.java)
        binding.viewModel = viewModel
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    fun initViewModel(){
        viewModel.addUrl(realurl);
        viewModel.getUrls().observe(this, Observer {it ->
            initWebView(webViewStatement, it)
        })
    }

    fun initWebView(webView: WebView, url: Url){
        viewModel.webViewUrl.value = url.url
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

}
