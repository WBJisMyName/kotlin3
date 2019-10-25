package com.transcend.otg.adapter

import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.databinding.BindingAdapter
import com.transcend.otg.utilities.MainApplication

object BindingAdapter {

    @BindingAdapter("app:isEmailValid")
    @JvmStatic
    fun emailValid(editText: EditText, value: String?) {
        if(!value.isNullOrEmpty()){
            if(!(Patterns.EMAIL_ADDRESS.matcher(value).matches())){
                editText.setError("錯誤格式")
            }
        }
    }

    @BindingAdapter("app:isSNValid")
    @JvmStatic
    fun snValid(editText: EditText, value: String?){
        if(!value.isNullOrEmpty()){
            if(value.length < 10){
                editText.setError("長度錯誤，應該為10")
                editText.setSelection(editText.length())
            }

        }
    }

    @BindingAdapter("app:isGone")
    @JvmStatic
    fun filesGone(view: View, isGone: Boolean) {
        view.visibility = if (isGone) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    @BindingAdapter("visible")
    @JvmStatic
    fun setVisible(view: View, visible: Boolean){
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @BindingAdapter("imageResource")
    @JvmStatic
    fun setImageResource(imageView : ImageView, resId : Int){
//        Glide.with(imageView.context)
//            .load(resId)
//            .into(imageView)
        imageView.setImageResource(resId)
    }

    @JvmStatic
    @BindingAdapter("dropdownList")
    fun setDropdownList(spinner: AppCompatSpinner?, dropdownList: List<String>) {
        val adapter = MainApplication.getInstance()?.getDropdownAdapter()
        adapter?.setDropdowonList(dropdownList)
        spinner?.adapter = adapter
    }

    @JvmStatic
    @BindingAdapter("enable")
    fun setSpinnerEnabled(spinner: AppCompatSpinner?, enable: Boolean) {
        spinner?.isEnabled = enable
    }
}