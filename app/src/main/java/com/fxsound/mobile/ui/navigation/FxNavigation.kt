package com.fxsound.mobile.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.equalizer.EqualizerScreen
import com.fxsound.mobile.ui.home.HomeScreen
import com.fxsound.mobile.ui.presets.PresetsScreen
import com.fxsound.mobile.ui.settings.SettingsScreen
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBgDarker
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary

const val TAB_TRANSITION_MS = 140

enum class FxScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    EQUALIZER("EQ", Icons.Default.Equalizer),
    PRESETS("Presets", Icons.Default.LibraryMusic),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun FxAppScaffold(
    audioController: AudioEngineController,
    presetRepository: PresetRepository
) {
    var currentScreen by remember { mutableStateOf(FxScreen.HOME) }
    var visited by remember { mutableStateOf(setOf(FxScreen.HOME)) }

    fun show(screen: FxScreen) {
        currentScreen = screen
        visited = visited + screen
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = FxBgDarker,
                tonalElevation = 0.dp
            ) {
                FxScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { show(screen) },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) FxAccentCoral else FxTextMuted
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (isSelected) FxTextPrimary else FxTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = FxBorder
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FxBgDark)
                .padding(innerPadding)
        ) {
            val density = LocalDensity.current
            val slidePx = with(density) { 32.dp.toPx() }
            visited.forEach { screen ->
                val selected = currentScreen == screen
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(TAB_TRANSITION_MS),
                    label = "tabAlpha${screen.name}"
                )
                val offset by animateFloatAsState(
                    targetValue = when {
                        selected -> 0f
                        screen.ordinal < currentScreen.ordinal -> -slidePx
                        else -> slidePx
                    },
                    animationSpec = tween(TAB_TRANSITION_MS),
                    label = "tabSlide${screen.name}"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (selected) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                            translationX = offset
                        }
                ) {
                    when (screen) {
                        FxScreen.HOME -> HomeScreen(
                            audioController = audioController,
                            presetRepository = presetRepository,
                            onNavigateToEqualizer = { show(FxScreen.EQUALIZER) }
                        )
                        FxScreen.EQUALIZER -> EqualizerScreen(
                            audioController = audioController
                        )
                        FxScreen.PRESETS -> PresetsScreen(
                            audioController = audioController,
                            presetRepository = presetRepository
                        )
                        FxScreen.SETTINGS -> SettingsScreen(
                            audioController = audioController,
                            presetRepository = presetRepository,
                            selected = selected
                        )
                    }
                }
            }
        }
    }
}
