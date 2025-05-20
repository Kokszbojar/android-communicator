package com.example.frontend

import retrofit2.http.*
import retrofit2.Call

data class TokenRequest(val refresh: String)
data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val access: String, val refresh: String)

data class FriendDto(val id: Int, val username: String?, val lastMessage: String?, val timestamp: String?)
data class UserSearchResultDto(val id: Int, val username: String, val requestSent: Boolean)
data class FriendRequestDto(val id: Int, val from_user: String, val to_user: String, val status: String)
data class FriendRequestAction(val action: String)
data class FriendRequestBody(val to_user_id: Int)

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
    fun respondToRequest(
        @Path("id") requestId: Int,
        @Body action: FriendRequestAction
    ): Call<Void>
}
