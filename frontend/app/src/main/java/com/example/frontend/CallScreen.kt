package com.example.frontend

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import io.livekit.android.renderer.SurfaceViewRenderer

@Composable
fun CallScreen(
    viewModel: CallViewModel, // lub rememberViewModel()
    serverUrl: String,
    token: String,
    friendName: String = "Znajomy",
    friendAvatar: String = "",
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val videoTrack by viewModel.videoTrack.collectAsState()
    val callStatus by viewModel.callStatus.collectAsState()

    var permissionsGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isNotEmpty()) {
            launcher.launch(denied.toTypedArray())
        } else {
            permissionsGranted = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionsGranted) {
            if (callStatus == "connected" && videoTrack != null) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            setEnableHardwareScaler(true)
                            viewModel.initVideoRenderer(this)
                        }
                    },
                    update = { renderer -> videoTrack?.addRenderer(renderer) },
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    Modifier.fillMaxSize().padding(64.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            viewModel.disconnect()
                            onDisconnect()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Rozłącz", color = Color.White)
                    }
                }
            } else if (callStatus == "pending"){
                // 👤 Ekran oczekiwania
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (friendAvatar != "") {
                        AsyncImage(
                            model = friendAvatar,
                            contentDescription = "Awatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Awatar",
                            tint = Color.Gray,
                            modifier = Modifier.size(128.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Dzwonienie do $friendName…",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CircularProgressIndicator(color = Color.White)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            viewModel.disconnect()
                            onDisconnect()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Rozłącz", color = Color.White)
                    }
                }
            } else if (callStatus == "disconnected"){
            // 👤 Ekran zakończonego połączenia
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (friendAvatar != "") {
                    AsyncImage(
                        model = friendAvatar,
                        contentDescription = "Awatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Awatar",
                        tint = Color.Gray,
                        modifier = Modifier.size(128.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Zakończono połączenie z $friendName",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.disconnect()
                        onDisconnect()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Wróć do czatu", color = Color.White)
                }
            }
        }

            LaunchedEffect(Unit) {
                viewModel.connectToRoom(serverUrl, token)
            }
        } else {
            Text(
                "Oczekiwanie na uprawnienia…",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}