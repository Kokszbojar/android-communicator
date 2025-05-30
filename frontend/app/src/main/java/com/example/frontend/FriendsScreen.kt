package com.example.frontend

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun FriendsScreen(viewModel: FriendViewModel, onNavigateToChat: (Int) -> Unit, onLogout: () -> Unit) {
    val friends = viewModel.friends
    val searchResults = viewModel.searchResults
    val receivedRequests = viewModel.receivedRequests
    val sentRequests = viewModel.sentRequests

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Znajomi", "Wyszukaj", "Zaproszenia")
    val searchQuery = remember { mutableStateOf("") }

    val insets = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.background(Color(0xFF121212)).fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF1E1E1E),
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(Color(0xFF121212)),
            topBar = {
                Column(modifier = Modifier.background(Color(0xFF1A1A1A)).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Twoje kontakty",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Wyloguj", tint = Color.White)
                        }
                    }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(12.dp)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> { // 👥 Znajomi
                        if (friends.isEmpty()) {
                            Text("Brak znajomych", color = Color.Gray)
                        } else {
                            friends.forEach { friend ->
                                FriendItem(friend) { onNavigateToChat(friend.id) }
                            }
                        }
                    }

                    1 -> { // 🔍 Wyszukiwanie
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            label = { Text("Szukaj użytkownika") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = { viewModel.searchUsers(searchQuery.value) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(vertical = 8.dp)
                        ) {
                            Text("Szukaj")
                        }

                        if (searchResults.isNotEmpty()) {
                            searchResults.forEach { result ->
                                SearchResultItem(
                                    result,
                                    friends,
                                    onSendRequest = { viewModel.sendFriendRequest(result.id) },
                                    onRemoveFriend = { viewModel.removeFriend(result.id) }
                                )
                            }
                        } else {
                            Text("Brak wyników", color = Color.Gray)
                        }
                    }

                    2 -> { // 📬 Zaproszenia
                        if (receivedRequests.isEmpty()) {
                            Text("Brak oczekujących zaproszeń", color = Color.Gray)
                        } else {
                            receivedRequests.forEach { request ->
                                ReceivedRequestItem(
                                    request,
                                    onAccept = { viewModel.acceptRequest(request.id) },
                                    onReject = { viewModel.deleteRequest(request.id) }
                                )
                            }
                        }
                        if (sentRequests.isEmpty()) {
                            Text("Brak wysłanych zaproszeń", color = Color.Gray)
                        } else {
                            sentRequests.forEach { request ->
                                SentRequestItem(
                                    request,
                                    onClick = { viewModel.deleteRequest(request.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(friend: FriendDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                friend.hasNewMessage = false
                onClick()
            }
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.username ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (friend.hasNewMessage) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = friend.timestamp ?: "",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Text(
            text = friend.lastMessage ?: "",
            color = Color.LightGray,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
fun SearchResultItem(result: UserSearchResultDto, friends: List<FriendDto>, onSendRequest: () -> Unit, onRemoveFriend: () -> Unit) {
    val isAlreadyFriend = friends.any { it.username == result.username }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF2C2C2C))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = result.username,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        when {
            isAlreadyFriend -> Button(onClick = onRemoveFriend) {
                Text("Usuń")
            }
            result.requestSent -> Text("Wysłano", color = Color.Gray)
            result.requestReceived -> Text("Oczekuje", color = Color.Yellow)
            else -> Button(onClick = onSendRequest) {
                Text("Zaproś")
            }
        }
    }
}

@Composable
fun ReceivedRequestItem(
    request: FriendRequestDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF2C2C2C))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = request.from_user,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onAccept, modifier = Modifier.padding(end = 8.dp)) {
            Text("Akceptuj")
        }
        OutlinedButton(onClick = onReject) {
            Text("Odrzuć")
        }
    }
}

@Composable
fun SentRequestItem(
    request: FriendRequestDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF2C2C2C))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = request.friend,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) {
            Text("Anuluj")
        }
    }
}