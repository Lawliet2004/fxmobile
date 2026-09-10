package com.fxsound.mobile.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.equalizer.EqualizerScreen
import com.fxsound.mobile.ui.home.HomeScreen
import com.fxsound.mobile.ui.presets.PresetsScreen
import com.fxsound.mobile.ui.settings.SettingsScreen
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxInvert
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.FxOnInvert
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.reduceMotion

const val TAB_TRANSITION_MS = FxMotion.TAB_MS

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
    val haptic = LocalHapticFeedback.current
    val reduced = reduceMotion()

    fun show(screen: FxScreen) {
        if (screen != currentScreen && !reduced) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        currentScreen = screen
    }

    val fadeSpec = if (reduced) {
        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    } else {
        fadeIn(spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS)) togetherWith
            fadeOut(spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS))
    }

    Box(modifier = Modifier.fillMaxSize().background(FxBgDark)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeSpec },
                label = "tabTransition"
            ) { screen ->
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
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
                            selected = true
                        )
                    }
                }
            }
        }

        NavigationBar(
            containerColor = FxCardElevated,
            tonalElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(64.dp)
                .border(1.dp, FxBorder, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
        ) {
            FxScreen.values().forEach { screen ->
                val isSelected = currentScreen == screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { show(screen) },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = null,
                            tint = if (isSelected) FxOnInvert else FxTextMuted
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            color = if (isSelected) FxTextPrimary else FxTextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = FxInvert,
                        selectedIconColor = FxOnInvert,
                        unselectedIconColor = FxTextMuted,
                        selectedTextColor = FxTextPrimary,
                        unselectedTextColor = FxTextMuted
                    )
                )
            }
        }
    }
}
