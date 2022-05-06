package com.example.dofbot

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class UserActivity : AppCompatActivity() {
    private lateinit var adapter: Adapter
    private val contacts = ArrayList<Contact>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.extras?.let {
            if (requestCode == 1 && resultCode == Activity.RESULT_OK){
                val homename = it.getString("name") ?: return@let
                val homeid = it.getString("id") ?: return@let
                contacts.add(Contact(homename,homeid))
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val createHome = findViewById<Button>(R.id.createHome)
        val joinHome = findViewById<Button>(R.id.joinHome)
        val joinHomeId = layoutInflater.inflate(R.layout.user_joinhome,null)
        val setting = findViewById<ImageView>(R.id.setting)
        val modifypassword = (Intent(this,ModifyPassword::class.java))

        val homeview = findViewById<RecyclerView>(R.id.homeView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        homeview.layoutManager = linearLayoutManager

        adapter = Adapter(contacts)
        homeview.adapter = adapter

        createHome.setOnClickListener {
            startActivityForResult(Intent(this,CreateHome::class.java),1)
        }

        joinHome.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("請輸入ID")
                .setView(joinHomeId)
                .setPositiveButton("確認"){ dialog,which->
                }.show()
        }

        setting.setOnClickListener {
            startActivity(modifypassword)
        }
    }
}

data class Contact(
    val name: String,
    val id: String
)