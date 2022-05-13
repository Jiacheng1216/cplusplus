package com.example.dofbot

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent.ACTION_DOWN
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.View
import android.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.platform.android.AndroidLogHandler.publish
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject

lateinit var angleView:TextView


class ControlArm : AppCompatActivity() {
    val mqtt = MQTT()

    var angle1 = 90
    var angle1max = 180
    var angle2 = 90
    var angle2max = 180
    var angle3 = 90
    var angle3max = 180
    var angle4 = 90
    var angle4max = 180
    var angle5 = 90
    var angle5max = 180
    var angle6 = 90
    var angle6max = 180
    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_arm)

        val No1 = findViewById<RadioButton>(R.id.No1)
        val No2 = findViewById<RadioButton>(R.id.No2)
        val No3 = findViewById<RadioButton>(R.id.No3)
        val No4 = findViewById<RadioButton>(R.id.No4)
        val No5 = findViewById<RadioButton>(R.id.No5)
        val No6 = findViewById<RadioButton>(R.id.No6)


        angleView = findViewById(R.id.angleView)
        var id = 0
        val angleSeekbar = angleSeekbar(angleView , id)

        val rightButton = findViewById<Button>(R.id.rightButton)
        val leftButton = findViewById<Button>(R.id.leftButton)

        val id1 = "{'id1' : 1}"
        val id1Json = JSONObject(id1)
        val id2 = "{'id2' : 2}"
        val id2Json = JSONObject(id2)
        val id3 = "{'id3' : 3}"
        val id3Json = JSONObject(id3)
        val id4 = "{'id4' : 4}"
        val id4Json = JSONObject(id4)
        val id5 = "{'id5' : 5}"
        val id5Json = JSONObject(id5)
        val id6 = "{'id6' : 6}"
        val id6Json = JSONObject(id6)

        No1.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id1Json")
            rightButton.text = "右"
            leftButton.text = "左"
            angleSeekbar?.setProgress(angle1)
            angleSeekbar?.max = angle1max
            id = 1
        }
        No2.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id2Json")
            rightButton.text = "上"
            leftButton.text = "下"
            angleSeekbar?.setProgress(angle2)
            angleSeekbar?.max = angle2max
            id = 2
        }
        No3.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id3Json")
            rightButton.text = "上"
            leftButton.text = "下"
            angleSeekbar?.setProgress(angle3)
            angleSeekbar?.max = angle3max
            id = 3
        }
        No4.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id4Json")
            rightButton.text = "上"
            leftButton.text = "下"
            angleSeekbar?.setProgress(angle4)
            angleSeekbar?.max = angle4max
            id = 4
        }
        No5.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id5Json")
            rightButton.text = "右"
            leftButton.text = "左"
            angleSeekbar?.setProgress(angle5)
            angleSeekbar?.max = angle5max
            id = 5
        }
        No6.setOnClickListener{
            mqtt.publish("NTUT/MQTT","$id6Json")
            rightButton.text = "抓"
            leftButton.text = "鬆"
            angleSeekbar?.setProgress(angle6)
            angleSeekbar?.max = angle6max
            id = 6
        }

        val midden = findViewById<Button>(R.id.midden) //置中
        midden.setOnClickListener {
            mqtt.publish("NTUT/MQTT","{\"mid\":0}")
            angleSeekbar!!.setProgress(90)
        }

        leftButton.setOnClickListener {  //角度-1
            angleSeekbar!!.progress-=1
        }

        rightButton.setOnClickListener { //角度+1
            angleSeekbar!!.progress+=1
        }
    }

    private fun angleSeekbar(angleView: TextView, id:Int): SeekBar? {
        val angleSeekbar = findViewById<SeekBar>(R.id.angleSeekBar)
        angleSeekbar.setProgress(90)

        angleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                angleView.setText("當前角度:" + progress+"度")
                val angle = "{'angle' : $progress }"
                val angleJson = JSONObject(angle)
                mqtt.publish("NTUT/MQTT","$angleJson")

                return when(id){
                    1 -> angle1 = progress
                    2 -> angle2 = progress
                    3 -> angle3 = progress
                    4 -> angle4 = progress
                    5 -> angle5 = progress
                    else -> angle6 = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        return angleSeekbar
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK){
            val image = data?.extras?.get("data") ?: return
            imageView.setImageBitmap(image as Bitmap)
        }
    }

}

