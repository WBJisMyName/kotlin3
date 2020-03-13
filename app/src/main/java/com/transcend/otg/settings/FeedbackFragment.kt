package com.transcend.otg.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.transcend.otg.databinding.FragmentFeedbackBinding
import com.transcend.otg.viewmodels.FeedbackViewModel


class FeedbackFragment : Fragment() {

    companion object {
        fun newInstance() = FeedbackFragment()
    }

    private lateinit var viewModel: FeedbackViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        viewModel = ViewModelProviders.of(this).get(FeedbackViewModel::class.java)
        binding.viewModel = viewModel

        return binding.root
    }

}
