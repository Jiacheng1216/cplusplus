package com.example.dofbot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.timer

var dataid = ""
class Authentication : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val back = findViewById<Button>(R.id.backtoregister) //返回上一頁
        val sentcode = findViewById<Button>(R.id.sentcode) //重發
        val verify = findViewById<Button>(R.id.verify)    //驗證
        val verifyEdit = findViewById<EditText>(R.id.verifyEdit)
        val textView3 = findViewById<TextView>(R.id.textView3)

        //返回
        back.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        //驗證成功
        fun success(){
            val returnlogin = Intent(this, MainActivity::class.java)
            startActivity(returnlogin)
            /*
            AlertDialog.Builder(this)
                .setTitle("註冊成功!")
                .setMessage("請輸入你的email和密碼進行登入")
                .setPositiveButton("右按鈕") { dialog, which ->
                }.show()*/
        }

        //驗證
        verify.setOnClickListener {
            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
            val param = "{\"code\" : ${verifyEdit.text}}"
            val body = param.toRequestBody(type)
            val readtoken = getSharedPreferences("token",0)

            val Url = "http://35.77.46.57:3000/user/verified"
            val req = Request.Builder().url(Url).addHeader("token",readtoken.getString("EMAILTOKEN", "").toString()).post(body).build()

            OkHttpClient().newCall(req).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string() //server回應訊息
                    val json = JSONObject(res.toString())

                    when(json.getInt("status")){
                        422 -> textView3.text = "錯誤的驗證碼"
                        200 -> success()
                    }

                    Log.e("id",dataid)
                    Log.e("token",readtoken.getString("EMAILTOKEN", "").toString())
                    Log.e("123","$res")

                    //403:missing token
                    //422:錯誤驗證碼
                    //200:驗證成功
                }

                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
            })
        }

        //重發驗證碼
        sentcode.setOnClickListener {
            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
            val param = "{}"
            val body = param.toRequestBody(type)
            val Url = "http://35.77.46.57:3000/user/resend/$dataid"
            val req = Request.Builder().url(Url).post(body).build()

            val timer = object: CountDownTimer(600000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    textView3.text = "驗證碼已重發，將在${millisUntilFinished/1000}秒後失效"
                }
                override fun onFinish() {
                    textView3.text = "驗證碼已失效"
                }
            }
            timer.start()



            OkHttpClient().newCall(req).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string() //server回應訊息
                    val headData = response.headers //取得server的token值
                    val token = headData.get("token").toString() //儲存token值成變數
                    val settings = getSharedPreferences("token",0)  //儲存token到SharedPreferences
                    settings.edit().putString("TOKEN", token).apply()

                    Log.e("response",res.toString())
                    Log.e("token",token)
                }

                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
            })

        }

    }
}