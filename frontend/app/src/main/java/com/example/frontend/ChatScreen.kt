package com.example.frontend

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import java.io.File

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload", null, context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, context: Context, onNavigateToCall: () -> Unit, onNavigateToFriends: () -> Unit) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val type = when {
                mimeType.startsWith("image") -> "image"
                mimeType.startsWith("audio") -> "audio"
                else -> "file"
            }
            file?.let { safeFile -> viewModel.sendFileMessage(safeFile, type, mimeType) }
        }
    }
    val friendName = viewModel.friendName.value
    val selectedUserId = viewModel.userId
    val messages = viewModel.messages
    val currentMessage by viewModel.currentMessage
    val insets = WindowInsets.systemBars.asPaddingValues()


    Box(modifier = Modifier.background(Color(0xFF121212)).fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            ChatTopBar(
                friendName = friendName,
                onCallClick = onNavigateToCall,
                onBack = onNavigateToFriends
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                reverseLayout = true
            ) {
                item {
                    if (viewModel.canLoadMore.value) {
                        Button(
                            onClick = {
                                viewModel.currentOffset.value += 50
                                viewModel.loadMessagesForUser(
                                    selectedUserId,
                                    offset = viewModel.currentOffset.value,
                                    appendToTop = true
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Wczytaj starsze", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                items(messages.reversed()) { ChatBubble(friendName, context, it) }
            }

            // Pole do wpisywania wiadomości
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = currentMessage,
                    onValueChange = viewModel::onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        focusedContainerColor = Color(0xFF2C2C2C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Wpisz wiadomość...", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                        .background(Color.Gray, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Dołącz plik",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(52.dp)
                        .background(Color(0xFFBB86FC), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Wyślij",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(friendName: String, onCallClick: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        ) {
            // Avatar Placeholder
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = friendName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCallClick) {
            Icon(Icons.Default.Phone, contentDescription = "Zadzwoń", tint = Color.Green)
        }
    }
}

@Composable
fun ChatBubble(friendName: String, context: Context, message: Message) {
    val bubbleColor = if (message.fromUser != friendName) Color(0xFF3700B3) else Color(0xFF2C2C2C)
    val alignment = if (message.fromUser != friendName) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.text.isNotBlank()) {
                Text(text = message.text, color = Color.White, fontSize = 14.sp)
            }

            message.fileUrl?.let { url ->
                when (message.fileType) {
                    "image" -> AsyncImage(
                        model = url,
                        contentDescription = "Obraz",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                    )
                    "audio" -> Text(
                        text = "Wiadomość audio - kliknij aby pobrać",
                        color = Color.Cyan,
                        modifier = Modifier.clickable {
                            val request = DownloadManager.Request(url.toUri())
                                .setTitle("Audio")
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "komunikator_wiadomosc_audio.mp3")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                        }
                    )
                }
            }
        }
    }
}

data class Message(
    val text: String,
    val fromUser: String,
    val timestamp: String,
    val fileUrl: String? = null,
    val fileType: String? = null
)
