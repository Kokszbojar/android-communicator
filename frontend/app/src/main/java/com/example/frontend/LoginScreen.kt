package com.example.frontend

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: (Pair<String, String>) -> Unit, onNavigateToRegister: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = username, onValueChange = { username = it }, label = { Text("Login") })
        Spacer(Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Hasło") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            viewModel.login(username, password) { success, access, refresh ->
                if (success && access != null && refresh != null) {
                    onLoginSuccess(Pair(access, refresh))
                } else {
                    error = "Nieprawidłowe dane"
                }
            }
        }) {
            Text("Zaloguj się")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onNavigateToRegister() }) {
            Text("Nie masz konta? Zarejestruj się")
        }
        error?.let { Text(it, color = Color.Red) }
    }
}
