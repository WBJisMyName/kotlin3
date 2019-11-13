package com.transcend.otg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.transcend.otg.databinding.FragmentSettingsBinding
import com.transcend.otg.viewmodels.SettingsViewModel

class SettingsFragment: Fragment(){

    private lateinit var viewModel: SettingsViewModel
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = (activity as MainActivity).findNavController(R.id.container)
        binding.layoutAbout.setOnClickListener {
            navController.navigate(R.id.aboutFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setMidTitle(getString(R.string.settingsTitle))
    }
}