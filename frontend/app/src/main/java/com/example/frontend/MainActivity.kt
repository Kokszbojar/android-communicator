package com.example.frontend

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend.ChatScreen
import com.example.frontend.ChatViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authViewModel = AuthViewModel()
        val tokenManager = TokenManager(this)

        setContent {
            val navController = rememberNavController()
            val savedToken = tokenManager.getToken()

            LaunchedEffect(savedToken) {
                if (savedToken != null) {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                startTokenRefreshLoop(tokenManager)                                         // Rozpoczęcie pętli odświeżania co 5 minut
            }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = { (access, refresh) ->
                            tokenManager.saveTokens(access, refresh)
                            navController.navigate("chat") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate("register")
                        }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("chat") {
                    val viewModel = ChatViewModel(token = tokenManager.getToken())
                    ChatScreen(viewModel)
                }
            }
        }
    }

    private fun refreshToken(tokenManager: TokenManager, onResult: (Boolean) -> Unit) {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            onResult(false)
            return
        }

        val call = RetrofitClient.instance.refresh(TokenRequest(refreshToken))
        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val newAccessToken = response.body()?.access
                    val newRefreshToken = response.body()?.refresh ?: refreshToken

                    if (newAccessToken != null) {
                        tokenManager.saveTokens(newAccessToken, newRefreshToken)
                        onResult(true)
                    }
                    else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("Refresh", "Błąd odświeżania tokena", t)
                onResult(false)
            }
        })
    }

    private fun startTokenRefreshLoop(tokenManager: TokenManager) {
        val handler = Handler(Looper.getMainLooper())
        val refreshRunnable = object : Runnable {
            override fun run() {
                refreshToken(tokenManager) { success ->
                    if (!success) {
                        println("Nie udało się odświeżyć tokena.")
                    }
                }
                handler.postDelayed(this, 5 * 60 * 1000L) // co 5 minut
            }
        }
        handler.post(refreshRunnable)
    }
}

