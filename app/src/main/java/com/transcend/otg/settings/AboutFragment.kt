package com.transcend.otg.settings

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.transcend.otg.BuildConfig
import com.transcend.otg.MainActivity
import com.transcend.otg.R
import com.transcend.otg.databinding.FragmentAboutBinding
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.viewmodels.AboutViewModel

class AboutFragment: Fragment(){

    private lateinit var viewModel: AboutViewModel
    private lateinit var binding: FragmentAboutBinding

    private var mActivity: Activity? = null
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as Activity
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAboutBinding.inflate(inflater, container, false)
        viewModel = ViewModelProviders.of(this).get(AboutViewModel::class.java)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val f = AboutPreference()
        fragmentManager?.beginTransaction()?.replace(R.id.about_frame, f)?.commit()
    }

    class AboutPreference: PreferenceFragmentCompat(){
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preference_about)
            val version = findPreference<Preference>(MainApplication.getInstance()!!.resources.getString(
                R.string.about_version
            ))
            version?.setSummary(BuildConfig.VERSION_NAME)
            
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if(activity == null)
                return super.onPreferenceTreeClick(preference)

            val navController = (activity as MainActivity).findNavController(
                R.id.container
            )
            if (preference?.key.equals(getString(R.string.about_enduser))){
                navController.navigate(R.id.EULAFragment)
            } else if (preference?.key.equals(getString(R.string.about_opensource))){
                navController.navigate(R.id.statementFragment)
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity == null)
            return
        (activity as MainActivity).setMidTitle(getString(
            R.string.setting_about
        ))
    }
}