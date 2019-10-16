package com.transcend.otg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.transcend.otg.databinding.FragmentPermissionBinding
import com.transcend.otg.utilities.InjectorUtils
import com.transcend.otg.viewmodels.StartPermissionViewModel

class StartPermissionFragment: Fragment(){

    private lateinit var viewModel: StartPermissionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var binding = FragmentPermissionBinding.inflate(inflater, container, false)
        val factory = InjectorUtils.provideStartPermissionViewModelFactory(activity!!)
        viewModel = ViewModelProviders.of(this, factory).get(StartPermissionViewModel::class.java)
        binding.viewModel = viewModel
        return binding.root
    }
}