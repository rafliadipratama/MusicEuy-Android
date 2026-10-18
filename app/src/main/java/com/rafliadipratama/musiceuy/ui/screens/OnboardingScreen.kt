package com.rafliadipratama.musiceuy.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.BrandGreenBright
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private enum class OnboardingStep {
    WELCOME,
    SELECT_ARTISTS
}

private data class OnboardingArtistItem(
    val name: String,
    val genre: String,
    val avatarUrl: String
)

@Composable
fun OnboardingScreen(
    viewModel: MusicPlayerViewModel,
    onComplete: (List<String>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val selectedArtists = remember { mutableStateListOf<String>() }

    // Curated Verified Artists (500x500 CDN)
    val curatedArtists = remember {
        listOf(
            OnboardingArtistItem("Hindia", "Indie Pop", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Hindia") ?: ""),
            OnboardingArtistItem("Tulus", "Pop / Soul", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Tulus") ?: ""),
            OnboardingArtistItem("Sheila On 7", "Pop Rock", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Sheila On 7") ?: ""),
            OnboardingArtistItem("Bernadya", "Pop / Indie", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Bernadya") ?: ""),
            OnboardingArtistItem("Mahalini", "Pop Ballad", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Mahalini") ?: ""),
            OnboardingArtistItem("Denny Caknan", "Dangdut Koplo", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Denny Caknan") ?: ""),
            OnboardingArtistItem("NIKI", "R&B / Pop", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("NIKI") ?: ""),
            OnboardingArtistItem("Taylor Swift", "Pop Global", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Taylor Swift") ?: ""),
            OnboardingArtistItem("Coldplay", "Alternative Rock", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Coldplay") ?: ""),
            OnboardingArtistItem("Sal Priadi", "Art Pop", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Sal Priadi") ?: ""),
            OnboardingArtistItem("Nadin Amizah", "Folk / Akustik", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Nadin Amizah") ?: ""),
            OnboardingArtistItem("Juicy Luicy", "Pop Akustik", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Juicy Luicy") ?: ""),
            OnboardingArtistItem("Bruno Mars", "Pop / Funk", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Bruno Mars") ?: ""),
            OnboardingArtistItem("Feast", "Rock / Indie", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Feast") ?: ""),
            OnboardingArtistItem("Fourtwnty", "Indie Folk", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Fourtwnty") ?: ""),
            OnboardingArtistItem("NDX A.K.A", "Dangdut Hip-Hop", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("NDX A.K.A") ?: ""),
            OnboardingArtistItem("Billie Eilish", "Alt Pop", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Billie Eilish") ?: ""),
            OnboardingArtistItem("Lyodra", "Pop Diva", com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl("Lyodra") ?: "")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "OnboardingTransition"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> {
                    AppleWelcomeContent(
                        onStart = { currentStep = OnboardingStep.SELECT_ARTISTS },
                        onSkip = onSkip
                    )
                }
                OnboardingStep.SELECT_ARTISTS -> {
                    AppleSelectArtistsContent(
                        allArtists = curatedArtists,
                        selectedArtists = selectedArtists,
                        onArtistToggle = { name ->
                            if (selectedArtists.contains(name)) {
                                selectedArtists.remove(name)
                            } else {
                                selectedArtists.add(name)
                            }
                        },
                        onNext = {
                            onComplete(selectedArtists.toList())
                        },
                        onBack = { currentStep = OnboardingStep.WELCOME }
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 1: APPLE MUSIC AESTHETIC WELCOME CONTENT
// ============================================================================
@Composable
private fun AppleWelcomeContent(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text(text = "Lewati", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFC3C44).copy(alpha = 0.25f), BrandGreen.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .border(2.dp, BrandGreen.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_musiceuy_logo),
                    contentDescription = "MusicEuy Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MusicEuy",
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.0).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Pengalaman Mendengarkan Musik Kelas Atas",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppleFeatureCard(
                    icon = Icons.Default.Headphones,
                    iconColor = Color(0xFFFC3C44),
                    title = "Streaming Lossless & Bebas Iklan",
                    description = "Dengarkan jutaan lagu dalam kualitas audio resolusi tinggi tanpa interupsi iklan apa pun."
                )
                AppleFeatureCard(
                    icon = Icons.Default.Sync,
                    iconColor = Color(0xFF1DB954),
                    title = "Multi-Platform Sync",
                    description = "Hubungkan akun YouTube Music, Spotify, dan Apple Music tanpa akun berbayar."
                )
                AppleFeatureCard(
                    icon = Icons.Default.Mic,
                    iconColor = Color(0xFF38EF7D),
                    title = "Lirik Realtime Karaoke",
                    description = "Lirik bergulir tersinkronisasi per baris dari LRCLib yang dapat disimpan offline."
                )
            }
        }

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Mulai Pengaturan",
                color = Color(0xFF0C0E11),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF0C0E11), modifier = Modifier.size(18.dp))
        }
    }
}

// ============================================================================
// STEP 2: CONNECT ACCOUNTS (YOUTUBE MUSIC / SPOTIFY / APPLE MUSIC)
// ============================================================================
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AppleConnectAccountContent(
    viewModel: MusicPlayerViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showYtLogin by remember { mutableStateOf(false) }
    var showSpotifyImport by remember { mutableStateOf(false) }
    var spotifyUrlInput by remember { mutableStateOf("") }
    var isYtConnected by remember {
        mutableStateOf(
            context.getSharedPreferences("musiceuy_yt_auth", android.content.Context.MODE_PRIVATE)
                .getBoolean("is_logged_in", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
                TextButton(onClick = onNext) {
                    Text("Lewati", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Hubungkan Akun",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sinkronkan daftar lagu dan riwayat putar dari layanan yang kamu gunakan.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. YouTube Music Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showYtLogin = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFF990000)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("YT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YouTube Music / Akun Google", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (isYtConnected) "✓ Akun Google Terhubung" else "Login via WebView aman tanpa password tersimpan",
                            color = if (isYtConnected) BrandGreen else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (isYtConnected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Spotify Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSpotifyImport = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1DB954), Color(0xFF0F5A28)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Spotify Playlist Importer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Impor link playlist Spotify langsung ke koleksi", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            if (showSpotifyImport) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = spotifyUrlInput,
                    onValueChange = { spotifyUrlInput = it },
                    placeholder = { Text("Tempel link playlist Spotify di sini...", color = TextMuted, fontSize = 12.sp) },
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
                Button(
                    onClick = {
                        viewModel.importExternalPlaylist(spotifyUrlInput.trim())
                        Toast.makeText(context, "Memulai impor playlist...", Toast.LENGTH_SHORT).show()
                        showSpotifyImport = false
                    },
                    enabled = spotifyUrlInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Impor Sekarang", color = Color(0xFF0C0E11), fontWeight = FontWeight.Bold)
                }
            }

            if (showYtLogin) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                                CookieManager.getInstance().setAcceptCookie(true)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        if (url != null && url.startsWith("https://music.youtube.com")) {
                                            val cookie = CookieManager.getInstance().getCookie(url)
                                            if (cookie != null && cookie.contains("SAPISID")) {
                                                ctx.getSharedPreferences("musiceuy_yt_auth", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putString("cookie", cookie)
                                                    .putBoolean("is_logged_in", true)
                                                    .apply()
                                                isYtConnected = true
                                                showYtLogin = false
                                            }
                                        }
                                    }
                                }
                                loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                            }
                        }
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Lanjutkan", color = Color(0xFF0C0E11), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF0C0E11), modifier = Modifier.size(18.dp))
        }
    }
}

// ============================================================================
// STEP 3: APPLE MUSIC BUBBLE ARTIST PICKER
// ============================================================================
@Composable
private fun AppleSelectArtistsContent(
    allArtists: List<OnboardingArtistItem>,
    selectedArtists: List<String>,
    onArtistToggle: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val canProceed = selectedArtists.size >= 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (canProceed) BrandGreen else SurfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (canProceed) "${selectedArtists.size} Dipilih ✓" else "Pilih minimal 3 (${selectedArtists.size}/3)",
                        color = if (canProceed) Color(0xFF0C0E11) else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pilih Artis Favorit",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Algoritma MusicEuy akan menyusun rekomendasi beranda berdasarkan pilihanmu.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Artist Bubble Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allArtists, key = { it.name }) { artist ->
                    val isSelected = selectedArtists.contains(artist.name)
                    AppleArtistBubble(
                        artist = artist,
                        isSelected = isSelected,
                        onClick = { onArtistToggle(artist.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onNext,
            enabled = canProceed,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandGreen,
                disabledContainerColor = SurfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = if (canProceed) "Selesai & Masuk Beranda" else "Pilih ${3 - selectedArtists.size} Artis Lagi",
                color = if (canProceed) Color(0xFF0C0E11) else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppleArtistBubble(
    artist: OnboardingArtistItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(SurfaceContainer)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) BrandGreen else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF0C0E11),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            color = if (isSelected) BrandGreenBright else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = artist.genre,
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// STEP 4: PROFILE SETUP (APPLE MUSIC ID CARD)
// ============================================================================
@Composable
private fun AppleProfileSetupContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    selectedArtistsCount: Int,
    isCalibrating: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Kartu Member MusicEuy",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Akun personal lokalmu siap untuk streaming.",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Apple Style Glass Member Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Brush.horizontalGradient(listOf(BrandGreen.copy(alpha = 0.5f), Color(0xFFFC3C44).copy(alpha = 0.3f))), RoundedCornerShape(26.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MUSICEUY PASS",
                            color = BrandGreenBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_musiceuy_logo),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BrandGreen.copy(alpha = 0.2f))
                                .border(2.dp, BrandGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = username.ifBlank { "Sobat MusicEuy" },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$selectedArtistsCount Artis Favorit • Hi-Res Audio",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TAHUN GABUNG", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("2026", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Bebas Iklan Selamanya", color = BrandGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Nama Pengguna / Nickname", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = SurfaceContainer,
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainerHigh,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = onComplete,
            enabled = !isCalibrating,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isCalibrating) {
                CircularProgressIndicator(color = Color(0xFF0C0E11), modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Menyiapkan Beranda...", color = Color(0xFF0C0E11), fontWeight = FontWeight.Bold)
            } else {
                Text("Selesai & Masuk ke MusicEuy", color = Color(0xFF0C0E11), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AppleFeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}
