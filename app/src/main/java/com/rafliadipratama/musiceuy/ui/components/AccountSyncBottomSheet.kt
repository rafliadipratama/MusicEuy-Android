package com.rafliadipratama.musiceuy.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.DarkSurface
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SyncViewTab {
    CHOOSE,
    YOUTUBE_WEBVIEW,
    SPOTIFY_INPUT,
    APPLE_INPUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSyncBottomSheet(
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTab by remember { mutableStateOf(SyncViewTab.CHOOSE) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when (currentTab) {
                SyncViewTab.CHOOSE -> {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Integrasi & Sinkronisasi Akun",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hubungkan akun musik favoritmu ke MusicEuy",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. YouTube Music Card
                    ProviderCard(
                        title = "YouTube Music / Google Account",
                        subtitle = "Sinkronkan riwayat putar, likes, dan playlist akun Google",
                        gradient = listOf(Color(0xFFFF0000), Color(0xFF990000)),
                        iconResId = com.rafliadipratama.musiceuy.R.drawable.ic_youtube_music,
                        onClick = { currentTab = SyncViewTab.YOUTUBE_WEBVIEW }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Spotify Card
                    ProviderCard(
                        title = "Spotify Playlist Importer",
                        subtitle = "Impor playlist publik atau Liked Songs Spotify ke MusicEuy",
                        gradient = listOf(Color(0xFF1DB954), Color(0xFF137535)),
                        iconResId = com.rafliadipratama.musiceuy.R.drawable.ic_spotify,
                        onClick = { currentTab = SyncViewTab.SPOTIFY_INPUT }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Apple Music Card
                    ProviderCard(
                        title = "Apple Music Importer",
                        subtitle = "Impor playlist Apple Music dan tangga lagu kurasi Apple",
                        gradient = listOf(Color(0xFFFC3C44), Color(0xFF9E1B21)),
                        iconResId = com.rafliadipratama.musiceuy.R.drawable.ic_apple_music,
                        onClick = { currentTab = SyncViewTab.APPLE_INPUT }
                    )
                }

                SyncViewTab.YOUTUBE_WEBVIEW -> {
                    YouTubeLoginWebView(
                        onSuccess = { email, name ->
                            Toast.makeText(context, "Berhasil masuk: $name", Toast.LENGTH_SHORT).show()
                            currentTab = SyncViewTab.CHOOSE
                        },
                        onBack = { currentTab = SyncViewTab.CHOOSE }
                    )
                }

                SyncViewTab.SPOTIFY_INPUT -> {
                    UrlImporterView(
                        title = "Impor Playlist Spotify",
                        placeholder = "https://open.spotify.com/playlist/...",
                        helpText = "Buka Spotify > Bagikan Playlist > Salin Tautan (Copy Link)",
                        onImport = { url ->
                            viewModel.importExternalPlaylist(url)
                            onDismiss()
                        },
                        onBack = { currentTab = SyncViewTab.CHOOSE }
                    )
                }

                SyncViewTab.APPLE_INPUT -> {
                    UrlImporterView(
                        title = "Impor Playlist Apple Music",
                        placeholder = "https://music.apple.com/playlist/...",
                        helpText = "Buka Apple Music > Bagikan Playlist > Salin Tautan",
                        onImport = { url ->
                            viewModel.importExternalPlaylist(url)
                            onDismiss()
                        },
                        onBack = { currentTab = SyncViewTab.CHOOSE }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    @androidx.annotation.DrawableRes iconResId: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(id = iconResId),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun UrlImporterView(
    title: String,
    placeholder: String,
    helpText: String,
    onImport: (String) -> Unit,
    onBack: () -> Unit
) {
    var urlText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Kembali", tint = TextMuted)
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen,
                unfocusedBorderColor = SurfaceContainer,
                focusedContainerColor = SurfaceContainerHigh,
                unfocusedContainerColor = SurfaceContainerHigh,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = helpText,
            color = TextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onImport(urlText.trim()) },
            enabled = urlText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Mulai Impor Playlist",
                color = Color(0xFF0C0E11),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeLoginWebView(
    onSuccess: (email: String?, name: String?) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    val loginUrl = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Login Akun Google / YouTube Music",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = BrandGreen,
                trackColor = SurfaceContainer
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        // Google rejects embedded WebViews unless `; wv` is removed from userAgentString
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (url != null && url.startsWith("https://music.youtube.com")) {
                                    val cookie = CookieManager.getInstance().getCookie(url)
                                    if (cookie != null && cookie.contains("SAPISID")) {
                                        // Save credentials to local preferences
                                        ctx.getSharedPreferences("musiceuy_yt_auth", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("cookie", cookie)
                                            .putBoolean("is_logged_in", true)
                                            .apply()
                                        onSuccess("Google Account", "YouTube Music User")
                                    }
                                }
                            }
                        }

                        loadUrl(loginUrl)
                    }
                }
            )
        }
    }
}
