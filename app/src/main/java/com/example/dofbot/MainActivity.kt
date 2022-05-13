package com.example.dofbot

import android.app.Activity
import android.content.Context
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
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.io.IOException

var connectSuccess = false

const val topic = "NTUT/MQTT"
class MainActivity : AppCompatActivity() {

    private lateinit var dbrw: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loginAccount = findViewById<EditText>(R.id.LoginAccount)
        val loginPassword = findViewById<EditText>(R.id.LoginPassword)
        val signUp = findViewById<TextView>(R.id.signUp)
        val signIn = findViewById<Button>(R.id.signIn)
        val button = findViewById<Button>(R.id.button)

        //取得資料庫實體
        dbrw = Database(this,"TOKEN").writableDatabase

        //資料庫測試
        button.setOnClickListener {
            startActivity(Intent(this,DataBaseTest::class.java))
        }

        //登錄成功，切換到用戶介面
        fun loginSuccess(id: ArrayList<String>, name: ArrayList<String>){
            //將id跟name跟email打包並傳給UserActivity
            val b = Bundle()
            //id跟name為陣列形式
            b.putStringArrayList("id", ArrayList(id))
            b.putStringArrayList("name", ArrayList(name))
            //將email傳給UserActivity做全域變數
            b.putString("LoginEmail",loginAccount.text.toString())

            val loginIntent = Intent(this,UserActivity::class.java)
            startActivity(loginIntent.putExtras(b))
            showToast("登錄成功!")
        }

        //403登錄失敗，跳到驗證畫面
        fun unAuthentication(verity: String){
            val authenticationIntent = Intent(this,Authentication::class.java)
            authenticationIntent.putExtra("EMAIL",loginAccount.text.toString())
            authenticationIntent.putExtra("_id",verity)
            startActivity(authenticationIntent)
            showToast("請先通過Email驗證")
        }

        //註冊
        signUp.setOnClickListener {
            startActivity(Intent(this,RegisterActivity::class.java))
        }
        //登錄
        signIn.setOnClickListener {
            when{
                //檢查輸入是否為空
                loginAccount.length() < 1 -> showToast("請輸入Email")
                loginPassword.length() < 1 -> showToast("請輸入密碼")
                else -> {
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param =
                        "{\"email\" : \"${loginAccount.text}\" ," +
                        " \"password\" : \"${loginPassword.text}\"}"
                    val body = param.toRequestBody(type)

                    val url = "http://35.77.46.57:3000/user/login"
                    val req = Request.Builder().url(url).post(body).build()

                    OkHttpClient().newCall(req).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string()
                            val json = JSONObject(res.toString())

                            if(json.has("data")){
                                //從server回傳的home name id紀錄 打包傳送到UserActivity
                                val data = json.getJSONObject("data")

                                if (data.has("homes")){
                                    val homes = data.getJSONArray("homes")
                                    var name = ""
                                    var id = ""

                                    //取得server的token值
                                    val headData = response.headers
                                    //儲存token值成變數
                                    val token = headData.get("token").toString()

                                    val c = dbrw.rawQuery(
                                        "SELECT * FROM tokenList WHERE email LIKE '${loginAccount.text}'",
                                        null)
                                    //從第一筆開始搜尋
                                    c.moveToFirst()

                                    when (json.getInt("status")) {
                                        200 -> {
                                            //如果資料庫都沒有資料
                                            if(c.count == 0){
                                                //將email和token值新增到資料庫
                                                dbrw.execSQL(
                                                    "INSERT INTO tokenList(email,token) VALUES('${loginAccount.text}','$token')",
                                                )
                                            }
                                            //如果資料庫有資料但是沒有該email資料
                                            else if(c.getString(0) != loginAccount.text.toString()) {
                                                //將email和token值新增到資料庫
                                                dbrw.execSQL(
                                                    "INSERT INTO tokenList(email,token) VALUES('${loginAccount.text}','$token')",
                                                )
                                            }else
                                            {
                                                //登錄後更新token值
                                                dbrw.execSQL("UPDATE tokenList SET token = '$token' WHERE email LIKE '${loginAccount.text}'")
                                            }
                                            //取得陣列元素數量
                                            val indexNumber = homes.length()-1
                                            //建立name和id的陣列
                                            val nameArray :ArrayList<String> = ArrayList()
                                            val idArray :ArrayList<String> = ArrayList()
                                            //依序存取各個家庭
                                            for(i in 0..indexNumber){
                                                val objecttt = homes.getJSONObject(i)
                                                //拆解後取得name和id，並依序加到陣列裡
                                                name = objecttt.getString("name")
                                                nameArray.add(name)
                                                id = objecttt.getString("_id")
                                                idArray.add(id)
                                            }
                                            //將完成的陣列作為引數並傳遞給函數做參數
                                            loginSuccess(idArray,nameArray)
                                            Log.e("data", "${json.getJSONObject("data")}")
                                        }
                                    }
                                }else if(data.has("_id")){
                                    when(json.getInt("status")){
                                        401 -> showToast("錯誤的email或密碼")
                                        //未驗證，跳到驗證畫面
                                        403 -> {
                                            unAuthentication(data.getString("_id"))
                                        }
                                    }

                                    Log.e("SERVER", "$json")
                                }
                            }
                            //Log.e("token", token)
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            showToast("server沒有回應")
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



