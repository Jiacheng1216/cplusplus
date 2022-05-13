package com.example.dofbot

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.view.ViewGroup
import androidx.core.view.isGone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.reflect.Field

//登入後將email存放到全域變數，讓資料庫存取
var userEmail = ""

class UserActivity : AppCompatActivity() {
    private lateinit var adapter: Adapter
    private lateinit var dbrw: SQLiteDatabase
    private val contacts = ArrayList<Contact>()
    private val mqtt = MQTT()

    //建立列表功能，需要name跟id兩個參數
    private fun createList(name :String, id :String){
        contacts.add(Contact(name, id))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.extras?.let {
            if (requestCode == 1 && resultCode == Activity.RESULT_OK){
                val homeName = it.getString("name") ?: return@let
                val homeId = it.getString("id") ?: return@let
                createList(homeName,homeId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val createHome = findViewById<Button>(R.id.createHome)
        val joinHome = findViewById<Button>(R.id.joinHome)
        val setting = findViewById<ImageView>(R.id.setting)
        val connect = findViewById<ImageView>(R.id.connect)
        val disconnection = findViewById<ImageView>(R.id.disconnection)
        val controlArm = findViewById<ImageView>(R.id.controlArm)

        //接收登陸畫面傳過來的email
        val email = intent.extras?.getString("LoginEmail")
        //接收登陸畫面傳過來的name跟id陣列
        val name = intent.extras?.getStringArrayList("name")
        val id = intent.extras?.getStringArrayList("id")
        //存取兩個陣列大小再減一並用變數儲存
        val nameNumber = name?.size?.minus(1)
        val idNumber = id?.size?.minus(1)
        //如果大小相等
        if (nameNumber == idNumber){
            //依序建立列表
            for (i in 0..nameNumber!!) {
                createList(name[i], "${id?.get(i)}")
            }
        }

        //將email存放到全域變數
        userEmail = email.toString()

        val homeView = findViewById<RecyclerView>(R.id.homeView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        homeView.layoutManager = linearLayoutManager

        adapter = Adapter(contacts)
        homeView.adapter = adapter

        //存取資料庫
        dbrw = Database(this,"TOKEN").writableDatabase

        //資料庫搜尋token
        val c = dbrw.rawQuery(
            "SELECT * FROM tokenList WHERE email LIKE '$userEmail'",
            null)
        c.moveToFirst()

        //連接MQTT
        connect.setOnClickListener {
            if (connectSuccess){
                showToast("已連線")
            }else{
                mqtt.connect(this)
                showToast("連線中...")
            }
        }
        //中斷MQTT連線
        disconnection.setOnClickListener {
            if (connectSuccess){
                mqtt.disconnect()
                showToast("中斷連線")
            }else{
                showToast("尚未連線")
            }
        }
        //開始控制手臂
        controlArm.setOnClickListener {
            if (connectSuccess){
                startActivity(Intent(this, ControlArm::class.java))
            }else{
                showToast("還沒連線，請先點擊wifi圖片進行連線")
            }
        }

        //建立家庭
        createHome.setOnClickListener {
            startActivityForResult(Intent(this,CreateHome::class.java),1)
        }

        //加入現有家庭
        joinHome.setOnClickListener {
            val userJoinHome = layoutInflater.inflate(R.layout.user_joinhome,null,false)
            //輸入的文字框
            val joinHomeId = userJoinHome.findViewById<TextView>(R.id.joinHomeId)

            AlertDialog.Builder(this)
                .setTitle("請輸入ID")
                .setView(userJoinHome)
                .setPositiveButton("確認"){ dialog, which ->
                    //輸入完的id存放到變數
                    val keyId = joinHomeId.text.toString()

                    //body building
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{}"
                    val body = param.toRequestBody(type)

                    //向server發出request
                    val url = "http://35.77.46.57:3000/home/join/$keyId"
                    val request = Request.Builder()
                        .url(url)
                        //送出token
                        .addHeader("token",c.getString(1))
                        .patch(body)
                        .build()

                    //server response
                    OkHttpClient.Builder().build().newCall(request).enqueue(object : Callback{
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string()
                            val json = JSONObject(res.toString())

                            if(json.has("data")){
                                val data = json.getJSONObject("data")
                                val homeName = data.getString("name")

                                when (json.getInt("status")) {
                                    201 -> {
                                        //join success
                                        showToast(json.getString("message"))
                                        //建立listview
                                        createList(homeName,keyId)
                                        //更新頁面
                                        GlobalScope.launch(Dispatchers.Main) {
                                            //隱藏元件
                                            //findViewById<ImageView>(R.id.deleteHome).setVisibility(View.GONE)
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                }
                            }else{
                                when(json.getInt("status")){
                                    //id error & home already joined
                                    400 -> showToast(json.getString("message"))
                                }
                            }

                            Log.e("server","$json")
                            Log.e("token",c.getString(1))
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            showToast("請求失敗")
                        }
                    })

                }.create().show()
        }

        //修改密碼視窗
        setting.setOnClickListener {
            //綁定修改密碼的layout檔和元件
            val modifyPassword = layoutInflater
                .inflate(R.layout.activity_modify_password,null,false)
            val modUserName = modifyPassword.findViewById<EditText>(R.id.modusername)
            val modPassWord = modifyPassword.findViewById<EditText>(R.id.modpassword)
            val checkModifyPassword = modifyPassword.findViewById<EditText>(R.id.checkmodifypassword)

            fun successmodify(){
                showToast("修改成功，請重新登入")
                val returnlogin = Intent(this,MainActivity::class.java)
                startActivity(returnlogin)
                finish()
            }

            AlertDialog.Builder(this)
                .setTitle("請輸入新的用戶名和密碼")
                .setView(modifyPassword)
                .setPositiveButton("確認"){ dialog, which ->
                    when {
                        modUserName.length() < 1 -> {showToast("請輸入帳號");return@setPositiveButton }
                        modPassWord.length() < 1 -> {showToast("請輸入密碼"); return@setPositiveButton}
                        checkModifyPassword.length() < 1 -> {showToast("請輸入確認密碼"); return@setPositiveButton}
                        modPassWord.text.toString() != checkModifyPassword.text.toString() ->{
                            showToast("兩次密碼不一致"); return@setPositiveButton
                        }
                        else -> {
                            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                            val param =
                                "{\"username\" : \"${modUserName.text}\" , \"password\" : \"${modPassWord.text}\"}"
                            val body = param.toRequestBody(type)

                            val url = "http://35.77.46.57:3000/user/update"
                            val request = Request.Builder()
                                .url(url)
                                .addHeader("token",c.getString(1))
                                .put(body)
                                .build()

                            OkHttpClient.Builder().build().newCall(request).enqueue(object : Callback{
                                override fun onResponse(call: Call, response: Response) {
                                    val res = response.body?.string()
                                    val json = JSONObject(res.toString())

                                    when (json.getInt("status")) {
                                        200 -> successmodify() //修改成功
                                    }
                                    Log.e("server","$json")
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    TODO("Not yet implemented")
                                }
                            })
                        }
                    }

                }.create().show()
        }
    }

    fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}

data class Contact(
    val name: String,
    val id: String
)