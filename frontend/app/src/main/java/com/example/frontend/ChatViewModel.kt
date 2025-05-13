package com.example.frontend

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import okhttp3.*

class ChatViewModel : ViewModel() {

    var messages = mutableStateListOf<String>()
        private set

    var currentMessage = mutableStateOf("")
        private set

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    init {
        connectWebSocket()
    }

    fun onMessageChange(newMessage: String) {
        currentMessage.value = newMessage
    }

    fun sendMessage() {
        val message = currentMessage.value
        if (message.isNotBlank()) {
            webSocket.send(message)
            messages.add("Ty: $message")
            currentMessage.value = ""
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.0.130:8000/ws/chat/") // Emulator -> host
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                messages.add("Server: $text")
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
}
