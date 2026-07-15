package com.example.myapplication.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.myapplication.presentation.theme.AMOLEDBlack
import com.example.myapplication.presentation.theme.OrangeAccent

@Composable
fun LoginScreen(onSignInClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMOLEDBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Todolist",
                style = MaterialTheme.typography.titleMedium,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use same account as phone",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
