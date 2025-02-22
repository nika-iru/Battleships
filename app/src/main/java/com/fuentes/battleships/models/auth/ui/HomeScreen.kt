package com.fuentes.battleships.models.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
fun HomeScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Battleships (insert image here)", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { //add authentication for login here
                navController.navigate("game_board") }) {
            Text(text = "Play")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            authViewModel.logout()
        }) {
            Text(text = "Logout")
        }
    }
}