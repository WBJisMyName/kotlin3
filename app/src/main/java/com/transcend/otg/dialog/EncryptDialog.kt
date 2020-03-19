package com.transcend.otg.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.transcend.otg.R
import com.transcend.otg.browser.BrowserFragment
import com.transcend.otg.browser.FileActionLocateActivity
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.Md5
import org.apache.commons.io.FilenameUtils
import java.io.File

/**
 * Created by wangbojie on 2017/3/22.
 * Modified by mike chen on 2020/03/19
 */
abstract class EncryptDialog(private val mContext: BrowserFragment, private val mPath: String) : TextWatcher,
    View.OnClickListener {
    abstract fun onConfirm(newName: String, password: String, text: String)

    private var mDialog: AlertDialog? = null
    private var mDlgBtnPos: Button? = null
    private var mFieldName: TextInputLayout? = null
    private var mFieldPassword: TextInputLayout? = null
    private var mFieldConfirmPassword: TextInputLayout? = null
    private var pathView: TextView? = null

    private var absPath = mPath
    private fun initDialog() {
        val builder = AlertDialog.Builder(mContext.getFragmentActicity())
        builder.setTitle(mContext.resources.getString(R.string.encrypt))
        builder.setIcon(R.drawable.ic_encrypt_grey)
        builder.setView(R.layout.dialog_encrypt)
        builder.setNegativeButton(R.string.cancel, null)
        builder.setPositiveButton(R.string.confirm, null)
        builder.setCancelable(true)
        mDialog = builder.show()

        pathView = mDialog!!.findViewById<TextView>(R.id.encrypt_dest_path)
        updatePath(mPath)

        val select = mDialog!!.findViewById<ImageView>(R.id.select_dest)
        select?.setOnClickListener {
            val args = Bundle()
            args.putString("path", mPath)
            val intent = Intent()
            intent.setClass(mContext.getFragmentActicity(), FileActionLocateActivity::class.java)
            intent.putExtras(args)
            mContext.startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE)
        }

        mDlgBtnPos = mDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
        mDlgBtnPos!!.setOnClickListener(this)
    }

    fun updatePath(path: String){
        absPath = path
        var pathWithoutRoot=  path
        if (path.startsWith(Constant.LOCAL_ROOT))
            pathWithoutRoot = path.replaceFirst(Constant.LOCAL_ROOT, "")
        pathView?.setText(pathWithoutRoot)
        if(mFieldName == null || mFieldName?.editText == null)
            isFileExist(uniqueName)
        else
            isFileExist(mFieldName?.editText?.text.toString())
    }

    fun isShowing(): Boolean{
        return mDialog?.isShowing ?: false
    }

    fun isFileExist(filename: String): Boolean{
        val path = absPath + File.separator + filename + mContext.getString(R.string.encrypt_subfilename)
        return File(path).exists()
    }

    private fun initFieldName() {
        mFieldName = mDialog!!.findViewById<View>(R.id.dialog_encrypt_name) as TextInputLayout?
        if (mFieldName!!.editText == null) return
        mFieldName!!.editText!!.setText(uniqueName)
        mFieldName!!.editText!!.addTextChangedListener(this)
        mFieldPassword = mDialog!!.findViewById<View>(R.id.encrypt_password) as TextInputLayout?
        mFieldConfirmPassword = mDialog!!.findViewById<View>(R.id.encrypt_confirm_password) as TextInputLayout?
        mDlgBtnPos!!.isEnabled = false

        mFieldPassword!!.editText!!.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                var error: String? = null
                var enabled = true
                if (!password.equals(mFieldConfirmPassword?.editText?.text.toString())){
                    error = mContext.getString(R.string.incorrect_password)
                    enabled = false
                }
                mFieldConfirmPassword!!.error = error
                mDlgBtnPos!!.isEnabled = enabled
            }
        })

        mFieldConfirmPassword!!.editText!!.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val passwordConfirm = s.toString()
                var error: String? = null
                var enabled = true
                if (!passwordConfirm.equals(mFieldPassword?.editText?.text.toString())){
                    error = mContext.getString(R.string.incorrect_password)
                    enabled = false
                }
                mFieldConfirmPassword!!.error = error
                mDlgBtnPos!!.isEnabled = enabled
            }
        })
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { mDlgBtnPos!!.isEnabled = false }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val name = s.toString()
        var error: String? = null
        var enabled = true
        if (isInvalid(name)) {
            error = mContext.getString(R.string.invalid_name)
            enabled = false
        }
        if (!isIlleagal(name)) {
            error = mContext.getString(R.string.illegal_name)
            enabled = false
        }
        if (isFileExist(name)){
            error = mContext.getString(R.string.duplicate_name)
            enabled = false
        }
        mFieldName!!.error = error
        mDlgBtnPos!!.isEnabled = enabled
    }

    override fun afterTextChanged(s: Editable) {}
    override fun onClick(v: View) {
        if (v == mDlgBtnPos) {
            if (mFieldName!!.editText == null || mFieldName!!.editText!!.text.toString() == "" || mFieldPassword!!.editText!!.text.toString() == "" || mFieldPassword!!.editText!!.text.toString() != mFieldConfirmPassword!!.editText!!.text.toString()) return
            val name = mFieldName!!.editText!!.text.toString()
            var password = mFieldPassword!!.editText!!.text.toString()
            password = getPassword(password)
            onConfirm(name, password, absPath)
            mDialog!!.dismiss()
        }
    }

    private val uniqueName: String
        get() {
            return mContext.getString(R.string.encrypted_file)
        }

    private fun isInvalid(name: String?): Boolean {
        return name == null || name.isEmpty()
    }

    /*private boolean isDuplicated(String name) {
        if (isInvalid(name)) return false;
        return mFolderNames.contains(name.toLowerCase() + mContext.getResources().getString(R.string.encrypt_subfilename));
    }*/
    private fun isIlleagal(name: String): Boolean {
        if (isInvalid(name)) return true
        val prefix = FilenameUtils.getBaseName(name)
        if (name.contains("\\")) return false
        if (name.contains("/")) return false
        if (prefix.contains(":")) return false
        if (prefix.contains("*")) return false
        if (prefix.contains("?")) return false
        if (prefix.contains("<")) return false
        if (prefix.contains(">")) return false
        if (prefix.contains("|")) return false
        if (prefix.contains("\"")) return false
        if (prefix.startsWith(".")) return false
        return if (name == ".") false else true
    }

    companion object {
        private fun getPassword(str: String): String {
            var enc = str
            var md5: String? = null
            for (i in 0..2) {
                md5 = Md5.encode(enc)
                enc = md5.toUpperCase()
            }
            return enc
        }
    }

    //private List<String> mFolderNames;
    init {
        //mFolderNames = folderNames;
        initDialog()
        initFieldName()
    }
}