package com.example.frontend

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class ChatViewModel(private val token: String?) : ViewModel() {

    var messages = mutableStateListOf<String>()
        private set

    var currentMessage = mutableStateOf("")
        private set

    var selectedUserId = mutableStateOf(1)
    var currentOffset = mutableStateOf(0)
    var canLoadMore = mutableStateOf(true)

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    init {
        connectWebSocket()
    }

    fun onMessageChange(newMessage: String) {
        currentMessage.value = newMessage
    }

    fun selectUser(userId: Int) {
        selectedUserId.value = userId
        currentOffset.value = 0
        canLoadMore.value = true
        messages.clear()
        loadMessagesForUser(userId)
    }

    fun sendMessage() {
        val message = currentMessage.value
        if (message.isNotBlank()) {
            val json = JSONObject()
            json.put("action", "send_message")
            json.put("to", selectedUserId.value)
            json.put("message", message)

            webSocket.send(json.toString())
            messages.add("Ty: $message")
            currentMessage.value = ""
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.0.130:8000/ws/chat/?token=$token") // Emulator -> host
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                val fromId = json.getJSONObject("message").getInt("from")
                val content = json.getJSONObject("message").getString("content")

                // Na razie nazwę użytkownika zastępujemy ID
                val formatted = "Użytkownik $fromId\n$content"
                messages.add(formatted)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                messages.add("Błąd połączenia: ${t.localizedMessage}")
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.close(1000, "Koniec")
    }

    fun loadMessagesForUser(userId: Int, offset: Int = 0, limit: Int = 50, appendToTop: Boolean = false) {
        val token = this.token ?: return
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://192.168.0.130:8000/api/chat/history/?user_id=$userId&offset=$offset&limit=$limit")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val jsonArray = JSONArray(body)
                    val newMessages = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val senderName = obj.getString("sender_name")
                        val content = obj.getString("content")
                        newMessages.add("$senderName\n$content")
                    }

                    Handler(Looper.getMainLooper()).post {
                        if (newMessages.size < limit) {
                            canLoadMore.value = false
                        }

                        if (appendToTop) {
                            messages.addAll(0, newMessages.reversed())
                        } else {
                            messages.clear()
                            messages.addAll(newMessages.reversed())
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatHistory", "Failed to load messages", e)
            }
        })
    }
}
