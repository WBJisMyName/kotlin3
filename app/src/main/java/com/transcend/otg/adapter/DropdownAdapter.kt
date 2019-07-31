package com.transcend.otg.adapter

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.UnitConverter
import java.lang.ref.WeakReference
import java.util.*

class DropDownAdapter(context: Context, private val historyPage_arrow: ImageView) : BaseAdapter() {

    private var mDropdown: Spinner? = null
    private var mList: MutableList<String>? = null
    private val mContext: WeakReference<Context>

    //初始位置名稱
    private var PREFIX_MAINPAGE = "Fragment"

    private var dropdownItemListener: OnDropdownItemSelectedListener? = null

    val topText: String
        get() = if (mList!!.size > 0)
            mList!![0]
        else
            ""

    interface OnDropdownItemSelectedListener {
        fun onDropdownItemSelected(position: Int)
    }

    init {
        mList = ArrayList()
        mContext = WeakReference(context)
    }

    //設置初始目錄的標題
    fun setMainPage(title: String) {
        var count = 0
        for (a in mList!!) {
            if (a == PREFIX_MAINPAGE) {
                mList!![count] = title
                break
            }
            count++
        }
        PREFIX_MAINPAGE = title
        notifyDataSetChanged()
    }

    fun setOnDropdownItemSelectedListener(listener: OnDropdownItemSelectedListener) {
        dropdownItemListener = listener
    }

    //以路徑更新列表
    fun updateDropDownList(path: String) {
        var path = path
        path = path.replaceFirst(Constant.LOCAL_ROOT, PREFIX_MAINPAGE)//jj

        var list: MutableList<String> = ArrayList()
        val items = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        list = Arrays.asList(*items)
        Collections.reverse(list)
        mList = list
        notifyDataSetChanged()
    }

    //以列表更新列表
    fun updateDropDownList(list: MutableList<String>) {
        mList = list
    }

    fun resetList() {
        mList = ArrayList()
    }

    fun getPath(position: Int): String {
        val list = mList!!.subList(position, mList!!.size)
        Collections.reverse(list)
        val builder = StringBuilder()
        for (item in list) {
            builder.append(item)
            builder.append("/")
        }
        var path = builder.toString()

        path = path.replaceFirst(PREFIX_MAINPAGE.toRegex(), Constant.LOCAL_ROOT)//jj

        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        return path
    }

    override fun getCount(): Int {
        return mList!!.size
    }

    override fun getItem(position: Int): Any {
        return mList!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        var context = mContext.get()
        if (context != null) {//判斷有無被系統GC
            context = mContext.get()
            //可以執行到這，就表示 context 還未被系統回收，可繼續做接下來的任務
        } else
            return convertView

        if (parent is Spinner)
            mDropdown = parent
        if (convertView == null) {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.history_dropdown_page, parent, false)
            convertView = ViewHolder[view, R.id.dropdown_text]
        }

        (convertView as TextView).text = mList!![0]
        convertView.setTextColor(Color.WHITE)

        val converter = UnitConverter(context)
        convertView.textSize = converter.convertPtToSp(38f)
        convertView.setSingleLine(true)
        convertView.ellipsize = TextUtils.TruncateAt.valueOf("END") //內容過長，End部位以...省略

        if (mList!!.size == 1) {
            mDropdown!!.isEnabled = false
            historyPage_arrow.visibility = View.INVISIBLE
        } else {
            mDropdown!!.isEnabled = true
            historyPage_arrow.visibility = View.VISIBLE
        }

        return convertView
    }

    override fun getDropDownView(position: Int, convertview: View?, parent: ViewGroup): View {
        var convertView = convertview
        if (convertView == null) {
            val inflater = LayoutInflater.from(parent.context)
            convertView = inflater.inflate(R.layout.history_dropdown_page, parent, false)
        }
        val tv = ViewHolder.get<TextView>(convertView!!, R.id.dropdown_text)
        tv.setText(mList!![position])
        var iv = ViewHolder.get<ImageView>(convertView,  R.id.dropdown_icon)

        if (position == 0)
        //目前位置
            iv.setImageResource(R.drawable.ic_tab_location_solid_grey)
        else if (position == mList!!.size - 1)
        //根目錄位置
            iv.setImageResource(R.drawable.ic_tab_home_solid_grey)
        else
        //其餘皆以資料夾顯示
            iv.setImageResource(R.drawable.ic_tab_folder_solid_grey)

        var context = mContext.get()
        if (context != null) {//判斷有無被系統GC  可以移到前面就判斷
            context = mContext.get()
            //可以執行到這，就表示 context 還未被系統回收，可繼續做接下來的任務
        } else
            return convertView

        //UI調整
        val converter = UnitConverter(context)
        val layout_manager = ViewHolder.get<LinearLayout>(convertView!!, R.id.dropdown_layout_manager)
        layout_manager.getLayoutParams().height = converter.convertPixelToDp(88f).toInt()

        val lp = iv.getLayoutParams() as ViewGroup.MarginLayoutParams
        lp.setMargins(converter.convertPixelToDp(36f).toInt(), 0, converter.convertPixelToDp(20f).toInt(), 0)
        iv.setLayoutParams(lp)

        tv.setPadding(0, 0, converter.convertPixelToDp(72f).toInt(), 0)

        tv.setTextSize(converter.convertPtToSp(36f))
        tv.setSingleLine(true)
        tv.setEllipsize(TextUtils.TruncateAt.valueOf("END"))

        layout_manager.setOnTouchListener(OnDropdownItemTouchListener(position))
        //End of UI調整

        return convertView
    }

    object ViewHolder {
        operator fun <T : View> get(view: View, id: Int): T {
            var holder = SparseArray<View>()
            view.tag = holder
            var child: View? = holder.get(id)
            if (child == null) {
                child = view.findViewById(id)
                holder.put(id, child)
            }
            return child as T
        }
    }

    inner class OnDropdownItemTouchListener(private val mPosition: Int) : View.OnTouchListener {

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                if (dropdownItemListener != null) {
                    dropdownItemListener!!.onDropdownItemSelected(mPosition)
                }
                dismissDropdownList()
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
