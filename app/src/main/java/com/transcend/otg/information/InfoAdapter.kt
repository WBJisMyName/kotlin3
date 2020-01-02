package com.transcend.otg.information

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.transcend.otg.R
import com.transcend.otg.utilities.Constant
import com.transcend.otg.utilities.MainApplication
import com.transcend.otg.utilities.UnitConverter

class InfoAdapter(val context: Activity): RecyclerView.Adapter<InfoAdapter.ViewHolder>() {

    private val Recycler_View_Content = 0
    private val Recycler_View_Map = 1
    private val Recycler_View_Thumbnail = 2

    val mList = ArrayList<InfoContentItem>()
    var mType = -1

    private var mMap: GoogleMap? = null
    private val thumbnailBitmap: Bitmap? = null

    var mPortraitScreenWidth: Int = 0

    init {
        //目前好像只能在activity取得螢幕寬度
        val displaymetrics = DisplayMetrics()
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics)
        mPortraitScreenWidth = displaymetrics.widthPixels
//        mPortraitScreenHeight = displaymetrics.heightPixels
    }

    fun setData(info: ImageInfo?){
        if (info == null)
            return
        mType = Constant.TYPE_IMAGE
        val pathContent = InfoContentItem()
        pathContent.title = info.path
        pathContent.iconResID = R.drawable.ic_mediainfo_folder_grey
        mList.add(pathContent)
        if(info.time_title != null) {
            val timeContent = InfoContentItem()
            timeContent.iconResID = R.drawable.ic_photoinfo_date_grey
            timeContent.title = info.time_title
            timeContent.subtitle = info.time_subtitle
            mList.add(timeContent)
        }
        val nameContent = InfoContentItem()
        nameContent.iconResID = R.drawable.ic_photoinfo_name_grey
        nameContent.title = info.name_title
        if (info.name_subtitle != null)
            nameContent.subtitle = info.name_subtitle
        mList.add(nameContent)
        if (info.device_title != null) {
            val deviceContent = InfoContentItem()
            deviceContent.iconResID = R.drawable.ic_photoinfo_lensinfo_grey
            deviceContent.title = info.device_title
            if(info.device_subtitle != null)
                deviceContent.subtitle = info.device_subtitle
            mList.add(deviceContent)
        }
        if (info.location_title != null) {
            val locateContent = InfoContentItem()
            locateContent.iconResID = R.drawable.ic_photoinfo_map_grey
            locateContent.title = info.location_title
            if (info.location_subtitle != null)
                locateContent.subtitle = info.location_subtitle
            mList.add(locateContent)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mLayoutInflater = LayoutInflater.from(parent.context)
        if (viewType == Recycler_View_Content)
            return ViewHolder(mLayoutInflater.inflate(R.layout.info_adapter_content, parent, false), viewType)
        else if (viewType == Recycler_View_Map)
            return ViewHolder(mLayoutInflater.inflate(R.layout.info_adapter_map, parent, false), viewType)
        else  //Recycler_View_Thumbnail
            return ViewHolder(mLayoutInflater.inflate(R.layout.info_adapter_thumbnail, parent, false), viewType)
    }

    override fun getItemCount(): Int {
        if (mType == -1)
            return 0
        else if (mType == Constant.TYPE_IMAGE){
            if (mList.get(mList.size - 1).iconResID == R.drawable.ic_photoinfo_map_grey)   //表示有地圖資訊
                return mList.size + 1   //多留一個位置給地圖
            else
                return mList.size
        } else if (mType == Constant.TYPE_MUSIC){

        } else if (mType == Constant.TYPE_VIDEO){

        } else if (mType == Constant.TYPE_DOC){

        } else if (mType == Constant.TYPE_DIR){

        } else {

        }
        return mList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (mType == -1)
            return Recycler_View_Content
        else if (mType == Constant.TYPE_IMAGE){
            if (position == mList.size)   //只有有地圖資訊的position會達到跟列表大小一樣
                return Recycler_View_Map
            else
                return Recycler_View_Content
        } else if (mType == Constant.TYPE_MUSIC){

        } else if (mType == Constant.TYPE_VIDEO){

        } else if (mType == Constant.TYPE_DOC){

        } else if (mType == Constant.TYPE_DIR){

        } else {

        }
        return Recycler_View_Content
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.itemViewType == Recycler_View_Map) {
            if (position == 0) return
            val locateInfo = mList.get(mList.size - 1)
            if (locateInfo.title == null || locateInfo.subtitle == null) return

            val locate = locateInfo.subtitle!!.split(",")
            val latitude = locate[0].toDouble()
            val longitude = locate[1].toDouble()

            holder.getMapFragmentAndCallback(OnMapReadyCallback { googleMap ->
                mMap = googleMap
                val converter = UnitConverter(context)
                //Activity先送圖檔，此處應該已經有圖，但還是防呆
                if (thumbnailBitmap != null)
                    holder.imageThumbnail!!.setImageBitmap(thumbnailBitmap)
                else
                    holder.imageThumbnail!!.setImageResource(R.drawable.ic_photoinfo_name_grey)
                //禁止拖動及移動地圖
                mMap!!.getUiSettings().setAllGesturesEnabled(false)
                val sydney = LatLng(latitude, longitude)
                //mMap.addMarker(new MarkerOptions().position(sydney).title(mFileInfo.name));
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16f))
                val lp = holder.mapLayout!!.layoutParams as MarginLayoutParams
                lp.setMargins(
                    converter.convertPixelToDp(40f).toInt(),
                    converter.convertPixelToDp(20f).toInt(),
                    converter.convertPixelToDp(40f).toInt(),
                    converter.convertPixelToDp(50f).toInt()
                )
                holder.mapLayout!!.layoutParams = lp
                holder.mapLayout!!.layoutParams.height = mPortraitScreenWidth - 40 * 2
                holder.imageThumbnail!!.layoutParams.width = converter.convertPixelToDp(250f).toInt()
                holder.imageThumbnail!!.layoutParams.height = converter.convertPixelToDp(250f).toInt()
                mMap!!.setOnMapClickListener(OnMapClickListener {
                    Log.e("Map", "Clicked!")
//                    val dialog = PopDialog(context)
//                    dialog.setTitle(context.getString(R.string.app_name))
//                    dialog.setMessage(context.getString(R.string.open_map_app))
//                    val confirm = Button(context)
//                    val cancel = Button(context)
//                    confirm.text = context.getString(R.string.confirm)
//                    confirm.setOnClickListener {
//                        val context: Context = mContext.get()
//                        launchGoogleMapApp(
//                            (context as Activity),
//                            mData.latitude,
//                            mData.longitude,
//                            mData.file_name
//                        )
//                    }
//                    cancel.text = context.getString(R.string.cancel)
//                    dialog.setPositiveBtn(confirm)
//                    dialog.setNegativeBtn(cancel)
//                    dialog.buildNormalDialog()
//                    dialog.show()
                })
            })
            return
        }
        else if(holder.itemViewType == Recycler_View_Content) {
            if (position >= mList.size)
                return
            adjustContentUI(MainApplication.getInstance()!!.applicationContext, holder)
            when (mType) {
                Constant.TYPE_IMAGE -> {
                    //若沒有sub title，且位置須改為置中。最多三行
                    if (mList[position].subtitle == null || mList[position].subtitle!!.length == 0) {
                        holder.title!!.gravity = Gravity.CENTER_VERTICAL
                        holder.title!!.isSingleLine = false
                        holder.title!!.maxLines = 3
                        holder.subtitle!!.visibility = View.GONE
                    } else {
                        holder.subtitle!!.text = mList[position - 1].subtitle
                    }

                    holder.title!!.text = mList[position].title
                    holder.subtitle!!.text = mList[position].subtitle
                    holder.icon!!.setImageResource(mList[position].iconResID)

                    //地址欄位最多擴增到2行
                    if (mList[position].iconResID == R.drawable.ic_photoinfo_map_grey) {
                        holder.title!!.isSingleLine = false
                        holder.title!!.maxLines = 2
                        val lp = holder.title_layout!!.layoutParams as MarginLayoutParams
                        lp.setMargins(50, 30, 50, 0)
                        holder.title_layout!!.layoutParams = lp
                    }
                }
            }
        }
    }

    private fun adjustContentUI(context: Context, holder: ViewHolder) {
        val converter = UnitConverter(context)
        var lp = holder.icon!!.getLayoutParams() as MarginLayoutParams
        lp.setMargins(converter.convertPixelToDp(32f).toInt(), 0, 0, 0)
        holder.icon!!.setLayoutParams(lp)
        holder.icon!!.getLayoutParams().width = converter.convertPixelToDp(76f).toInt()
        holder.icon!!.getLayoutParams().height = converter.convertPixelToDp(76f).toInt()
        lp = holder.title_layout!!.getLayoutParams() as MarginLayoutParams
        lp.setMargins(
            converter.convertPixelToDp(44f).toInt(), 0,
            converter.convertPixelToDp(50f).toInt(), 0)
        holder.title_layout!!.setLayoutParams(lp)
        //        holder.item_manager.getLayoutParams().height = (int) converter.convertPixelToDp(135);
        holder.title!!.setTextSize(converter.convertPtToSp(36f))

        holder.title!!.setTextColor(ContextCompat.getColor(context, R.color.c_02))
        holder.subtitle!!.setTextSize(converter.convertPtToSp(28f))
        holder.subtitle!!.setTextColor(ContextCompat.getColor(context, R.color.c_03))
    }

    //開啟google map
    private fun launchGoogleMapApp(context: Activity, lat: Double, lon: Double, filename: String) {
        val mapUri = Uri.parse("geo:$lat,$lon?q=($filename)@$lat,$lon")
        val returnIt = Intent(Intent.ACTION_VIEW, mapUri)
        context.startActivity(returnIt)
    }

    inner class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
