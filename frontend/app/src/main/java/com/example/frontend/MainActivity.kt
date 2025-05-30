package com.example.frontend

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

suspend fun fetchLiveKitToken(token: String?, userId: Int): String? {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("http://192.168.0.130:8000/api/livekit-token/?room=$userId")
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

class MainActivity : ComponentActivity(), LifecycleEventObserver {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        val tokenManager = TokenManager(this)

        val navigateToChat = intent?.getBooleanExtra("navigate_to_chat", false) ?: false
        val notificationUserId = intent?.getIntExtra("userId", -1) ?: -1
        val navigateToCall= intent?.getBooleanExtra("navigate_to_call", false) ?: false
        val notificationRoomId = intent?.getIntExtra("roomId", -1) ?: -1

        setContent {
            val navController = rememberNavController()
            val alreadyNavigated = remember { mutableStateOf(false) }
            val rememberMe = tokenManager.getRemember()
            val context = LocalContext.current
            LaunchedEffect(rememberMe) {
                if (!rememberMe) {
                    navController.navigate("login") {
                        popUpTo("login") { saveState=false }
                    }
                } else {
                    val credentials = tokenManager.decryptCredentials()
                    AuthViewModel(true).login(credentials.first, credentials.second, true) { success, access, refresh, userId ->
                        if (success && access != null && refresh != null && userId != null) {
                            WebSocketManager.initialize(access)
                            tokenManager.saveTokens(access, refresh)
                            startTokenRefreshLoop(tokenManager)
                            if (!isServiceRunning(context, MqttService::class.java)) {
                                val intent = Intent(context, MqttService::class.java).apply {
                                    putExtra("user_id", userId)
                                }
                                context.startForegroundService(intent)
                            }
                            if (navigateToChat && notificationUserId != -1 && !alreadyNavigated.value) {
                                navController.navigate("chat/$notificationUserId") {
                                    popUpTo("friends") { saveState=false }
                                }
                                alreadyNavigated.value = true
                            } else if (navigateToCall && notificationRoomId != -1 && !alreadyNavigated.value) {
                                navController.navigate("call/$notificationRoomId") {
                                    popUpTo("friends") { saveState=false }
                                }
                                alreadyNavigated.value = true
                            } else {
                                navController.navigate("friends") {
                                    popUpTo("friends") { saveState=false }
                                }
                            }
                        }
                    }
                }
            }

            NavHost(navController = navController, startDestination = "loading") {
                composable("loading") {
                    LoadingScreen()
                }
                composable("login") {
                    LoginScreen(
                        viewModel = AuthViewModel(rememberMe),
                        onLoginSuccess = { access, refresh, userId ->
                            WebSocketManager.initialize(access)
                            tokenManager.saveTokens(access, refresh)
                            WindowCompat.setDecorFitsSystemWindows(window, false)
                            startTokenRefreshLoop(tokenManager)
                            if (!isServiceRunning(context, MqttService::class.java)) {
                                val intent = Intent(context, MqttService::class.java).apply {
                                    putExtra("user_id", userId)
                                }
                                context.startForegroundService(intent)
                            }
                            navController.navigate("friends") {
                                popUpTo("friends") { saveState=false }
                            }
                        },
                        onRememberLogin = { (username, password) ->
                            tokenManager.clearLoginData()
                            tokenManager.encryptAndSave(username, password, true)
                        },
                        onNavigateToRegister = {
                            navController.navigate("register")
                        }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        viewModel = AuthViewModel(false),
                        onRegisterSuccess = {
                            navController.navigate("login") {
                                popUpTo("login") { saveState=false }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                    popUpTo("login") { saveState=false }
                            }
                        }
                    )
                }
                composable("friends") {
                    FriendsScreen(
                        viewModel = FriendViewModel(tokenManager),
                        onNavigateToChat = { userId ->
                            navController.navigate("chat/$userId") {
                                popUpTo("friends") { saveState=false }
                            }
                        },
                        onLogout = {
                            tokenManager.clearLoginData()
                            val intent = Intent(context, MqttService::class.java)
                            context.stopService(intent)
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable("chat/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                    if (userId != null) {
                        val viewModel =
                            ChatViewModel(userId = userId, token = tokenManager.getToken())
                        ChatScreen(
                            viewModel,
                            context = context,
                            onNavigateToCall = {
                                navController.navigate("call/$userId") {
                                    popUpTo("friends") { saveState=false }
                                }
                            },
                            onNavigateToFriends = {
                                navController.navigate("friends")
                            }
                        )
                    }
                }
                composable("call/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                    if (userId != null) {
                        val viewModel = remember { CallViewModel(context) }
                        var token by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            token = fetchLiveKitToken(tokenManager.getToken(), userId)
                            if (token == null) {
                                navController.navigate("login") {
                                    popUpTo("login") { saveState=false }
                                }
                            }
                        }
                        token?.let {
                            CallScreen(
                                viewModel = viewModel,
                                serverUrl = "ws://192.168.0.130:7880/",
                                token = it,
                                onDisconnect = {
                                    navController.navigate("friends") {
                                        popUpTo("friends") { saveState=false }
                                    }
                                }
                            )
                        } ?: run {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    fun refreshToken(
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
        tokenManager: TokenManager
    ) {
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

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                // App wróciła na pierwszy plan – odśwież połączenie
                val token = TokenManager(this).getToken()
                if (token != null) {
                    WebSocketManager.initialize(token)
                }
            }

            Lifecycle.Event.ON_STOP -> {
                // App poszła w tło – rozłącz WS, jeśli chcesz
                WebSocketManager.close()
            }

            else -> Unit
        }
    }
}

