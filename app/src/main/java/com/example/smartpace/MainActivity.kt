package com.example.smartpace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smartpace.navigation.SmartPaceApp
import com.example.smartpace.ui.theme.SmartPaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPaceTheme {
                SmartPaceApp()
            }
        }
    }
}
