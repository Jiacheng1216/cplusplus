package com.example.dofbot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import java.lang.Exception

class DataBaseTest : AppCompatActivity() {
    private var items: ArrayList<String> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var dbrw: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_base_test)

        dbrw = Database(this,"TOKEN").writableDatabase
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        findViewById<ListView>(R.id.listView).adapter = adapter
        setListener()
    }

    private fun setListener(){
        val ed_book = findViewById<EditText>(R.id.ed_book)

        findViewById<Button>(R.id.btn_insert).setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }

        findViewById<Button>(R.id.btn_delete).setOnClickListener {
            try {
                dbrw.execSQL("DELETE FROM tokenList WHERE email LIKE '${ed_book.text}'")
                showToast("刪除:${ed_book.text}")
            }catch (e:Exception){
                showToast("刪除失敗:$e")
            }
        }

        findViewById<Button>(R.id.btn_query).setOnClickListener {
            val queryString = if (ed_book.length()<1)
                "SELECT*FROM tokenList"
            else
                "SELECT * FROM tokenList WHERE email LIKE '${ed_book.text}'"

            val c = dbrw.rawQuery(queryString,null)
            c.moveToFirst()
            items.clear()
            showToast("共有${c.count}筆資料")
            for (i in 0 until c.count){
                items.add("email:${c.getString(0)}" +
                        "\t\t\t\ttoken:${c.getString(1)}")// +
                        //"\t\t\t\tname:${c.getString(2)}" +
                        //"\t\t\t\tid:${c.getString(3)}}")
                c.moveToNext()
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showToast(text:String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

}

