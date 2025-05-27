package com.example.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (Pair<String, String>) -> Unit,
    onRememberLogin: (Pair<String, String>) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val inputRemember = viewModel.rememberMe
    var rememberMe by remember { mutableStateOf(inputRemember) }
    var error by remember { mutableStateOf<String?>(null) }
    val insets = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(insets)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Zaloguj się",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Login", color = Color.White) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło", color = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.Gray
                    )
                )
                Text("Zapamiętaj mnie", color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.login(username, password, rememberMe) { success, access, refresh ->
                        if (success && access != null && refresh != null) {
                            onLoginSuccess(Pair(access, refresh))
                            if (rememberMe) {
                                onRememberLogin(Pair(username, password))
                            }
                        } else {
                            error = "Nieprawidłowe dane"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F51B5),
                    contentColor = Color.White
                )
            ) {
                Text("Zaloguj się")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { onNavigateToRegister() }) {
                Text("Nie masz konta? Zarejestruj się", color = Color(0xFF90CAF9))
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.Red)
            }
        }
    }
}
