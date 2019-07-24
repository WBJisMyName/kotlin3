package com.wbj.kotlin3.viewmodels

import android.database.Observable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.wbj.kotlin3.BuildConfig
import com.wbj.kotlin3.FeedbackFragment
import jcifs.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import javax.net.ssl.HttpsURLConnection

class FeedbackViewModel : ViewModel() {
    private val TAG = FeedbackFragment::class.java!!.getSimpleName()

    private val ID_ERROR_HANDLING = -1
    private val ID_GET_CATEGORY_LIST = 0
    private val ID_SEND_FEEDBACK = 1

    private val CATEGORY_LIST_URL = "https://www.transcend-info.com/Service/SMSService.svc/webssl/GetSrvCategoryList"

    private val XML_PARSER_NAME = "CName"
    private val XML_PARSER_TYPE = "CType"
    private val XML_PARSER_CATEGORY = "WebCatNo"
    private var XML_PARSER_TAG = "USB Flash Drives"
    private val REGION = "Taiwan"
    private var PRODUCT_NAME = "Android Transcend Elite"

    private val KEY_SERVICE_TYPE = "service_type"          //Server 給的值，可從XML解析得到
    private val KEY_SERVICE_CATEGORY = "service_category"//Server 給的值，可從XML解析得到

    private val FEEDBACK_URL = "https://www.transcend-info.com/Service/SMSService.svc/webssl/ServiceMailCaseAdd"

    private var AppVersion = BuildConfig.VERSION_NAME

    private val _name = MutableLiveData<String>()
    private val _email = MutableLiveData<String>()
    private val _sn = MutableLiveData<String>()
    private val _message = MutableLiveData<String>()
    private val _hint = MutableLiveData<String>()
    private val _send = MutableLiveData<String>()

    val name:LiveData<String> = _name
    val email:LiveData<String> = _email
    val sn:LiveData<String> = _sn
    val message:LiveData<String> = _message
    val send:LiveData<String> = _send


    val hint:LiveData<String> = _hint

    val nameText = ObservableField<String>()
    val emailText = ObservableField<String>()
    val snText = ObservableField<String>()
    val messageText = ObservableField<String>()
    val sendEnable = ObservableField<Boolean>()
    val progress = ObservableInt(View.GONE)

    init {
        _name.value = "title"
        _email.value = "E-mail"
        _sn.value = "sn"
        _message.value = "Message"
        _hint.value = "必填"
        _send.value = "傳送"
    }

    fun afterNameChanged(s: CharSequence){
        nameText.set(s.toString())
        checkTextInsert()
    }

    fun afterEmailChanged(s: CharSequence){
        emailText.set(s.toString())
        checkTextInsert()
    }

    fun afterSNChanged(s: CharSequence){
        snText.set(s.toString())
    }

    fun afterMessageChanged(s: CharSequence){
        messageText.set(s.toString())
        checkTextInsert()
    }

    fun checkTextInsert(){
        sendEnable.set(checkName() && checkEmail() && checkMessage())
    }

    fun checkName() : Boolean{
        return !(nameText.get().isNullOrEmpty())
    }

    fun checkEmail() : Boolean{
        if(emailText.get().isNullOrEmpty())return false
        else return Patterns.EMAIL_ADDRESS.matcher(emailText?.get()).matches()
    }

    fun checkMessage() : Boolean{
        return !(messageText.get().isNullOrEmpty())
    }

    fun onSendClick(){
        progress.set(View.VISIBLE)
        sendRequest(configurePostRequest(CATEGORY_LIST_URL), null, ID_GET_CATEGORY_LIST)
    }

