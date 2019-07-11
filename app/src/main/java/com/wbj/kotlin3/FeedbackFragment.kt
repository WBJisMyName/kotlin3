package com.wbj.kotlin3

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wbj.kotlin3.databinding.FeedbackFragmentBinding
import com.wbj.kotlin3.viewmodels.FeedbackViewModel


class FeedbackFragment : Fragment() {

    companion object {
        fun newInstance() = FeedbackFragment()
    }

    private lateinit var viewModel: FeedbackViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var binding = FeedbackFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProviders.of(this).get(FeedbackViewModel::class.java)
        binding.viewModel = viewModel

        return binding.root
    }

}
