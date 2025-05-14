package com.example.frontend

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET

data class TokenRequest(val refresh: String)
data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val access: String, val refresh: String)

interface ApiService {
    @POST("api/register/")
    fun register(@Body request: AuthRequest): Call<Void>

    @POST("api/login/")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @POST("api/token/refresh/")
    fun refresh(@Body request: TokenRequest): Call<AuthResponse>
}
