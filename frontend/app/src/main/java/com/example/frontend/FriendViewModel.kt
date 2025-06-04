package com.example.frontend

import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import android.util.Log
import androidx.compose.runtime.State
import org.json.JSONObject

class FriendViewModel(private val tokenManager: TokenManager) : ViewModel() {
    private val api = RetrofitClient.getInstance(tokenManager)
    val friends = mutableStateListOf<FriendDto>()
    val searchResults = mutableStateListOf<UserSearchResultDto>()
    val receivedRequests = mutableStateListOf<FriendRequestDto>()
    val sentRequests = mutableStateListOf<FriendRequestDto>()
    val loading = mutableStateOf(false)

    init {
        fetchFriends()
        fetchRequests()
        listenForMessages()
    }

    fun listenForMessages() {
        WebSocketManager.addListener { json ->
            if (json.getString("type") == "chat_message") {
                val fromUser = json.getString("sender_name")
                val message = json.optString("content")
                val timestamp = json.optString("timestamp")
                updateFriendWithMessage(fromUser, message, timestamp)
            } else if (json.getString("type") == "friend_request") {
                fetchFriends()
                fetchRequests()
                searchResults.clear()
            } else if (json.getString("type") == "friend_delete") {
                fetchFriends()
            }
        }
    }

    private fun updateFriendWithMessage(senderName: String, message: String, timestamp: String) {
        val index = friends.indexOfFirst { it.username == senderName }
        if (index != -1) {
            val updatedFriend = friends[index].copy(
                lastMessage = message,
                timestamp = timestamp,
                hasNewMessage = true
            )
            friends.removeAt(index)
            friends.add(0, updatedFriend) // przesuwa na górę
        }
    }

    private fun fetchFriends() {
        loading.value = true
        friends.clear()
        api.getFriends().enqueue(object : Callback<List<FriendDto>> {
            override fun onResponse(call: Call<List<FriendDto>>, response: Response<List<FriendDto>>) {
                response.body()?.let { friends.addAll(it) }
                loading.value = false
            }
            override fun onFailure(call: Call<List<FriendDto>>, t: Throwable) {
                loading.value = false
            }
        })
    }

    fun searchUsers(query: String) {
        searchResults.clear()
        fetchRequests()
        fetchFriends()
        api.searchUsers(query).enqueue(object : Callback<List<UserSearchResultDto>> {
            override fun onResponse(call: Call<List<UserSearchResultDto>>, response: Response<List<UserSearchResultDto>>) {
                response.body()?.let { searchResults.addAll(it) }
            }
            override fun onFailure(call: Call<List<UserSearchResultDto>>, t: Throwable) {
            }
        })
    }

    fun sendFriendRequest(toUserId: Int) {
        searchResults.clear()
        api.sendFriendRequest(FriendRequestBody(toUserId)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchRequests()
                val json = JSONObject()
                json.put("action", "friend_request_send")
                json.put("to", toUserId)
                WebSocketManager.send(json)
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
            }
        })
    }

    private fun fetchRequests() {
        receivedRequests.clear()
        sentRequests.clear()
        api.getRequests().enqueue(object : Callback<FriendRequestsResponse> {
            override fun onResponse(call: Call<FriendRequestsResponse>, response: Response<FriendRequestsResponse>) {
                response.body()?.let {
                    receivedRequests.addAll(it.received)
                    sentRequests.addAll(it.sent)
                }
            }
            override fun onFailure(call: Call<FriendRequestsResponse>, t: Throwable) {
            }
        })
    }

    fun acceptRequest(requestId: Int) {
        api.acceptRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchFriends()
                fetchRequests()
                val json = JSONObject()
                json.put("action", "friend_request_accept")
                json.put("id", requestId)
                WebSocketManager.send(json)
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
            }
        })
    }

    fun deleteRequest(requestId: Int) {
        api.deleteRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchRequests()
                searchResults.clear()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
            }
        })
    }

    fun removeFriend(friendId: Int) {
        api.removeFriend(friendId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchFriends()
                searchResults.clear()
                val json = JSONObject()
                json.put("action", "friend_delete")
                json.put("friendId", friendId)
                WebSocketManager.send(json)
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
            }
        })
    }
}
