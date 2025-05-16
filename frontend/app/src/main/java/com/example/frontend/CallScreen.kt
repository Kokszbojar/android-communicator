package com.example.frontend

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.frontend.CallViewModel
import io.livekit.android.renderer.SurfaceViewRenderer
import livekit.org.webrtc.EglBase

//@Composable
//fun CallScreen(viewModel: CallViewModel) {
//    RoomScope(
//        url = "ws://192.168.0.130:7880",
//        token = viewModel.token,
//        audio = true,
//        video = true,
//        connect = true,
//    ) {
//        // Get all the tracks in the room.
//        val trackRefs = rememberTracks()
//
//        // Display the video tracks.
//        // Audio tracks are automatically played.
//        LazyColumn(modifier = Modifier.fillMaxSize()) {
//            items(trackRefs.size) { index ->
//                VideoTrackView(
//                    trackReference = trackRefs[index],
//                    modifier = Modifier.fillParentMaxHeight(0.5f)
//                )
//            }
//            item {
//                Text("Wczytaj starsze", color = Color.White, fontSize = 12.sp)
//                Text("Wczytaj starsze", color = Color.White, fontSize = 12.sp)
//                Text("Wczytaj starsze", color = Color.White, fontSize = 12.sp)
//                Text("Wczytaj starsze", color = Color.White, fontSize = 12.sp)
//            }
//        }
//    }
//}

@Composable
fun CallScreen(
    viewModel: CallViewModel, // lub rememberViewModel()
    serverUrl: String,
    token: String
) {
    val context = LocalContext.current
    val videoTrack by viewModel.videoTrack.collectAsState()

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

    if (permissionsGranted) {
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    setEnableHardwareScaler(true)
                    viewModel.initVideoRenderer(this)
                }
            },
            update = { renderer ->
                videoTrack?.addRenderer(renderer)
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {
            viewModel.connectToRoom(serverUrl, token)
        }
    } else {
        Text("Oczekiwanie na uprawnienia…")
    }
}