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
import java.util.*

/**
 * Created by wangbojie on 2017/3/23.
 * Modified by mike chen on 2020/03/19
 */
abstract class DecryptDialog(
    private val mContext: BrowserFragment,
    private val mFilePath: String
) : View.OnClickListener, TextWatcher {
    abstract fun onConfirm(
        newFolderPath: String,
        password: String,
        mFilePath: String
    )

    private var mDialog: AlertDialog? = null
    private var mDlgBtnPos: Button? = null
    private var mFieldFolderName: TextInputLayout? = null
    private var mFieldPassword: TextInputLayout? = null

    private var pathView: TextView? = null
    private var destAbsPath: String? = null

    private val mFolderNames = ArrayList<String>()

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
        val tmpFile = File(mFilePath)
        mFieldFolderName!!.editText!!.setText(FilenameUtils.removeExtension(tmpFile.name))
    }

    private fun initDialog() {
        val builder = AlertDialog.Builder(mContext.getFragmentActicity())
        builder.setTitle(mContext.resources.getString(R.string.decrypt))
        builder.setIcon(R.drawable.ic_encrypt_grey)
        builder.setView(R.layout.dialog_decrypt)
        builder.setNegativeButton(R.string.cancel, null)
        builder.setPositiveButton(R.string.confirm, null)
        builder.setCancelable(true)
        mDialog = builder.show()
        mDlgBtnPos = mDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
        mDlgBtnPos!!.setOnClickListener(this)
        mDlgBtnPos!!.isEnabled = false

        pathView = mDialog!!.findViewById<TextView>(R.id.decrypt_dest_path)

        val select = mDialog!!.findViewById<ImageView>(R.id.select_dest)
        select?.setOnClickListener {
            val args = Bundle()
            args.putString("path", mFilePath)
            val intent = Intent()
            intent.setClass(mContext.getFragmentActicity(), FileActionLocateActivity::class.java)
            intent.putExtras(args)
            mContext.startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE)
        }
    }

    fun updatePath(path: String){
        destAbsPath = path
        var pathWithoutRoot=  path
        if (pathWithoutRoot.startsWith(Constant.LOCAL_ROOT))
            pathWithoutRoot = pathWithoutRoot.replaceFirst(Constant.LOCAL_ROOT, "")
        pathView?.setText(pathWithoutRoot)

        getPathNames()
        mDlgBtnPos!!.isEnabled = (checkName() && checkPassword())
    }

    fun isShowing(): Boolean{
        return mDialog?.isShowing ?: false
    }

    private fun initFieldName() {
        mFieldFolderName = mDialog!!.findViewById<View>(R.id.dialog_decrypt_name) as TextInputLayout?
        if (mFieldFolderName!!.editText == null) return
        mFieldFolderName!!.editText!!.setText(uniqueName)
        mFieldFolderName!!.editText!!.addTextChangedListener(this)
        mFieldPassword = mDialog!!.findViewById<View>(R.id.dialog_decrypt_password) as TextInputLayout?
        mFieldPassword!!.editText!!.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var enabled = (s?.length != 0)
                mDlgBtnPos!!.isEnabled = (checkName() && enabled)
            }
        })

        updatePath(File(mFilePath).parent)
    }

    private fun getPathNames(){
        if (destAbsPath == null) return
        mFolderNames.clear()
        if ((destAbsPath!!.startsWith(Constant.LOCAL_ROOT)) || (Constant.SD_ROOT != null && (destAbsPath!!.startsWith(Constant.SD_ROOT!!)))){
            val target = File(destAbsPath)
            if (target.isDirectory){
                for (file in target.listFiles()){
                    if (file.isDirectory)
                        mFolderNames.add(file.name.toLowerCase())
                }
            }
        }
    }

    override fun onClick(v: View) {
        if (v == mDlgBtnPos) {
            if (mFieldFolderName!!.editText!!.text.toString() == "" || mFieldPassword!!.editText!!.text.toString() == "" || mFieldPassword!!.editText!!.text.toString() == "") {
                return
            }
            val name = mFieldFolderName!!.editText!!.text.toString()
            var password = mFieldPassword!!.editText!!.text.toString()
            password =
                getPassword(
                    password
                )

            val newFolderPath = destAbsPath + File.separator + name
            onConfirm(newFolderPath, password, mFilePath)
            mDialog!!.dismiss()
        } else {
            mDialog!!.dismiss()
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val name = s.toString()
        mDlgBtnPos!!.isEnabled = (checkName(name) && checkPassword())
    }

    override fun afterTextChanged(s: Editable) {}
    private fun isInvalid(name: String?): Boolean {
        return name == null || name.isEmpty()
    }

    private fun isDuplicated(name: String): Boolean {
        return if (isInvalid(name)) false else mFolderNames.contains(name.toLowerCase())
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

    private val uniqueName: String
        private get() {
            var index = 1
            val name = mContext.resources.getString(R.string.untitled_folder)
            var unique = name
            while (mFolderNames.contains(unique)) {
                unique = String.format("$name (%d)", index++)
            }
            return unique
        }

    fun checkName(): Boolean{
        if (mFieldFolderName == null || mFieldFolderName?.editText == null) return false
        return checkName(mFieldFolderName!!.editText!!.text.toString())
    }

    fun checkName(name: String): Boolean{
        var error: String? = null
        if (isInvalid(name))
            error = mContext.getString(R.string.invalid_name)
        if (!isIlleagal(name))
            error = mContext.getString(R.string.illegal_name)
        if (isDuplicated(name))
            error = mContext.getString(R.string.duplicate_name)
        mFieldFolderName!!.error = error
        return (error == null)
    }

    fun checkPassword(): Boolean{
        if (mFieldPassword == null || mFieldPassword?.editText == null) return false
        return checkPassword(mFieldPassword!!.editText!!.text.toString())
    }

    fun checkPassword(password: String): Boolean{
        if (password.equals(""))    return false
        return true
    }
}