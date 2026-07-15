package com.aniwavestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aniwavestream.app.ui.navigation.AniwaveNavHost
import com.aniwavestream.app.ui.theme.AniwaveTheme
import com.aniwavestream.app.ui.theme.Background

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AniwaveApp
        setContent {
            AniwaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AniwaveNavHost(
                        repository = app.repository,
                        library = app.library
                    )
                }
            }
        }
    }
}
