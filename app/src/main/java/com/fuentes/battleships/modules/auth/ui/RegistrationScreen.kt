package com.fuentes.battleships.modules.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .safeContentPadding() // adds safe padding, so that the content doesn't overlap the system bars
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var name by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Text(text = "Registration Screen")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    authViewModel.registerUser(email, password)

                    navController.popBackStack("login", inclusive = false)

                    // or pop back to specific route
                    // navController.popBackStack("login", inclusive = false)
                }) {
                Text(text = "Register")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.popBackStack("login", inclusive = false)

                    // or pop back to specific route
                    // navController.popBackStack("login", inclusive = false)
                }) {
                Text(text = "Cancel")
            }
        }
    }
}