//        var mItemView: View
//        var mViewType: Int
        var item_manager: View? = null  //整個item的layout
        var title_layout: View? = null  //放title、subtitle(只有list)的地方
        var icon: ImageView? = null
        var title: TextView? = null
        var subtitle: TextView? = null
        var mapFragment: SupportMapFragment? = null
        var imageThumbnail: ImageView? = null
        var mapLayout: RelativeLayout? = null
        var mediaThumbnail: ImageView? = null
        override fun onClick(v: View) {}

        init {
//            mItemView = itemView
//            mViewType = viewType
            if (viewType == Recycler_View_Map) {
                mapLayout = itemView.findViewById<View>(R.id.mapLayout) as RelativeLayout
                imageThumbnail = itemView.findViewById<View>(R.id.imageThumbnail) as ImageView
            } else if (viewType == Recycler_View_Thumbnail) {
                mediaThumbnail = itemView.findViewById(R.id.info_thumbnail)
            } else {
                item_manager = itemView.findViewById(R.id.item_manage) as View
                title_layout = itemView.findViewById(R.id.item_title_layout) as View
                icon = itemView.findViewById<View>(R.id.item_icon) as ImageView
                title = itemView.findViewById<View>(R.id.item_title) as TextView
                subtitle = itemView.findViewById<View>(R.id.item_subtitle) as TextView
            }
        }

        fun getMapFragmentAndCallback(callback: OnMapReadyCallback?): SupportMapFragment? {
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                mapFragment!!.getMapAsync(callback)
            }

            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.map, mapFragment!!).commit()
            return mapFragment
        }
    }
}