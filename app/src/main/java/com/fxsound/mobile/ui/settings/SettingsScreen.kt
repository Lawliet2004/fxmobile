package com.fxsound.mobile.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxCyan
import com.fxsound.mobile.ui.theme.FxGreen
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary

@Composable
fun SettingsScreen(
    audioController: AudioEngineController,
    presetRepository: PresetRepository,
    selected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val telemetry by audioController.telemetry.collectAsState()
    var selectedSourceType by remember { mutableIntStateOf(0) }

    DisposableEffect(selected, telemetry.isPlaying, telemetry.sourceType) {
        val metersOn = selected && telemetry.isPlaying && telemetry.sourceType != 4
        audioController.setSettingsVisible(metersOn)
        onDispose { audioController.setSettingsVisible(false) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FxBgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings).uppercase(),
            color = FxTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(title = "PROCESSING MODE", icon = Icons.Default.Speed, iconTint = FxCyan) {
            ModeChip(
                label = stringResource(R.string.mode_internal),
                selected = telemetry.processingMode == ProcessingMode.INTERNAL_ENGINE,
                onClick = { audioController.setProcessingMode(ProcessingMode.INTERNAL_ENGINE) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModeChip(
                label = stringResource(R.string.mode_session),
                selected = telemetry.processingMode == ProcessingMode.PLAYER_SESSION,
                onClick = { audioController.setProcessingMode(ProcessingMode.PLAYER_SESSION) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.mode_session_hint),
                color = FxTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            if (telemetry.sessionAttached) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Attached: ${telemetry.sessionLabel}", color = FxGreen, fontSize = 12.sp)
            }
            telemetry.lastError?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = FxAccentCoral, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(title = "REAL-TIME AUDIO ENGINE", icon = Icons.Default.Speed, iconTint = FxCyan) {
            TelemetryRow("Audio Backend", telemetry.audioBackend)
            TelemetryRow("Native Sample Rate", "${telemetry.sampleRate} Hz")
            TelemetryRow("Channels", "${telemetry.channelCount} (Stereo PCM Float32)")
            TelemetryRow("Buffer Size", "${telemetry.bufferSizeFrames} frames (${telemetry.burstFrames} burst)")
            TelemetryRow(
                "Estimated Latency",
                "${String.format("%.2f", telemetry.estimatedLatencyMs)} ms",
                highlight = telemetry.estimatedLatencyMs < 10.0f
            )
            TelemetryRow("DSP CPU Load", "${String.format("%.2f", telemetry.dspCpuLoadPercent)}%", highlight = true)
            TelemetryRow("Active Output Device", telemetry.activeOutputDevice)
            TelemetryRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(title = stringResource(R.string.test_generator).uppercase(), icon = Icons.Default.PlayArrow, iconTint = FxAccentCoral) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        if (telemetry.isPlaying && telemetry.sourceType != 4) {
                            audioController.stopPlayback()
                        } else {
                            audioController.startPlayback(selectedSourceType, 440.0f)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (telemetry.isPlaying && telemetry.sourceType != 4) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = FxTextPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0 to stringResource(R.string.test_sine),
                        1 to stringResource(R.string.test_sweep),
                        2 to stringResource(R.string.test_noise)
                    ).forEach { (idx, name) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedSourceType == idx) FxAccentCoral else FxCardElevated)
                                .clickable {
                                    selectedSourceType = idx
                                    audioController.setSourceType(idx, 440.0f)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(name, color = FxTextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                presetRepository.selectPreset(BuiltInPresets.FLAT)
                audioController.applyPreset(BuiltInPresets.FLAT)
            },
            colors = ButtonDefaults.buttonColors(containerColor = FxCardElevated, contentColor = FxTextPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.reset_settings))
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(title = stringResource(R.string.privacy).uppercase(), icon = Icons.Default.Lock, iconTint = FxGreen) {
            Text(
                text = "All enhancement runs locally in the Rust DSP engine. No account, no cloud, no analytics, no microphone, and no internet permission.",
                color = FxTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(title = stringResource(R.string.about).uppercase(), icon = Icons.Default.Info, iconTint = FxAccentCoral) {
            Text(
                text = "FxMobile is a lightweight Android audio enhancer inspired by open-source FxSound (AGPL v3.0).\n\n" +
                    "Android cannot apply this engine to every other app. Use Internal engine for files you open here. Player session mode attaches limited platform effects when a player publishes a session ID.\n\n" +
                    stringResource(R.string.licenses) + ":\n" +
                    "• Google Oboe (Apache 2.0)\n" +
                    "• RBJ Audio EQ Cookbook (Public Domain)\n" +
                    "• Android Jetpack Compose (Apache 2.0)",
                color = FxTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.padding(end = 8.dp))
            Text(title, color = FxTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FxAccentCoral else FxCardElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = FxTextPrimary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = FxTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = if (highlight) FxCyan else FxTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
