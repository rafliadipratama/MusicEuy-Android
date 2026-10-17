package com.rafliadipratama.musiceuy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafliadipratama.musiceuy.ui.screens.HomeScreen
import com.rafliadipratama.musiceuy.ui.screens.PlayerScreen
import com.rafliadipratama.musiceuy.ui.theme.MusicEuyTheme
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicEuyTheme {
                val viewModel: MusicPlayerViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                BackHandler(enabled = uiState.isPlayerExpanded) {
                    viewModel.collapsePlayer()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    HomeScreen(viewModel = viewModel)

                    AnimatedVisibility(
                        visible = uiState.isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        PlayerScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
