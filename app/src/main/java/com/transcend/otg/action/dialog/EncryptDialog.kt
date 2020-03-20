package com.transcend.otg.action.dialog

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

    init {
        initDialog()
        initFieldName()
    }

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
        mDlgBtnPos!!.isEnabled = checkName() && checkPassword() && checkPasswordConfirm()
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
                mDlgBtnPos!!.isEnabled = checkName() && checkPassword(password) && checkPasswordConfirm()
            }
        })

        mFieldConfirmPassword!!.editText!!.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val passwordConfirm = s.toString()
                checkPasswordConfirm(passwordConfirm)
                mDlgBtnPos!!.isEnabled = checkName() && checkPassword() && checkPasswordConfirm(passwordConfirm)
            }
        })

        updatePath(mPath)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { mDlgBtnPos!!.isEnabled = false }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val name = s.toString()
        mDlgBtnPos!!.isEnabled = checkName(name) && checkPassword() && checkPasswordConfirm()
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

    fun checkName(): Boolean{
        if (mFieldName == null || mFieldName?.editText == null) return false
        return checkName(mFieldName!!.editText!!.text.toString())
    }

    fun checkName(name: String): Boolean{
        var error: String? = null
        if (isInvalid(name))
            error = mContext.getString(R.string.invalid_name)
        if (!isIlleagal(name))
            error = mContext.getString(R.string.illegal_name)
        if (isFileExist(name))
            error = mContext.getString(R.string.duplicate_name)
        mFieldName!!.error = error
        return (error == null)
    }

    fun checkPassword(): Boolean{
        if (mFieldPassword == null || mFieldPassword?.editText == null) return false
        return checkPassword(mFieldPassword!!.editText!!.text.toString())
    }

    fun checkPassword(password: String): Boolean{
        if (password.equals(""))    return false
        var error: String? = null
        if (!password.equals(mFieldConfirmPassword?.editText?.text.toString()))
            error = mContext.getString(R.string.incorrect_password)
        mFieldConfirmPassword!!.error = error
        return (error == null)
    }

    fun checkPasswordConfirm(): Boolean{
        if (mFieldConfirmPassword == null || mFieldConfirmPassword?.editText == null) return false
        return checkPassword(mFieldConfirmPassword!!.editText!!.text.toString())
    }

    fun checkPasswordConfirm(passwordConfirm: String): Boolean{
        if (passwordConfirm.equals(""))    return false
        var error: String? = null
        if (!passwordConfirm.equals(mFieldPassword?.editText?.text.toString()))
            error = mContext.getString(R.string.incorrect_password)
        mFieldConfirmPassword!!.error = error
        return (error == null)
    }
}