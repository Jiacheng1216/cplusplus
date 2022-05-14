package com.example.dofbot

import android.app.AlertDialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.zip.Adler32

val item = arrayOf("進入","刪除該家庭")

class Information {
    var message = ""
    var status = 0
    lateinit var data: Object

    class Object {
        var key = arrayOf("","")
        var updatedAt = ""
    }
}
val information = Information()


class Adapter(private val data: ArrayList<Contact>):
    RecyclerView.Adapter<Adapter.ViewHolder>(){
        class ViewHolder(v: View): RecyclerView.ViewHolder(v){
            val home_name = v.findViewById<TextView>(R.id.home_name)
            val home_id = v.findViewById<TextView>(R.id.home_id)
            val quitHome = v.findViewById<ImageView>(R.id.quitHome)
            val deleteHome = v.findViewById<ImageView>(R.id.deleteHome)
            val click = v.findViewById<ImageView>(R.id.click)
        }

    //建立資料庫實體
    private lateinit var dbrw: SQLiteDatabase

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int):
        ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.homeview,viewGroup,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.home_name.text = data[position].name
        holder.home_id.text = data[position].id

        //delete按鈕當context
        val deleteContext = holder.deleteHome.context
        //quit按鈕當context
        val quitContext = holder.quitHome.context
        //背景當context
        val clickContext = holder.click.context

        dbrw = Database(deleteContext,"TOKEN").writableDatabase
        //從資料庫以userEmail搜尋token值
        val c = dbrw.rawQuery(
            "SELECT * FROM tokenList WHERE email LIKE '$userEmail'",
            null)
        //從第一筆開始搜尋
        c.moveToFirst()

        //按下delete按鈕
        holder.deleteHome.setOnClickListener {
            AlertDialog.Builder(deleteContext)
                .setTitle("確定要刪除家庭?")
                .setMessage("一但刪除後將永久不可復原")
                .setNegativeButton("確定"){ dialog, which ->

                    //body building
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{}"
                    val body = param.toRequestBody(type)

                    val url = "http://35.77.46.57:3000/home/delete/${data[position].id}"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("token",c.getString(1))
                        .delete(body)
                        .build()

                    OkHttpClient.Builder().build().newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val res = response.body?.string()
                            val json = JSONObject(res.toString())

                            when(json.getInt("status")){
                                //刪除成功
                                201 -> Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(deleteContext, "刪除成功", Toast.LENGTH_SHORT).show()
                                    data.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                403 -> Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(deleteContext, "刪除失敗，你沒有權限", Toast.LENGTH_SHORT).show()
                                }

                            }

                            Log.e("server","$json")
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            TODO("Not yet implemented")
                        }
                    })

                }

                .setPositiveButton("取消") { _,_->
                }.show()

        }

        //按下quit按鈕
        holder.quitHome.setOnClickListener {
            AlertDialog.Builder(quitContext)
                .setTitle("確定要退出家庭?")
                .setMessage("可重新輸入ID再次加入")
                .setNegativeButton("確定") { dialog, which ->

                    //body building
                    val type = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val param = "{}"
                    val body = param.toRequestBody(type)

                    //資料庫搜尋token
                    val c = dbrw.rawQuery(
                        "SELECT * FROM tokenList WHERE email LIKE '$userEmail'",
                        null)
                    c.moveToFirst()

                    //向server發出request
                    val url = "http://35.77.46.57:3000/home/quit/${data[position].id}"
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

                            when(json.getInt("status")){
                                //退出家庭
                                201 -> Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(deleteContext, "退出成功", Toast.LENGTH_SHORT).show()
                                }
                            }

                            Log.e("server", "$json")
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            TODO("Not yet implemented")
                        }
                    })
                    data.removeAt(position)
                    notifyDataSetChanged()
                }
                .setPositiveButton("取消") {_, _ ->

                }.show()
        }

        //顯示資訊函數
        fun showDialog(objectt :JSONArray, updatedAt :String) {
            if(objectt.isNull(0)){
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(deleteContext, "沒有資訊", Toast.LENGTH_SHORT).show()
                }
            }else {
                //取得偵測到的物件數量
                val number = objectt.length() - 1
                //建立一個可空陣列，存放物件資料
                val array = arrayOfNulls<String>(objectt.length())
                //將可空陣列加入物件資料
                for (i in 0..number){
                    array.set(i,"${objectt[i]}")
                }
                //每個元素前面加上物件兩字
                array.forEachIndexed { index, data ->
                    array[index] = "物件：${data}"
                }

                AlertDialog.Builder(clickContext)
                    .setTitle("監視器捕捉到的物件\n最後更新時間:$updatedAt")
                    .setItems(array,null)
                    .setPositiveButton("確認") { _, _ ->
                        return@setPositiveButton
                    }.show()
            }
        }

        //點選home顯示資訊
        holder.click.setOnClickListener {
            val url = "http://35.77.46.57:3000/home/data/${data[position].id}"
            val req = Request.Builder()
                    .url(url)
                    //送出token
                    .addHeader("token",c.getString(1))
                    .build()

            OkHttpClient().newCall(req).enqueue(object : Callback{
                override fun onResponse(call: Call, response: Response) {
                    //取得server回傳的Json資料
                    val res = response.body?.string()
                    val json = JSONObject(res.toString())
                    val data = json.getJSONObject("data")
                    val objectt = data.getJSONArray("object")
                    val updatedAt = data.getString("updatedAt")

                    when(json.getInt("status")){
                        200 -> {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    clickContext,
                                    json.getString("message"),
                                    Toast.LENGTH_SHORT
                                ).show()
                                //顯示資訊
                                showDialog(objectt,updatedAt)
                            }
                        }
                    }

                    Log.e("json","$json")
                    Log.e("data","$data")
                    Log.e("object","$objectt")
                }

                override fun onFailure(call: Call, e: IOException) {
                    Toast.makeText(deleteContext, "查詢失敗", Toast.LENGTH_SHORT).show()
                }
            })

        }
    }
    }



//按下按鈕出現選單功能
/* Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(holderContext)
                    .setItems(item) {dialogInterface, i ->
                        when(item[i]){
                            "進入" -> return@setItems
                            "刪除該家庭" -> {
                                data.removeAt(position)
                                notifyDataSetChanged()
                            }
                        }
                    }.show()
            } */