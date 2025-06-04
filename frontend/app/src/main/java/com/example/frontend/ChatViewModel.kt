package com.example.frontend

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException

class ChatViewModel(var userId: Int, private val token: String?) : ViewModel() {

    var friendName = mutableStateOf("Unknown")

    var messages = mutableStateListOf<Message>()
        private set

    var currentMessage = mutableStateOf("")
        private set

    var currentOffset = mutableIntStateOf(0)
    var canLoadMore = mutableStateOf(true)

    private var onDisposeListener: ((JSONObject) -> Unit)? = null

    init {
        loadMessagesForUser(userId)

        // Reaguj na nowe wiadomości z WebSocketa
        val listener: (JSONObject) -> Unit = { json ->
            if (json.getString("type") == "chat_message") {
                val fromUser = json.getString("sender_name")
                if (friendName.value == fromUser) {
                    messages.add(
                        Message(
                            text = json.getString("content"),
                            fromUser = fromUser,
                            timestamp = json.getString("timestamp"),
                            fileUrl = json.getString("file_url"),
                            fileType = json.getString("file_type")
                        )
                    )
                }
            }
        }

        WebSocketManager.addListener(listener)
        // Zapamiętaj listener żeby usunąć
        onDisposeListener = listener
    }

    fun onMessageChange(newMessage: String) {
        currentMessage.value = newMessage
    }

    fun sendMessage() {
        val message = currentMessage.value
        if (message.isNotBlank()) {
            val json = JSONObject()
            json.put("action", "send_message")
            json.put("to", userId)
            json.put("message", message)

            WebSocketManager.send(json)
            messages.add(Message(text=message, fromUser="", timestamp=""))
            currentMessage.value = ""
        }
    }

    fun sendFileMessage(file: File, fileType: String, mimeType: String) {
        val base64 = encodeFileToBase64(file, mimeType)
        val json = JSONObject().apply {
            put("action", "send_message")
            put("to", userId)
            put("message", "")
            put("file", base64)
            put("file_type", fileType)
        }
        val uri = file.toUri()
        WebSocketManager.send(json)
        messages.add(
            Message(
                text = "",
                fromUser = "",
                timestamp = "",
                fileUrl = "",
                fileType = fileType,
                localFileUri = uri
            )
        )
    }

    private fun encodeFileToBase64(file: File, mimeType: String): String {
        val bytes = file.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:$mimeType;base64,$base64"
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
            .url("http://${BuildConfig.SERVER_HOST}:8000/api/chat/history/?user_id=$userId&offset=$offset&limit=$limit")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val jsonArray = JSONObject(body).getJSONArray("data")
                    friendName.value = JSONObject(body).getString("friendName")
                    val newMessages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val content = obj.getString("content")
                        val fromUser = obj.getString("sender_name")
                        val timestamp = obj.getString("timestamp")
                        val fileUrl = obj.getString("file_url")
                        val fileType = obj.getString("file_type")
                        newMessages.add(Message(content, fromUser, timestamp, fileUrl, fileType))
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
    val callStatus = MutableStateFlow("pending")

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
                            callStatus.value = "connected"
                        }
                    }
                    is RoomEvent.TrackUnsubscribed -> {
                        callStatus.value = "disconnected"
                    }

                    else -> {}
                }
            }
        }
    }

    fun disconnect() {
        liveKitRoom.disconnect()
        liveKitRoom.release()
    }

    override fun onCleared() {
        super.onCleared()
        liveKitRoom.disconnect()
        liveKitRoom.release()
    }
}
