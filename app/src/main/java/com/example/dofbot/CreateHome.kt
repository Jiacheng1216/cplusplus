package com.example.dofbot

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CreateHome : AppCompatActivity() {
    private lateinit var dbrw: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_createhome)

        val newHome = findViewById<Button>(R.id.newhome)
        val edHomeName = findViewById<EditText>(R.id.ed_homename)
        dbrw = Database(this,"TOKEN").writableDatabase

        //從資料庫以userEmail搜尋token值
        val c = dbrw.rawQuery(
            "SELECT * FROM tokenList WHERE email LIKE '$userEmail'",
            null)
        c.moveToFirst()


        newHome.setOnClickListener {
            when{
                edHomeName.length() < 1 -> showToast("請輸入家庭名稱")
                else -> {
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{\"name\" : \"${edHomeName.text}\"}"
                    val body = param.toRequestBody(type)
                    var id = ""

                    val url = "http://35.77.46.57:3000/home/create"
                    val req = Request
                        .Builder()
                        .url(url)
                        .addHeader("token",c.getString(1))
                        .post(body)
                        .build()

                    OkHttpClient().newCall(req).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string() //server回應
                            val json = JSONObject(res.toString()) //server回應轉成JSON格式
                            //取得json中的id物件
                            val idJson = json.getJSONObject("data")
                            //id物件取得id資料
                            val getId = idJson.getString("id")
                            id = getId

                            when(json.getInt("status")){
                                201 -> {
                                    showToast(json.getString("message")) //Create success
                                    //把home資料提交給adapter並建立listview
                                    val b = Bundle()
                                    b.putString("name", edHomeName.text.toString())
                                    b.putString("id", id)
                                    setResult(Activity.RESULT_OK, Intent().putExtras(b))
                                    finish()
                                }
                            }
                            Log.e("123","$json")
                            Log.e("token",c.getString(1))
                            Log.e("id",id)
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}