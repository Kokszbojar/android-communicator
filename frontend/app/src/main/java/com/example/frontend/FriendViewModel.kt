package com.example.frontend

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
                fetchSentRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    fun fetchReceivedRequests() {
        api.getReceivedRequests().enqueue(object : Callback<List<FriendRequestDto>> {
            override fun onResponse(call: Call<List<FriendRequestDto>>, response: Response<List<FriendRequestDto>>) {
                receivedRequests.clear()
                response.body()?.let { receivedRequests.addAll(it) }
            }

            override fun onFailure(call: Call<List<FriendRequestDto>>, t: Throwable) {}
        })
    }

    fun fetchSentRequests() {
        api.getSentRequests().enqueue(object : Callback<List<FriendRequestDto>> {
            override fun onResponse(call: Call<List<FriendRequestDto>>, response: Response<List<FriendRequestDto>>) {
                sentRequests.clear()
                response.body()?.let { sentRequests.addAll(it) }
            }

            override fun onFailure(call: Call<List<FriendRequestDto>>, t: Throwable) {}
        })
    }

    fun acceptRequest(requestId: Int) {
        api.acceptRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchFriends()
                fetchReceivedRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    fun rejectRequest(requestId: Int) {
        api.rejectRequest(requestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                fetchReceivedRequests()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
}
