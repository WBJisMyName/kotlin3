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

abstract class FileActionNewFolderDialog(private val mContext: Context, private val mFolderNames: List<String>) :
    TextWatcher, View.OnClickListener {
    private var mDialog: AlertDialog? = null
    private var mDlgBtnPos: Button? = null
    private var mFieldName: TextInputLayout? = null

    private val uniqueName: String  //避免已存在"未命名資料夾"，此處以index作分別
        get() {
            var index = 1
            val name = "Untitled Folder"
            var unique = name
            while (mFolderNames.contains(unique)) {
                unique = String.format("$name (%d)", index++)
            }
            return unique
        }

    abstract fun onConfirm(newName: String) //點擊確定後的處理函式

    init {
        initDialog()
        initFieldName()
    }

    private fun initDialog() {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("New Folder")
        builder.setIcon(R.drawable.ic_tab_newfolder_grey)
        builder.setView(R.layout.dialog_folder_create)
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("Confirm", null)
        builder.setCancelable(true)
        mDialog = builder.show()
        mDlgBtnPos = mDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
        mDlgBtnPos!!.setOnClickListener(this)
    }

    private fun initFieldName() {
        mFieldName = mDialog!!.findViewById(R.id.dialog_folder_create_name) as TextInputLayout?
        if (mFieldName!!.getEditText() == null)
            return
        mFieldName!!.getEditText()?.setText(uniqueName)
        mFieldName!!.getEditText()?.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val name = s.toString()
        var error: String? = null
        var enabled = true
        if (isInvalid(name)) {
            error = "Invalid name"
            enabled = false
        } else if (isDuplicated(name)) {
            error = "Duplicate name"
            enabled = false
        }
        mFieldName!!.setError(error)
        mDlgBtnPos!!.isEnabled = enabled
    }

    override fun afterTextChanged(s: Editable) {

    }

    override fun onClick(v: View) {
        if (v == mDlgBtnPos) {
            if (mFieldName!!.getEditText() == null) return
            val name = mFieldName!!.getEditText()?.getText().toString()
            val checker = FileNameChecker(name)
            if (checker.isContainInvalid || checker.isStartWithSpace) {
                Toast.makeText(mContext, "Invalid name", Toast.LENGTH_SHORT).show()
            } else {
                onConfirm(name)
                mDialog!!.dismiss()
            }
        }
    }

    private fun isInvalid(name: String?): Boolean {
        return name == null || name.isEmpty()
    }

    private fun isDuplicated(name: String): Boolean {
        return if (isInvalid(name)) false else mFolderNames.contains(name.toLowerCase())
    }

}
