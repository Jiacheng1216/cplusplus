package com.example.dofbot

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //返回上一頁
        val registerBackToLogin = findViewById<Button>(R.id.registerBackToLogin)
        registerBackToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val account = findViewById<EditText>(R.id.account)
        val password = findViewById<EditText>(R.id.password)
        val email = findViewById<EditText>(R.id.email)
        val register = findViewById<Button>(R.id.register)

        //切換到驗證頁面
        fun authentication(){
            startActivity(Intent(this, Authentication::class.java))
        }

        register.setOnClickListener {
            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
            val param = "{\"email\" : \"${email.text}\" , \"username\" : \"${account.text}\" , \"password\" : \"${password.text}\"}"
            val body = param.toRequestBody(type)

            val Url = "http://35.77.46.57:3000/user/register"
            val req = Request.Builder().url(Url).post(body).build()
            val respon = findViewById<TextView>(R.id.respon)

            when{
                account.text.length < 1 -> respon.setText("請輸入帳號!")
                password.text.length < 1 -> respon.setText("請輸入密碼!")
                email.text.length < 1 -> respon.setText("請輸入Email!")
                else ->
                    OkHttpClient().newCall(req).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val res = response.body?.string() //server回應
                        val json = JSONObject(res.toString()) //server回應轉成JSON格式

                        when(json.getInt("status")){
                            201 -> authentication() //註冊成功
                            409 -> respon.setText(json.getString("message")) //已存在
                            422 -> respon.setText(json.getString("message")) //格式錯誤
                            500 -> respon.setText(json.getString("message")) //server錯誤
                        }

                        if(json.has("data")){
                            val data = json.getString("data") //取其中的data
                            val datajson = JSONObject(data.toString()) //data轉成JSON格式
                            dataid = datajson.getString("_id") //將id值存放到dataid變數
                        }

                        val headData = response.headers //取得server的token值
                        val token = headData.get("token").toString() //儲存token值成變數
                        val settings = getSharedPreferences("token",0)  //儲存token到SharedPreferences
                        settings.edit().putString("EMAILTOKEN", token).apply()
                        val readtoken = getSharedPreferences("token",0)

                        Log.e("123", "$json")
                        Log.e("token","${readtoken.getString("EMAILTOKEN", "")}")



                    }

                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                        respon.setText("連線失敗")
                    }
                })
            }


        }
    }
}