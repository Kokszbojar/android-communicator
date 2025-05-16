package com.example.frontend

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val insets = WindowInsets.systemBars.asPaddingValues()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(insets)
        .padding(start=20.dp, end=20.dp)
    ) {
        TextField(value = username, onValueChange = { username = it }, label = { Text("Login") })
        Spacer(Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Hasło") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            viewModel.register(username, password) { success ->
                if (success) {
                    onRegisterSuccess()
                } else {
                    error = "Rejestracja nie powiodła się"
                }
            }
        }) {
            Text("Zarejestruj się")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onNavigateToLogin() }) {
            Text("Masz już konto? Zaloguj się")
        }
        error?.let { Text(it, color = Color.Red) }
    }
}
