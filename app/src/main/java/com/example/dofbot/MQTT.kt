package com.example.dofbot

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.dofbot.MQTT.Companion.TAG
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

private lateinit var mqttClient: MqttAndroidClient

class MQTT {
    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    fun connect(context: Context) :Boolean{
        val serverURI = "tcp://140.124.73.217:1883"
        mqttClient = MqttAndroidClient(context, serverURI, "NTUT")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.e("123", "Receive message: ${message.toString()} from topic: $topic")

            }

            override fun connectionLost(cause: Throwable?) {
                Log.d("123", "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("123", "Connection success")
                    Toast.makeText(context,"連線成功，可以點擊手臂圖示控制手臂",Toast.LENGTH_SHORT).show()
                    connectSuccess = true
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("123", "Connection failure")
                    connectSuccess = false
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        };return connectSuccess
    }

    fun disconnect() :Boolean{
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("123", "Disconnected")
                    connectSuccess = false
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("123", "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {

        };return connectSuccess
    }
    //MQTT訂閱
    fun subscribe(topic: String, subscribeView: TextView, qos: Int = 1,) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("123", "Subscribed to $topic")
                    subscribeView.text = "訂閱主題: $topic"
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("123", "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            // Toast.makeText(this,"還沒訂閱",Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    //MQTT發佈
    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("123", "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("123", "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
