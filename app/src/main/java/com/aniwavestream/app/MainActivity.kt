package com.aniwavestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aniwavestream.app.ui.navigation.AniwaveNavHost
import com.aniwavestream.app.ui.player.PlayerPipState
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

    // Auto-enter PiP when the user leaves the activity while an episode plays.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PlayerPipState.active) {
            val rational = android.util.Rational(16, 9)
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(rational)
                .build()
            try {
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
                // Device may not support PiP — ignore.
            }
        }
    }
}
