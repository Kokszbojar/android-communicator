package com.example.frontend

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val selectedUserId by viewModel.selectedUserId
    val messages = viewModel.messages
    val currentMessage by viewModel.currentMessage

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // Lista znajomych (lewa kolumna)
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E))
                .padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            listOf(1, 2, 3).forEach { userId ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (userId == selectedUserId) Color(0xFFBB86FC) else Color.Gray)
                        .clickable { viewModel.selectUser(userId) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userId.toString(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Główne okno czatu
        Column(modifier = Modifier.fillMaxSize()) {
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

                items(messages.reversed()) { message ->
                    ChatBubble(message)
                }
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
                        .height(48.dp),
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
                        .size(40.dp)
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
fun ChatBubble(message: String) {
    val isOwnMessage = message.startsWith("Ty:")
    val bubbleColor = if (isOwnMessage) Color(0xFF3700B3) else Color(0xFF2C2C2C)
    val textColor = Color.White
    val alignment = if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart

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
            Text(text = message, color = textColor, fontSize = 14.sp)
        }
    }
}
