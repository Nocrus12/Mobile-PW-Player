package com.example.lab_8_player


import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lab_8_player.ui.theme.Lab_8_PlayerTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        setContent {
            Lab_8_PlayerTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        val intent = Intent(applicationContext, PlaybackService::class.java).apply {
                            action = "ACTION_PLAY"
                        }
                        ContextCompat.startForegroundService(applicationContext, intent)
                    }) {
                        Text("Start Service")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val intent = Intent(applicationContext, PlaybackService::class.java).apply {
                            action = "ACTION_STOP"
                        }
                        ContextCompat.startForegroundService(applicationContext, intent)
                    }) {
                        Text("Stop Service")
                    }
                }
            }
        }
    }
}

