package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.BrandGreenBright
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerLow
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

    // Daftar Kurasi Musisi Populer Indonesia & Internasional
    val curatedArtists = remember {
        listOf(
            OnboardingArtistItem("Hindia", "Indie Pop", "https://yt3.googleusercontent.com/a/ACg8ocLq8Hindia=w500-h500"),
            OnboardingArtistItem("Tulus", "Pop / Soul", "https://yt3.googleusercontent.com/a/ACg8ocTulus=w500-h500"),
            OnboardingArtistItem("Sheila On 7", "Pop Rock", "https://lh3.googleusercontent.com/ZBI5XEhdUvkE59Kr7HKZYIjoopuA_kC0uPAWNsEw-EmLfTtRaXuDEv-qYWTv_lahkpA8Yuhe7P-sO2Y=w500-h500"),
            OnboardingArtistItem("Bernadya", "Pop / Indie", "https://yt3.googleusercontent.com/a/ACg8ocIdkG482z9_Bernadya=w500-h500"),
            OnboardingArtistItem("Mahalini", "Pop Ballad", "https://yt3.googleusercontent.com/a/ACg8ocMahalini=w500-h500"),
            OnboardingArtistItem("Denny Caknan", "Dangdut Koplo", "https://yt3.googleusercontent.com/a/ACg8ocDennyCaknan=w500-h500"),
            OnboardingArtistItem("NIKI", "R&B / Pop", "https://yt3.googleusercontent.com/a/ACg8ocNIKI_Official=w500-h500"),
            OnboardingArtistItem("Taylor Swift", "Pop Global", "https://yt3.googleusercontent.com/a/ACg8ocTaylorSwift=w500-h500"),
            OnboardingArtistItem("Coldplay", "Alternative Rock", "https://yt3.googleusercontent.com/a/ACg8ocColdplay_Band=w500-h500"),
            OnboardingArtistItem("Sal Priadi", "Art Pop", "https://yt3.googleusercontent.com/a/ACg8ocSalPriadi=w500-h500"),
            OnboardingArtistItem("Nadin Amizah", "Folk / Akustik", "https://yt3.googleusercontent.com/a/ACg8ocNadinAmizah=w500-h500"),
            OnboardingArtistItem("Juicy Luicy", "Pop Akustik", "https://yt3.googleusercontent.com/a/ACg8ocJuicyLuicy=w500-h500"),
            OnboardingArtistItem("Bruno Mars", "Pop / Funk", "https://yt3.googleusercontent.com/a/ACg8ocBrunoMars=w500-h500"),
            OnboardingArtistItem("Feast", "Rock / Indie", "https://yt3.googleusercontent.com/a/ACg8ocFeast_Rock=w500-h500"),
            OnboardingArtistItem("Fourtwnty", "Indie Folk", "https://yt3.googleusercontent.com/a/ACg8ocFourtwnty=w500-h500"),
            OnboardingArtistItem("NDX A.K.A", "Dangdut Hip-Hop", "https://yt3.googleusercontent.com/a/ACg8ocNDXAKA_Music=w500-h500"),
            OnboardingArtistItem("Guyon Waton", "Dangdut Akustik", "https://yt3.googleusercontent.com/a/ACg8ocGuyonWaton=w500-h500"),
            OnboardingArtistItem("Billie Eilish", "Alt Pop", "https://yt3.googleusercontent.com/a/ACg8ocBillieEilish=w500-h500"),
            OnboardingArtistItem("Lyodra", "Pop Divas", "https://yt3.googleusercontent.com/a/ACg8ocLyodra=w500-h500"),
            OnboardingArtistItem("Tiara Andini", "Pop", "https://yt3.googleusercontent.com/a/ACg8ocTiaraAndini=w500-h500"),
            OnboardingArtistItem("Dewa 19", "Rock Legend", "https://yt3.googleusercontent.com/a/ACg8ocDewa19_Official=w500-h500"),
            OnboardingArtistItem("Noah", "Pop Rock", "https://yt3.googleusercontent.com/a/ACg8ocNoah_Band=w500-h500"),
            OnboardingArtistItem("Raim Laode", "Pop Folk", "https://yt3.googleusercontent.com/a/ACg8ocRaimLaode=w500-h500"),
            OnboardingArtistItem("Pamungkas", "Indie Pop", "https://yt3.googleusercontent.com/a/ACg8ocPamungkas=w500-h500")
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
                if (targetState == OnboardingStep.SELECT_ARTISTS) {
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
                    WelcomeStepContent(
                        onStart = { currentStep = OnboardingStep.SELECT_ARTISTS },
                        onSkip = onSkip
                    )
                }
                OnboardingStep.SELECT_ARTISTS -> {
                    SelectArtistsStepContent(
                        allArtists = curatedArtists,
                        selectedArtists = selectedArtists,
                        isCalibrating = uiState.isCalibratingOnboarding,
                        onArtistToggle = { name ->
                            if (selectedArtists.contains(name)) {
                                selectedArtists.remove(name)
                            } else {
                                selectedArtists.add(name)
                            }
                        },
                        onBack = { currentStep = OnboardingStep.WELCOME },
                        onSkip = onSkip,
                        onComplete = {
                            onComplete(selectedArtists.toList())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
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
        // Skip Button di Top Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Lewati",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Hero Logo & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BrandGreen.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(2.dp, BrandGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_musiceuy_logo),
                    contentDescription = "MusicEuy Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Selamat Datang di",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "MusicEuy",
                color = BrandGreenBright,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Nikmati pemutar musik streaming tanpa iklan, hemat kuota, dengan lirik realtime dan kualitas audio terbaik.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.widthIn(max = 340.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3 Kartu Keunggulan
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureHighlightCard(
                    icon = Icons.Default.Headphones,
                    title = "100% Bebas Iklan",
                    description = "Dengarkan jutaan lagu favorit tanpa gangguan atau jeda iklan apa pun."
                )
                FeatureHighlightCard(
                    icon = Icons.Default.Speed,
                    title = "Audio Caching & Unduhan",
                    description = "Putar sekali, audio otomatis di-cache dan dapat disimpan ke galeri lokal HP."
                )
                FeatureHighlightCard(
                    icon = Icons.Default.Mic,
                    title = "Lirik Realtime LRCLIB",
                    description = "Bernyanyi seru dengan lirik sinkron otomatis yang tetap tersimpan saat offline."
                )
            }
        }

        // Action Buttons di Bawah
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Mulai & Pilih Musisi Favorit",
                    color = Color(0xFF0C0E11),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Langsung masuk ke Beranda",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SelectArtistsStepContent(
    allArtists: List<OnboardingArtistItem>,
    selectedArtists: List<String>,
    isCalibrating: Boolean,
    onArtistToggle: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredArtists = remember(searchQuery, allArtists) {
        if (searchQuery.isBlank()) {
            allArtists
        } else {
            allArtists.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.genre.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }

                TextButton(onClick = onSkip) {
                    Text(
                        text = "Lewati",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Title & Instruction
            Text(
                text = "Siapa musisi favoritmu?",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pilih minimal 3 musisi agar rekomendasi Beranda langsung menyajikan lagu yang sesuai seleramu.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar untuk mencari musisi
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "Cari musisi lain...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = SurfaceContainerHigh,
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Grid Musisi
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If searched artist doesn't match list, offer to add custom artist
                if (filteredArtists.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        val customName = searchQuery.trim()
                        val isSelected = selectedArtists.contains(customName)
                        ArtistCircleItem(
                            name = customName,
                            genre = "Musisi Kustom",
                            avatarUrl = "",
                            isSelected = isSelected,
                            onToggle = { onArtistToggle(customName) }
                        )
                    }
                }

                items(filteredArtists, key = { it.name }) { artist ->
                    val isSelected = selectedArtists.contains(artist.name)
                    ArtistCircleItem(
                        name = artist.name,
                        genre = artist.genre,
                        avatarUrl = artist.avatarUrl,
                        isSelected = isSelected,
                        onToggle = { onArtistToggle(artist.name) }
                    )
                }
            }
        }

        // Floating Bottom Sticky Dock: Counter & Finish Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color(0xFF161B22),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status counter
                val count = selectedArtists.size
                val statusText = when {
                    count == 0 -> "Pilih minimal 3 musisi"
                    count == 1 -> "Pilih 2 musisi lagi"
                    count == 2 -> "Pilih 1 musisi lagi"
                    else -> "Bagus! ($count musisi dipilih)"
                }
                val isTargetReached = count >= 3

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = if (isTargetReached) BrandGreenBright else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isTargetReached) FontWeight.Bold else FontWeight.Medium
                    )

                    if (count > 0) {
                        Text(
                            text = "$count Dipilih",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onComplete,
                    enabled = count >= 1 && !isCalibrating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        disabledContainerColor = SurfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isCalibrating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = Color(0xFF0C0E11),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mengalibrasi Beranda...",
                                color = Color(0xFF0C0E11),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (isTargetReached) "Selesai & Masuk Beranda" else "Lanjutkan ($count dipilih)",
                            color = if (count >= 1) Color(0xFF0C0E11) else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistCircleItem(
    name: String,
    genre: String,
    avatarUrl: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) BrandGreen else SurfaceContainerHigh,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(2).uppercase(),
                        color = BrandGreenBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Green Check Overlay when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = Color(0xFF0C0E11),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            color = if (isSelected) BrandGreenBright else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = genre,
            color = TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
