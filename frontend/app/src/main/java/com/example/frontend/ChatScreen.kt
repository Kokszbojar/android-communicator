package com.example.frontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontend.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages = viewModel.messages
    val currentMessage by viewModel.currentMessage

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = currentMessage,
                    onValueChange = { viewModel.onMessageChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Wpisz wiadomość...") }
                )
                Button(onClick = { viewModel.sendMessage() }) {
                    Text("Wyślij")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { msg ->
                Text(msg)
            }
        }
    }
}
