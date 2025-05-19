package com.example.frontend

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToCall: () -> Unit) {
    val selectedUserId by viewModel.selectedUserId
    val messages = viewModel.messages
    val currentMessage by viewModel.currentMessage
    val insets = WindowInsets.systemBars.asPaddingValues()

    var showFriendList = remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets)
            .background(Color(0xFF121212))
    ) {
        if (showFriendList.value) {
            FriendList(
                users = listOf(1, 2, 3),
                onSelect = { userId ->
                    viewModel.selectUser(userId)
                    showFriendList.value = false
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatTopBar(
                    user = selectedUserId ?: 0,
                    onCallClick = onNavigateToCall,
                    onBack = { showFriendList.value = true }
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

                    items(messages.reversed()) { ChatBubble(it) }
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
}

@Composable
fun FriendList(users: List<Int>, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        users.forEach { userId ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(userId) }
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    ) {
                        // Avatar Placeholder
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "User_$userId", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "7d+", color = Color.Gray, fontSize = 18.sp)
                    }
                }
                Text(
                    text = "Ostatnia wiadomość",
                    color = Color.LightGray,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
fun ChatTopBar(user: Int, onCallClick: () -> Unit, onBack: () -> Unit) {
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
        Text(text = "User_$user", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCallClick) {
            Icon(Icons.Default.Phone, contentDescription = "Zadzwoń", tint = Color.Green)
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val bubbleColor = if (message.isOwn) Color(0xFF3700B3) else Color(0xFF2C2C2C)
    val alignment = if (message.isOwn) Alignment.CenterEnd else Alignment.CenterStart

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
            Text(text = message.text, color = Color.White, fontSize = 14.sp)
        }
    }
}

data class Message(
    val text: String,
    val isOwn: Boolean
)
