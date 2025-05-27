package com.example.frontend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nowy token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Wiadomość: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: "Nowa wiadomość"
        val body = remoteMessage.notification?.body ?: "Masz nową wiadomość"

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "chat_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Jeśli chcesz przekazać dane, dodaj je tu
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Dla Androida 8+ potrzebny kanał
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wiadomości czatu",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o nowych wiadomościach"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.android_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}

fun enableNotifications(tokenManager: TokenManager) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            Log.d("FCM", "Token: $token")
            val api = RetrofitClient.getInstance(tokenManager)
            val call = api.updateFcmToken(FcmTokenRequest(token))

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "Token FCM wysłany pomyślnie")
                    } else {
                        Log.e("FCM", "Nie udało się zarejestrować tokena FCM: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCM", "Błąd sieci przy wysyłaniu tokena FCM: ${t.message}")
                }
            })
        }
    }
}

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

class MainActivity : ComponentActivity(), LifecycleEventObserver {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        val tokenManager = TokenManager(this)

        setContent {
            val navController = rememberNavController()
            val rememberMe = tokenManager.getRemember()
            val context = LocalContext.current
            LaunchedEffect(rememberMe) {
                if (!rememberMe) {
                    navController.navigate("login") {
                        popUpTo("loading") { inclusive = true }
                    }
                } else {
                    val credentials = tokenManager.decryptCredentials()
                    AuthViewModel(tokenManager, true).login(credentials.first, credentials.second, true) { success, access, refresh ->
                        if (success && access != null && refresh != null) {
                            WebSocketManager.initialize(access)
                            tokenManager.saveTokens(access, refresh)
                            enableNotifications(tokenManager)
                            startTokenRefreshLoop(              // Rozpoczęcie pętli odświeżania co 5 minut
                                tokenManager
                            )
                            navController.navigate("friends") {
                                popUpTo("loading") { inclusive = true }
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
                        viewModel = AuthViewModel(tokenManager, rememberMe),
                        onLoginSuccess = { (access, refresh) ->
                            WebSocketManager.initialize(access)
                            tokenManager.saveTokens(access, refresh)
                            WindowCompat.setDecorFitsSystemWindows(window, false)
                            enableNotifications(tokenManager)
                            startTokenRefreshLoop(
                                tokenManager
                            )
                            navController.navigate("friends")
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
                        viewModel = AuthViewModel(tokenManager, false),
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
                        onLogout = {
                            tokenManager.clearLoginData()
                            navController.navigate("login") {
                                popUpTo("friends") { inclusive = true }
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
                            navController.navigate("login")
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

