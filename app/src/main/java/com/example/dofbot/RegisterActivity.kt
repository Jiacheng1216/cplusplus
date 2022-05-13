package com.example.dofbot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbrw: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerBackToLogin = findViewById<TextView>(R.id.registerBackToLogin)
        val account = findViewById<EditText>(R.id.account)
        val password = findViewById<EditText>(R.id.password)
        val email = findViewById<EditText>(R.id.email)
        val register = findViewById<Button>(R.id.register)
        val checkpassword = findViewById<EditText>(R.id.checkpassword)
        val checkBox = findViewById<ImageView>(R.id.checkBox)
        var check = false

        //我不是機器人，確認框
        checkBox.setOnClickListener {
            if (check){
                checkBox.setImageResource(R.drawable.ic_baseline_check_box_outline_blank_24)
                check = false
            }else{
                check = true
                checkBox.setImageResource(R.drawable.ic_baseline_check_box_24)
            }
        }

        //取得資料庫實體
        dbrw = Database(this,"TOKEN").writableDatabase

        //返回上一頁
        registerBackToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //註冊成功，切換到驗證頁面
        fun authentication(){
            val intent = Intent(this, Authentication::class.java)
            //將email發送到驗證畫面，讓資料庫搜尋，KEY為EMAIL
            intent.putExtra("EMAIL",email.text.toString())
            startActivity(intent)
            showToast("註冊成功，請在10分鐘內輸入驗證碼")
            finish()
        }

        //註冊
        register.setOnClickListener {
            when{
                //檢查欄位是否正確輸入
                account.length() < 1 -> showToast("請輸入帳號!")
                password.length() < 1 -> showToast("請輸入密碼!")
                checkpassword.length() < 1 -> showToast("請輸入確認密碼")
                email.length() < 1 -> showToast("請輸入Email!")
                password.text.toString() != checkpassword.text.toString() -> {
                    showToast("兩次密碼不一致")
                }
                !check -> showToast("請勾選我不是機器人")
                else -> {
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{\"email\" : \"${email.text}\" ," +
                                " \"username\" : \"${account.text}\" ," +
                                " \"password\" : \"${password.text}\"}"
                    val body = param.toRequestBody(type)

                    val Url = "http://35.77.46.57:3000/user/register"
                    val req = Request.Builder().url(Url).post(body).build()

                    OkHttpClient().newCall(req).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string() //server回應
                            val json = JSONObject(res.toString()) //server回應轉成JSON格式

                            if (json.has("data")) {
                                val data = json.getString("data") //取其中的data
                                val datajson = JSONObject(data.toString()) //data轉成JSON格式
                                dataId = datajson.getString("_id") //將id值存放到dataid變數
                            }
                            //取得server回傳的token值
                            val headData = response.headers
                            //儲存token值成字串變數
                            val token = headData.get("token").toString()

                            Log.e("server回應", "$json")

                            when (json.getInt("status")) {
                                201 -> {
                                    //將email和token值新增到資料庫
                                    dbrw.execSQL(
                                        "INSERT INTO tokenList(email,token) VALUES('${email.text}','$token')",
                                    )
                                    authentication()//註冊成功
                                }
                                409 -> showToast(json.getString("message")) //已存在
                                422 -> showToast(json.getString("message")) //格式錯誤
                                500 -> showToast(json.getString("message")) //server錯誤
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                            showToast("連線失敗")
                        }
                    })
                }
            }
        }
    }
    fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}