    fun sendRequest(connection: HttpURLConnection?, jsonData: String?, messageId: Int) {
        if (connection == null) {
            return
        }

        Thread(Runnable {
            val msg = Message()
            try {
                val out = connection.outputStream
                if (jsonData != null) {
                    out.write(jsonData.toByteArray())
                }
                out.flush()
                out.close()

                val responseCode = connection.responseCode
//                Log.d(TAG, "param: " + jsonData!!)
//                Log.d(TAG, "responseCode: $responseCode")

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val result = getResponseResult(connection)
                    Log.d(TAG, "response result: $result")

                    if (messageId == ID_GET_CATEGORY_LIST && !parserCategoryData(msg, result)) {
                        doErrorHandling()
                        return@Runnable
                    }

                    msg.what = messageId
                    mHandler.sendMessage(msg)
                }

            } catch (e: IOException) {//out.close();
                Log.d(TAG, "IOException========================================================================")
                e.printStackTrace()
                doErrorHandling()
                Log.d(TAG, "IOException========================================================================")
            }
        }).start()

    }

    @Throws(IOException::class)
    private fun getResponseResult(connection: HttpURLConnection): String {
        val _in = BufferedReader(InputStreamReader(connection.inputStream))
        val line = _in.readLine()

        Log.d(TAG, "get category list result: $line")
        _in.close()
        return line
    }

    private fun parserCategoryData(msg: Message, responseData: String?): Boolean {
        //Parser response data to get two values: CType, WebCatNo
        val mKeys = HashMap<String, String>()
        mKeys[XML_PARSER_TYPE] = KEY_SERVICE_TYPE
        mKeys[XML_PARSER_CATEGORY] = KEY_SERVICE_CATEGORY

        val data = Bundle()
        if (responseData != null) {
            val factory: XmlPullParserFactory

            try {
                factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(StringReader(responseData))
                var eventType = parser.eventType
                var curTagName: String? = null
                var startAdd = false

                do {
                    val tagName = parser.name
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            val text = parser.text
                            if (startAdd) {
                                for (key in mKeys.keys) {
                                    if (curTagName == key) {
                                        Log.d(TAG, "Test: " + key + ", " + mKeys[key] + ", " + text)
                                        data.putString(mKeys[key], text)
                                        mKeys.remove(key)
                                        break
                                    }
                                }

                                if (mKeys.size == 0)
                                    break
                            } else {
                                startAdd = XML_PARSER_NAME == curTagName && XML_PARSER_TAG == text
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        curTagName = null
                    }
                    eventType = parser.next()
                } while (eventType != XmlPullParser.END_DOCUMENT)

            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else {
            //Use the default value to send feedback
            data.putString(KEY_SERVICE_TYPE, 15.toString())
            data.putString(KEY_SERVICE_CATEGORY, 384.toString())
            mKeys.clear()
        }

        msg.data = data
        return mKeys.size == 0
    }

    fun configurePostRequest(requestUrl: String): HttpURLConnection? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(requestUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("content-type", "application/json")
            conn.doOutput = true
            conn.readTimeout = 10000
            conn.connectTimeout = 10000
        } catch (e: Exception) {
            Log.d(TAG, "HttpURLConnection========================================================================")
            e.printStackTrace()
            doErrorHandling()
            Log.d(TAG, "HttpURLConnection========================================================================")
        }

        return conn
    }

    fun doErrorHandling() {
        val msg = Message()
        msg.what = ID_ERROR_HANDLING
        mHandler.sendMessage(msg)
    }

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                ID_GET_CATEGORY_LIST -> {
                    val feedbackData = getFeedbackData(msg)
                    if (feedbackData != null) {
                        sendRequest(configurePostRequest(FEEDBACK_URL), feedbackData, ID_SEND_FEEDBACK)
                    }
                }
                ID_SEND_FEEDBACK -> {
                    progress.set(View.GONE)
                    Log.d("jjj","ok")

                }
                ID_ERROR_HANDLING -> {
                    progress.set(View.GONE)
                    Log.d("jjj","fail")

                }
            }
        }

        private fun getFeedbackData(msg: Message): String? {
            var jsonData: String? = null
            if (msg.data != null) {
                val srvType = msg.data.getString(KEY_SERVICE_TYPE)
                val srvCategory = msg.data.getString(KEY_SERVICE_CATEGORY)
                val platformInfo = "App v" + AppVersion + " OS version: " + Build.VERSION.SDK_INT +
                        " device title: " + getAndroidDeviceName()

                jsonData = "{\"DataModel\":{\"CustName\":\"" + nameText.get()?.toString() + "\"" +
                        ",\"CustEmail\":\"" + emailText.get()?.toString() + "\"" +
                        ",\"Region\":\"" + REGION + "\"" +
                        ",\"ISOCode\":\"TW\"" +
                        ",\"Request\":\"" + platformInfo + "\"" +
                        ",\"SrvType\":\"" + srvType + "\"" +
                        ",\"SrvCategory\":\"" + srvCategory + "\"" +
                        ",\"ProductName \":\"" + PRODUCT_NAME + "\"" +
                        ",\"SerialNo\":\"" + "A123456789" + "\"" +
                        ",\"LocalProb\":\"" + Base64.encode(messageText.get()?.toByteArray()) + "\"}}"
            }
            return jsonData
        }
    }

     fun getAndroidDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }
}
