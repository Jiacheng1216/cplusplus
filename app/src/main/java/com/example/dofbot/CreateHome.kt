package com.example.dofbot

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class CreateHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_createhome)

        val backtouser = findViewById<Button>(R.id.backToUser)
        val newhome = findViewById<Button>(R.id.newhome)
        val ed_homename = findViewById<EditText>(R.id.ed_homename)
        val ed_homeid = findViewById<EditText>(R.id.ed_homeid)
        val createstatus = findViewById<TextView>(R.id.createstatus)

        backtouser.setOnClickListener {
            startActivity(Intent(this,UserActivity::class.java))
        }

        newhome.setOnClickListener {
            when{
                ed_homename.length() < 1 -> createstatus.setText("請輸入家庭名稱")
                ed_homeid.length() < 1 -> createstatus.setText("請輸入家庭ID")
                else -> {
                    val b = Bundle()
                    b.putString("name", ed_homename.text.toString())
                    b.putString("id", ed_homeid.text.toString())
                    setResult(Activity.RESULT_OK, Intent().putExtras(b))
                    finish()
                }
            }
        }
    }
}