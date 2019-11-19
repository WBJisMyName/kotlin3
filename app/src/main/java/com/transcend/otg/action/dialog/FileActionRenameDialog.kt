package com.transcend.otg.action.dialog

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.R
import com.transcend.otg.utilities.FileNameChecker
import org.apache.commons.io.FilenameUtils

abstract class FileActionRenameDialog(
    private val mContext: Context,
    private val mIgnoreType: Boolean,
    name: String,
    private val mNames: List<String>
) : TextWatcher, View.OnClickListener {
    private var mDialog: AlertDialog? = null
    private var mDlgBtnPos: Button? = null
    private var mFieldName: TextInputLayout? = null

    private var mName: String? = null
    private var mType: String? = null

    abstract fun onConfirm(newName: String)

    init {
        if (!mIgnoreType) {
            mName = FilenameUtils.getBaseName(name)
            mType = FilenameUtils.getExtension(name)
        } else {
            mName = name
        }
        initDialog()
        initFieldName()
    }

    private fun initDialog() {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle(mContext.resources.getString(R.string.rename))
        builder.setIcon(R.drawable.ic_tab_rename_grey)
        builder.setView(R.layout.dialog_folder_create)
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("Confirm", null)
        builder.setCancelable(true)
        mDialog = builder.show()
        mDlgBtnPos = mDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
        mDlgBtnPos!!.setOnClickListener(this)
    }

    private fun initFieldName() {
        mFieldName = mDialog!!.findViewById<View>(R.id.dialog_folder_create_name) as TextInputLayout?
        if (mFieldName!!.editText == null)
            return
        mFieldName!!.editText!!.setText(mName)
        mFieldName!!.editText!!.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val name = addExtension(s.toString())
        var error: String? = null
        var enabled = true
        if (isInvalid(name)) {
            error = "Invalid Name"
            enabled = false
        } else if (isDuplicated(name)) {
            error = "Duplicate Name"
            enabled = false
        }
        mFieldName!!.error = error
        mDlgBtnPos!!.isEnabled = enabled
    }

    override fun afterTextChanged(s: Editable) {

    }

    override fun onClick(v: View) {
        if (v == mDlgBtnPos) {
            if (mFieldName!!.editText == null)
                return
            val text = mFieldName!!.editText!!.text.toString()
            val checker = FileNameChecker(text)
            if (checker.isContainInvalid || checker.isStartWithSpace) {
                Toast.makeText(mContext, "Invalid Name", Toast.LENGTH_SHORT).show()
            } else {
                if (mName != text)
                    onConfirm(addExtension(text))
                mDialog!!.dismiss()
            }
        }
    }

    private fun isInvalid(name: String?): Boolean {
        return name == null || name.isEmpty()
    }

    private fun isDuplicated(name: String): Boolean {
        return if (isInvalid(name)) false else mNames.contains(name.toLowerCase())
    }

    private fun addExtension(name: String): String {
        var name = name
        if (!mIgnoreType)
            name = "$name.$mType"
        return name
    }
}
