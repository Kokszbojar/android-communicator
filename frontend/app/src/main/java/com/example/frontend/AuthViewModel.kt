package com.example.frontend

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

class AuthViewModel(private val tokenManager: TokenManager, var rememberMe: Boolean) : ViewModel() {

    fun register(username: String, password: String, onResult: (Boolean) -> Unit) {
        val call = RetrofitClient.instance.register(AuthRequest(username, password))
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Auth", "Błąd rejestracji", t)
                onResult(false)
            }
        })
    }

    fun login(username: String, password: String, remember: Boolean, onResult: (Boolean, String?, String?) -> Unit) {
        val call = RetrofitClient.instance.login(AuthRequest(username, password))
        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    onResult(true, response.body()?.access, response.body()?.refresh)
                } else {
                    onResult(false, null, null)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("Auth", "Błąd logowania", t)
                onResult(false, null, null)
            }
        })
    }
}
