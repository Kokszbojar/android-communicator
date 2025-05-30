package com.example.frontend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

import java.nio.charset.StandardCharsets
import java.util.*
import org.json.JSONObject

class MqttService : Service() {
    private lateinit var mqttNotificationManager: MqttNotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        mqttNotificationManager = MqttNotificationManager(applicationContext)
        mqttNotificationManager.connectAndSubscribe(userId)

        startForeground(NOTIF_ID, createNotification("Powiadomienia są włączone"))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttNotificationManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(content: String): Notification {
        val channelId = "mqtt_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MQTT Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Komunikator")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
    }
}

class MqttNotificationManager(private val context: Context) {

    private val TAG = "MQTT"
    private val mqttClient: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier("android-client-${UUID.randomUUID()}")
        .serverHost("192.168.0.130")
        .serverPort(1883)
        .buildAsync()

    fun connectAndSubscribe(userId: Int) {
        mqttClient.connectWith()
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Połączenie MQTT nieudane: ${throwable.message}")
                } else {
                    Log.d(TAG, "Połączono z MQTT")
                    subscribeToUserTopic(userId)
                }
            }
    }

    private fun subscribeToUserTopic(userId: Int) {
        val topic = "user/$userId"

        mqttClient.subscribeWith()
            .topicFilter(topic)
            .callback { publish: Mqtt3Publish ->
                val payload = publish.payload.orElse(null)
                val message = if (payload != null) {
                    val bytes = ByteArray(payload.remaining())
                    payload.get(bytes)  // Wypełniamy tablicę danymi z bufora
                    String(bytes, Charsets.UTF_8) // Zamieniamy na String (bo JSON)
                } else {
                    "(brak treści)"
                }
                Log.d(TAG, "Odebrano powiadomienie: $message")
                val json = JSONObject(message)
                val type = json.optString("type", null)
                if (type == "chat_message") {
                    val title = json.optString("title", "Wiadomość")
                    val body = json.optString("body", "")
                    val chatId = json.optInt("chat_id", -1)
                    showMessageNotification(title, body, chatId)
                } else if (type == "incoming_call") {
                    val title = json.optString("title", "Połączenie")
                    val body = json.optString("body", "")
                    val roomId = json.optInt("room_id", -1)
                    showCallNotification(title, body, roomId)
                }
            }
            .send()
    }

    private fun showMessageNotification(title: String, message: String, chatId: Int) {
        val channelId = "chat_channel"
        val notificationId = System.currentTimeMillis().toInt()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wiadomości czatu",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o nowych wiadomościach MQTT"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_chat", true)
            putExtra("userId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.android_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun showCallNotification(title: String, message: String, roomId: Int) {
        val channelId = "call_channel"
        val notificationId = System.currentTimeMillis().toInt()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Połączenia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o nowych połączeniach MQTT"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_call", true)
            putExtra("roomId", roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.android_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun disconnect() {
        mqttClient.disconnect()
    }
}