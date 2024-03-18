package xyz.brilliant.argpt.network

import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import xyz.brilliant.argpt.utils.Constant


object RequestAPICallBack {
        fun apiCallBackRequest(requestBody: RequestBody,token:String,apiUrl:String,callback:Callback){
            val client = OkHttpClient()
            val fullUrl = "${Constant.BASE_URL}$apiUrl"
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", token)
                .post(requestBody)
                .build()
            client.newCall(request).enqueue(callback)
        }

        fun apiCallBackMultiPartRequest(requestBody: MultipartBody,token:String,apiUrl:String,callback:Callback){
            val client = OkHttpClient()
            val fullUrl = "${Constant.BASE_URL}$apiUrl"
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", token)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(callback)
        }
}