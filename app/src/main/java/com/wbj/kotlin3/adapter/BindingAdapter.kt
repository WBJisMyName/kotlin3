package com.wbj.kotlin3.adapter

import android.util.Patterns
import android.view.View
import android.widget.EditText
import androidx.databinding.BindingAdapter

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

}