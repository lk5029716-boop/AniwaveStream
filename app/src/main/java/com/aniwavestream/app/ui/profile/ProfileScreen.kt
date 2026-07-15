package com.aniwavestream.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary

@Composable
fun ProfileScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .padding(20.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Guest Viewer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text("Aniwave Stream · Free demo", color = TextSecondary)
        Spacer(Modifier.height(28.dp))
        ProfileCard(
            title = "About this app",
            body = "Crunchyroll-style Android client built with Jetpack Compose, Material 3, " +
                "ExoPlayer, and Jikan (MyAnimeList) metadata. Episode playback uses public sample " +
                "videos only — not licensed anime streams."
        )
        Spacer(Modifier.height(12.dp))
        ProfileCard(
            title = "Stack",
            body = "Kotlin · Compose · Navigation · Retrofit · Coil · Media3 · DataStore"
        )
        Spacer(Modifier.height(12.dp))
        ProfileCard(
            title = "Legal",
            body = "Do not use this project to host or stream pirated content. " +
                "Wire a licensed CDN/API before any commercial release."
        )
        Spacer(Modifier.height(24.dp))
        Text("v1.0.0", color = TextMuted)
    }
}

@Composable
private fun ProfileCard(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = OrangePrimary)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
