package com.wbj.kotlin3.data

class UrlDatabase private constructor(){

    var urlDao = UrlDao()
        private set

    companion object{
        @Volatile private var instance : UrlDatabase?=null

        fun getInstance() =
                instance ?: synchronized(this){
                    instance ?: UrlDatabase().also { instance = it }
                }
    }
}