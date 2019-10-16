package com.transcend.otg.floatingbtn

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.transcend.otg.R

class BottomSheetFragment : BottomSheetDialogFragment() {

    private var mProgressBar: ProgressBar? = null
    private var mText: TextView? = null
    private var mProcess: TextView? = null
    private var mHide: TextView? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        initView()
    }

    fun initView(){
        //Set the custom view
        val view = LayoutInflater.from(context).inflate(R.layout.backup_bottom_sheet, null)
        mProgressBar = view.findViewById(R.id.bottom_sheet_progressBar)
        mText = view.findViewById(R.id.bottom_sheet_text)
        mProcess = view.findViewById(R.id.bottom_sheet_process)
        mHide = view.findViewById(R.id.bottom_sheet_hide)
        dialog?.setContentView(view)

        mHide?.setOnClickListener((object: View.OnClickListener{
            override fun onClick(v: View?) {
                dismissAllowingStateLoss()
            }
        }))
    }

    fun setProcessText(text: String){
        mProcess?.text = text
    }

    fun setProgressMax(max: Int){
        mProgressBar?.max = max
    }

    fun setProgressValue(value: Int){
        mProgressBar?.progress = value
    }
}