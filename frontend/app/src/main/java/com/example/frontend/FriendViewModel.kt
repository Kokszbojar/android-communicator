package com.example.frontend

import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import android.util.Log

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

    private fun listenForMessages() {
        WebSocketManager.addListener { json ->
            if (json.getString("type") == "chat_message") {
                val fromUser = json.getString("sender_name")
                val message = json.optString("content")
                val timestamp = json.optString("timestamp")
                updateFriendWithMessage(fromUser, message, timestamp)
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

    fun fetchFriends() {
        loading.value = true
        api.getFriends().enqueue(object : Callback<List<FriendDto>> {
            override fun onResponse(call: Call<List<FriendDto>>, response: Response<List<FriendDto>>) {
                friends.clear()
                response.body()?.let { friends.addAll(it) }
                loading.value = false
            }

            override fun onFailure(call: Call<List<FriendDto>>, t: Throwable) {
                loading.value = false
            }
        })
    }

    fun searchUsers(query: String) {
        api.searchUsers(query).enqueue(object : Callback<List<UserSearchResultDto>> {
            override fun onResponse(call: Call<List<UserSearchResultDto>>, response: Response<List<UserSearchResultDto>>) {
                searchResults.clear()
                response.body()?.let { searchResults.addAll(it) }
            }

            override fun onFailure(call: Call<List<UserSearchResultDto>>, t: Throwable) {}
        })
    }

    fun sendFriendRequest(toUserId: Int) {
        api.sendFriendRequest(FriendRequestBody(toUserId)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
            }
        })
    }

    fun fetchRequests() {
        api.getRequests().enqueue(object : Callback<FriendRequestsResponse> {
            override fun onResponse(call: Call<FriendRequestsResponse>, response: Response<FriendRequestsResponse>) {
                receivedRequests.clear()
                sentRequests.clear()
                response.body()?.let {
                    receivedRequests.addAll(it.received)
                    sentRequests.addAll(it.sent)
                }
            }
            override fun onFailure(call: Call<FriendRequestsResponse>, t: Throwable) {}
        })
    }

    fun acceptRequest(requestId: Int) {
        api.acceptRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchFriends()
                fetchRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    fun deleteRequest(requestId: Int) {
        api.deleteRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    fun removeFriend(friendId: Int) {
        api.removeFriend(friendId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchFriends()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
}
