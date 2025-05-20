package com.example.frontend

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend.ChatScreen
import com.example.frontend.CallScreen
import com.example.frontend.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

suspend fun fetchLiveKitToken(token: String?): String? {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("http://192.168.0.130:8000/api/livekit-token/?room=room_1")
        .addHeader("Authorization", "Bearer $token")
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                json.getString("token")
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val tokenManager = TokenManager(this)

        setContent {
            val navController = rememberNavController()
            val savedToken = tokenManager.getToken()
            val context = LocalContext.current

            LaunchedEffect(savedToken) {
                if (savedToken == null) {
                    navController.navigate("login") {
                        popUpTo("friends") { inclusive = true }
                    }
                }
                startTokenRefreshLoop(              // Rozpoczęcie pętli odświeżania co 5 minut
                    context,
                    tokenManager
                )
            }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(
                        viewModel = AuthViewModel(tokenManager),
                        onLoginSuccess = { (access, refresh) ->
                            Log.d("AuthInterceptor", "Refresh. Token: $access")
                            tokenManager.saveTokens(access, refresh)
                            navController.navigate("friends") {
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
                        viewModel = AuthViewModel(tokenManager),
                        onRegisterSuccess = {
                            navController.navigate("register") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("friends") {
                    FriendsScreen(
                        viewModel = FriendViewModel(tokenManager),
                        onNavigateToChat = { userId ->
                            navController.navigate("chat/$userId")
                        },
                    )
                }
                composable("chat/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                    if (userId != null) {
                        val viewModel =
                            ChatViewModel(userId = userId, token = tokenManager.getToken())
                        ChatScreen(
                            viewModel,
                            onNavigateToCall = {
                                navController.navigate("call")
                            },
                            onNavigateToFriends = {
                                navController.navigate("friends")
                            }
                        )
                    }
                }
                composable("call") {
                    val viewModel = remember { CallViewModel(context) }
                    var token by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        token = fetchLiveKitToken(tokenManager.getToken())
                        if (token == null) {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                    token?.let {
                        CallScreen(
                            viewModel = viewModel,
                            serverUrl = "ws://192.168.0.130:7880/",
                            token = it
                        )
                    } ?: run {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    fun refreshToken(
        context: android.content.Context,
        tokenManager: TokenManager,
        onResult: (Boolean) -> Unit
    ) {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            onResult(false)
            return
        }

        val call = RetrofitClient.getInstance(tokenManager).refresh(TokenRequest(refreshToken))
        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(
                call: Call<AuthResponse>,
                response: Response<AuthResponse>
            ) {
                if (response.isSuccessful) {
                    val newAccessToken = response.body()?.access
                    val newRefreshToken = response.body()?.refresh ?: refreshToken

                    if (newAccessToken != null) {
                        tokenManager.saveTokens(newAccessToken, newRefreshToken)
                        onResult(true)
                    } else {
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

    fun startTokenRefreshLoop(
        context: android.content.Context,
        tokenManager: TokenManager
    ) {
        val handler = Handler(Looper.getMainLooper())
        val refreshRunnable = object : Runnable {
            override fun run() {
                refreshToken(context, tokenManager) { success ->
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

