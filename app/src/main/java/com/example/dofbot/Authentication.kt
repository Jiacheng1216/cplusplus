package com.example.dofbot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

var dataId = ""
class Authentication : AppCompatActivity() {
    private lateinit var dbrw: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val back = findViewById<Button>(R.id.backtoregister) //返回上一頁
        val sentCode = findViewById<Button>(R.id.sentcode) //重發
        val verify = findViewById<Button>(R.id.verify)    //驗證
        val verifyEdit = findViewById<EditText>(R.id.verifyEdit)
        val textView3 = findViewById<TextView>(R.id.textView3)
        //取得資料庫實體
        dbrw = Database(this,"TOKEN").writableDatabase

        //接收註冊頁面發送的email
        val email = intent.extras?.getString("EMAIL")
        //接收註冊頁面發送的_id
        val id = intent.extras?.getString("_id")

        //返回
        back.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        //驗證成功
        fun success(){
            val returnlogin = Intent(this, MainActivity::class.java)
            startActivity(returnlogin)
            showToast("註冊成功，請使用email和密碼登入")
            finish()
        }

        //驗證
        verify.setOnClickListener {
            when{
                verifyEdit.length() < 1 -> showToast("請輸入驗證碼")
                else -> {
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{\"code\" : ${verifyEdit.text}}"
                    val body = param.toRequestBody(type)

                    //從資料庫以email搜尋token值
                    val c = dbrw.rawQuery(
                        "SELECT * FROM tokenList WHERE email LIKE '$email'",
                        null)
                    //從第一筆開始搜尋
                    c.moveToFirst()

                    val url = "http://35.77.46.57:3000/user/verified"
                    val req = Request
                        .Builder()
                        .url(url)
                        //將搜尋到的token值發送給server作驗證
                        .addHeader("token",c.getString(1))
                        .post(body)
                        .build()

                    OkHttpClient().newCall(req).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string() //server回應訊息
                            val json = JSONObject(res.toString())
                            //取得伺服器回傳的新token值
                            val headData = response.headers
                            //儲存新token值成字串變數
                            val token = headData.get("token").toString()

                            if (json.has("status")) {
                                when (json.getInt("status")) {
                                    422 -> showToast(json.getString("message"))
                                    200 -> {
                                        success()
                                    }
                                }
                            }

                            Log.e("123", "$res")
                            Log.e("token",c.getString(1))
                            Log.e("Email","$email")

                            //403:missing token
                            //422:錯誤驗證碼
                            //200:驗證成功
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            TODO("Not yet implemented")
                        }
                    })

                }
            }


        }

        //重發驗證碼
        sentCode.setOnClickListener {
            //60秒後可重發驗證碼
            var countTime = 60
            //初始化有效時間
            var effectTime = 0
            sentCode.isEnabled = false
            GlobalScope.launch(Dispatchers.Main) {
                while (countTime > 0) {
                    countTime--
                    sentCode.text = "$countTime" + "秒後才能重發"
                    delay(1000)
                    sentCode.isEnabled = false
                }
                sentCode.isEnabled = true
                sentCode.text = "重新發送"
            }

            //設定有效時間
            effectTime = 600
            //驗證碼600秒後失效
            GlobalScope.launch(Dispatchers.Main) {
                while (effectTime > 0){
                    effectTime--
                    delay(1000)
                    textView3.text = "驗證碼已重發，將在$effectTime" + "秒後失效"
                }
                textView3.text = "驗證碼已失效"
            }

            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
            val param = "{}"
            val body = param.toRequestBody(type)

            val url = "http://35.77.46.57:3000/user/resend/$id"
            val req = Request.Builder().url(url).post(body).build()

            OkHttpClient().newCall(req).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string() //server回應訊息
                    val json = JSONObject(res.toString())

                    when(json.getInt("status")){
                        200 -> showToast(json.getString("message"))
                    }

                    Log.e("response","$json")
                    //Log.e("token",)
                }

                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }


}