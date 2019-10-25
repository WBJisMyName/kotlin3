package com.transcend.otg.browser

import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant
import java.util.*


class DropDownAdapter : BaseAdapter() {

    internal var TAG = DropDownAdapter::class.java.simpleName

    private var mDropdown: Spinner? = null
    private var mList: MutableList<String>

    private var dropdownItemListener: OnDropdownItemSelectedListener? = null

    interface OnDropdownItemSelectedListener {
        fun onDropdownItemSelected(path: String)
    }

    init {
        mList = ArrayList()
    }

    fun setDropdowonList(dropdownList: List<String>){
        mList.clear()
        mList.addAll(dropdownList)
        notifyDataSetChanged()
    }

    fun setOnDropdownItemSelectedListener(listener: OnDropdownItemSelectedListener) {
        dropdownItemListener = listener
    }

    override fun getCount(): Int {
        return mList.size
    }

    override fun getItem(position: Int): Any {
        return mList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var mView = convertView
        if (parent is Spinner)
            mDropdown = parent
        if (mView == null) {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.history_dropdown_page, parent, false)
            mView = view.findViewById(R.id.dropdown_text)
        }

        (mView as TextView).run {
            text = mList[0]
            textSize = 18f
            setTextColor(Color.WHITE)
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.valueOf("END") //內容過長，End部位以...省略
        }
        return mView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mView = convertView
        if (mView == null) {
            val inflater = LayoutInflater.from(parent.context)
            mView = inflater.inflate(R.layout.history_dropdown_page, parent, false)
        }
        val tv: TextView = mView!!.findViewById(R.id.dropdown_text)
        tv.text = mList[position]

        val iv: ImageView = mView.findViewById(R.id.dropdown_icon)

        if (position == 0) //目前位置
            iv.setImageResource(R.drawable.ic_tab_location_solid_grey)
        else if (position == mList.size - 1) //根目錄位置
            iv.setImageResource(R.drawable.ic_tab_home_solid_grey)
        else //其餘皆以資料夾顯示
            iv.setImageResource(R.drawable.ic_tab_folder_solid_grey)

        val layout_manager: LinearLayout = mView.findViewById(R.id.dropdown_layout_manager)
        tv.run {
            textSize = 16f
            setSingleLine(true)
            setEllipsize(TextUtils.TruncateAt.valueOf("END"))
        }

        layout_manager.setOnTouchListener(OnDropdownItemTouchListener(position))
        return mView
    }

    inner class OnDropdownItemTouchListener(private val mPosition: Int) : View.OnTouchListener {

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                dismissDropdownList()
                if (dropdownItemListener != null) {
                    var path = ""
                    for(i in mPosition .. (mList.size - 1)){
                        if (path.equals(""))
                            path = mList[i]
                        else
                            path = mList[i] + "/" + path
                    }
                    dropdownItemListener!!.onDropdownItemSelected(path.replace(Constant.BrowserMainPageTitle, Constant.LOCAL_ROOT))
                }
            }
            return true
        }

        /**
         * In order to make dropdown list scrollable,
         * onDropdownItemSelected callback should be called in ACTION_UP instead of ACTION_DOWN.
         * That causes one problem that dropdown list would not dismiss automatically.
         * One solution is to detach spinner from window by reflection method,
         * and dropdown list will disappear.
         */
        private fun dismissDropdownList() {
            try {
                val method = Spinner::class.java.getDeclaredMethod("onDetachedFromWindow")
                method.isAccessible = true
                method.invoke(mDropdown)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}
