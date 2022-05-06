package com.example.dofbot

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

var connectSuccess = false
private lateinit var mqttClient: MqttAndroidClient
val topic = "NTUT/MQTT"
class MainActivity : AppCompatActivity() {
    private lateinit var connection:Button
    private lateinit var connectStatus :TextView
    private lateinit var startArm: Button
    private lateinit var signUp:Button

    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val LoginAccount = findViewById<EditText>(R.id.LoginAccount)
        val LoginPassword = findViewById<EditText>(R.id.LoginPassword)
        val connectStatus = findViewById<TextView>(R.id.connectStatus)
        val loginIntent = Intent(this,UserActivity::class.java)

        val signUp = findViewById<Button>(R.id.signUp)
        signUp.setOnClickListener {
            startActivity(Intent(this,RegisterActivity::class.java))
        }
        val signIn = findViewById<Button>(R.id.signIn)
        signIn.setOnClickListener {
            val type = "application/json; charset=utf-8".toMediaTypeOrNull()
            val param = "{\"email\" : \"${LoginAccount.text}\" , \"password\" : \"${LoginPassword.text}\"}"
            val body = param.toRequestBody(type)

            val Url = "http://35.77.46.57:3000/user/login"
            val req = Request.Builder().url(Url).post(body).build()

            OkHttpClient().newCall(req).enqueue(object :Callback{
                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string()
                    val json = JSONObject(res.toString())

                    val headData = response.headers //取得server的token值
                    val token = headData.get("token").toString() //儲存token值成變數
                    val settings = getSharedPreferences("token",0)  //儲存token到SharedPreferences
                    settings.edit().putString("TOKEN", token).apply()
                    val readtoken = getSharedPreferences("token",0)

                    when(json.getInt("status")){
                        200 -> startActivity(loginIntent)
                    }

                    Log.e("token","${readtoken.getString("TOKEN", "")}")
                    Log.e("321","$json")
                }

                override fun onFailure(call: Call, e: IOException) {

                }
            })
        }

        val connection = findViewById<Button>(R.id.connection)  //連線
        connection.setOnClickListener {
            connect(this , connectStatus )
            connectStatus.text = "連線中..."
        }

        val startArm = findViewById<Button>(R.id.startArm)
        startArm.setOnClickListener {
            if (connectSuccess == true){
                startActivity(Intent(this, ControlArm::class.java))
            }else{
                connectStatus.text = "還沒連線"
            }
        }

        val disconnect = findViewById<Button>(R.id.disconnect)  //中斷連線
        disconnect.setOnClickListener {
            disconnect(connectStatus)
        }



    }

    /* fun getCode() {  //取得二維碼
         val SSIDView = findViewById<EditText>(R.id.SSIDView) //輸入WIFI SSID
         val KEYView = findViewById<EditText>(R.id.KEYView)   //輸入密碼
         val imageView = findViewById<ImageView>(R.id.imageView) //二維碼容器
         val WPA_WPA2_WPA3 = findViewById<RadioButton>(R.id.WPA_WPA2_WPA3)
         val WEP = findViewById<RadioButton>(R.id.WEP)

         val Encryption = when {
             WPA_WPA2_WPA3.isChecked -> "WPA"
             WEP.isChecked -> "WEP"
             else -> ""
         }

         val BarcodeEncoder = BarcodeEncoder()  //轉換的方法
         val content = "WIFI:S:${SSIDView.text};T:$Encryption;P:${KEYView.text};" //二維碼內容
         val bitmap = BarcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 512, 512)

         imageView.setImageBitmap(bitmap)
     }*/
    private fun showToast(msg: String) =
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}



fun connect(context: Context ,
            connectStatus: TextView , ) :Boolean{
    val serverURI = "tcp://140.124.73.217:1883"
    mqttClient = MqttAndroidClient(context, serverURI, "NTUT")
    mqttClient.setCallback(object : MqttCallback {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.e(MainActivity.TAG, "Receive message: ${message.toString()} from topic: $topic")

        }

        override fun connectionLost(cause: Throwable?) {
            Log.d(MainActivity.TAG, "Connection lost ${cause.toString()}")
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {

        }
    })
    val options = MqttConnectOptions()
    try {
        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(MainActivity.TAG, "Connection success")
                connectStatus.text = "連線成功"
                connectSuccess = true
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(MainActivity.TAG, "Connection failure")
                connectStatus.text = "連線失敗"
                connectSuccess = false
            }
        })
    } catch (e: MqttException) {
        e.printStackTrace()
    };return connectSuccess
}

fun disconnect(connectStatus: TextView) :Boolean{
    try {
        mqttClient.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                connectStatus.text = "中斷連線"
                Log.d(MainActivity.TAG, "Disconnected")
                connectSuccess = false
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(MainActivity.TAG, "Failed to disconnect")
            }
        })
    } catch (e: MqttException) {
        e.printStackTrace()
    };return connectSuccess
}

fun subscribe(topic: String, subscribeView:TextView, qos: Int = 1,) {
    try {
        mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(MainActivity.TAG, "Subscribed to $topic")
                subscribeView.text = "訂閱主題: $topic"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(MainActivity.TAG, "Failed to subscribe $topic")
            }
        })
    } catch (e: MqttException) {
       // Toast.makeText(this,"還沒訂閱",Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}

fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
    try {
        val message = MqttMessage()
        message.payload = msg.toByteArray()
        message.qos = qos
        message.isRetained = retained
        mqttClient.publish(topic, message, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(MainActivity.TAG, "$msg published to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(MainActivity.TAG, "Failed to publish $msg to $topic")
            }
        })
    } catch (e: MqttException) {
        e.printStackTrace()
    }


}

