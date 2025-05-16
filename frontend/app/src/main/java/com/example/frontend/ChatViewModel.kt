package com.example.frontend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import livekit.org.webrtc.EglBase
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class ChatViewModel(private val token: String?) : ViewModel() {

    var messages = mutableStateListOf<Message>()
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
            messages.add(Message(text=message, isOwn=true))
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
                when (json.getString("type")) {
                    "chat_message" -> {
                        val fromId = json.getInt("from")
                        val content = json.getString("content")
                        messages.add(Message(content, false))
                    }
//                    "incoming_call" -> {
//                        val callerId = json.getInt("caller")
//                        val callId = json.getInt("call_id").toString()
//                        receiveIncomingCall(CallData(callerId, callId))
//                    }
//                    "call_answer" -> {
//                        val accepted = json.getBoolean("accepted")
//                        if (accepted) _callState.value = CallState.InCall else _callState.value = CallState.Idle
//                    }
                }

            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                messages.add(Message("Błąd połączenia: ${t.localizedMessage}", true))
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.close(1000, "Koniec")
    }

    fun loadMessagesForUser(
        userId: Int,
        offset: Int = 0,
        limit: Int = 50,
        appendToTop: Boolean = false
    ) {
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
                    val newMessages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val senderName = obj.getString("sender_name")
                        val content = obj.getString("content")
                        newMessages.add(Message(content, false))
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

class CallViewModel(
    private val context: Context
) : ViewModel() {

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val _videoTrack = MutableStateFlow<VideoTrack?>(null)
    val videoTrack: StateFlow<VideoTrack?> = _videoTrack

    private val liveKitRoom: Room = LiveKit.create(context)

    init {
        observeRoomEvents()
    }

    private var isRendererInitialized = false

    fun initVideoRenderer(renderer: SurfaceViewRenderer) {
        if (!isRendererInitialized) {
            liveKitRoom.initVideoRenderer(renderer)
            isRendererInitialized = true
        }
    }

    fun connectToRoom(url: String, token: String) {
        viewModelScope.launch {
            liveKitRoom.connect(url, token)
            val localParticipant = liveKitRoom.localParticipant
            localParticipant.setCameraEnabled(true)
            localParticipant.setMicrophoneEnabled(true)

            val remoteTrack = liveKitRoom.remoteParticipants.values.firstOrNull()
                ?.getTrackPublication(Track.Source.CAMERA)
                ?.track as? VideoTrack

            remoteTrack?.let {
                _videoTrack.value = it
            }

            _room.value = liveKitRoom
        }
    }

    private fun observeRoomEvents() {
        viewModelScope.launch {
            liveKitRoom.events.collect { event ->
                when (event) {
                    is RoomEvent.TrackSubscribed -> {
                        val track = event.track
                        if (track is VideoTrack) {
                            _videoTrack.value = track
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveKitRoom.disconnect()
        liveKitRoom.release()
    }
}

//    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
//    val callState: StateFlow<CallState> = _callState.asStateFlow()
//
//    private val _incomingCall = MutableStateFlow<CallData?>(null)
//    val incomingCall: StateFlow<CallData?> = _incomingCall.asStateFlow()
//
//    fun initiateCall(calleeId: Int) {
//        val json = JSONObject()
//        json.put("action", "call_user")
//        json.put("to", calleeId)
//
//        webSocket.send(json.toString())
//        _callState.value = CallState.Calling
//    }
//
//    fun receiveIncomingCall(callData: CallData) {
//        _incomingCall.value = callData
//        _callState.value = CallState.Ringing
//    }
//
//    fun acceptCall() {
//        val call = incomingCall.value ?: return
//        val json = JSONObject()
//        json.put("action", "answer_call")
//        json.put("call_id", call.callId)
//        json.put("accepted", true)
//
//        webSocket.send(json.toString())
//        _callState.value = CallState.InCall
//    }
//
//    fun declineCall() {
//        val call = incomingCall.value ?: return
//        val json = JSONObject()
//        json.put("action", "answer_call")
//        json.put("call_id", call.callId)
//        json.put("accepted", false)
//
//        webSocket.send(json.toString())
//        _callState.value = CallState.Idle
//        _incomingCall.value = null
//    }
//
//    fun endCall() {
//        // Zakończ połączenie
//        // ...
//        _callState.value = CallState.Idle
//    }
//
//}
//
//sealed class CallState {
//    object Idle : CallState()
//    object Calling : CallState()
//    object Ringing : CallState()
//    object InCall : CallState()
//}
//
//data class CallData(val callerId: Int, val callId: String)
