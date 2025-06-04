package com.example.frontend

import retrofit2.http.*
import retrofit2.Call

data class FcmTokenRequest(val token: String)
data class TokenRequest(val refresh: String)
data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val access: String, val refresh: String, val userId: Int)

data class FriendDto(val id: Int, val username: String?, val lastMessage: String?, var hasNewMessage: Boolean, val timestamp: String?)
data class UserSearchResultDto(val id: Int, val username: String, val requestSent: Boolean, val requestReceived: Boolean)
data class FriendRequestDto(val id: Int, val from_user: String, val friend: String, val status: String)
data class FriendRequestBody(val to_user: Int)

data class FriendRequestsResponse(
    val received: List<FriendRequestDto>,
    val sent: List<FriendRequestDto>
)

interface ApiService {
    @POST("api/register/")
    fun register(@Body request: AuthRequest): Call<Void>

    @POST("api/login/")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @POST("api/token/refresh/")
    fun refresh(@Body request: TokenRequest): Call<AuthResponse>

    @GET("api/friends/")
    fun getFriends(): Call<List<FriendDto>>

    @GET("api/users/search/")
    fun searchUsers(@Query("q") query: String): Call<List<UserSearchResultDto>>

    @POST("api/friends/request/")
    fun sendFriendRequest(@Body body: FriendRequestBody): Call<Void>

    @GET("api/friends/requests/")
    fun getRequests(): Call<FriendRequestsResponse>

    @PATCH("api/friends/request/{id}/")
    fun acceptRequest(
        @Path("id") requestId: Int
    ): Call<Void>

    @DELETE("api/friends/request/{id}/")
    fun deleteRequest(
        @Path("id") requestId: Int
    ): Call<Void>

    @DELETE("api/friends/remove/{id}/")
    fun removeFriend(
        @Path("id") friendId: Int,
    ): Call<Void>
}
