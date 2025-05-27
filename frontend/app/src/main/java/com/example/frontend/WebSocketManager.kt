package com.example.frontend

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var client = OkHttpClient()
    private var token: String? = null

    private val listeners = mutableSetOf<(JSONObject) -> Unit>()

    fun initialize(token: String) {
        this.token = token
        if (webSocket != null) return // zapobiegaj ponownemu łączeniu

        val request = Request.Builder()
            .url("ws://192.168.0.130:8000/ws/chat/?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                notifyListeners(json)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val error = JSONObject().apply {
                    put("type", "error")
                    put("message", "Błąd połączenia: ${t.localizedMessage}")
                }
                notifyListeners(error)
            }
        })
    }

    fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    fun close() {
        webSocket?.close(1000, "Zamknięcie przez użytkownika")
        webSocket = null
    }

    fun addListener(listener: (JSONObject) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (JSONObject) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(message: JSONObject) {
        listeners.forEach { it(message) }
    }
}