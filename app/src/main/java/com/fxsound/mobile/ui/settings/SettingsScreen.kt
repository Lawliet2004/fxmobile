package com.fxsound.mobile.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.theme.FxAccent
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxBorderLight
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxGreen
import com.fxsound.mobile.ui.theme.FxInvert
import com.fxsound.mobile.ui.theme.FxOnAccent
import com.fxsound.mobile.ui.theme.FxOnInvert
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.NumericTextStyle
import com.fxsound.mobile.ui.util.UiPacking
import com.fxsound.mobile.ui.util.collectMappedAsState
import com.fxsound.mobile.ui.util.fxClickable
import java.util.Locale

// Only the telemetry fields Settings actually displays. Excludes
// mediaPositionMs / meters so playback progress ticks don't recompose
// the whole screen.
private data class SettingsTelemetry(
    val processingMode: ProcessingMode,
    val sessionAttached: Boolean,
    val sessionLabel: String,
    val lastError: String?,
    val isPlaying: Boolean,
    val sourceType: Int,
    val audioBackend: String,
    val sampleRate: Int,
    val channelCount: Int,
    val bufferSizeFrames: Int,
    val burstFrames: Int,
    val estimatedLatencyMs: Float,
    val dspCpuLoadPercent: Float,
    val activeOutputDevice: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    audioController: AudioEngineController,
    presetRepository: PresetRepository,
    selected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val telemetry by audioController.telemetry.collectMappedAsState {
        SettingsTelemetry(
            processingMode = it.processingMode,
            sessionAttached = it.sessionAttached,
            sessionLabel = it.sessionLabel,
            lastError = it.lastError,
            isPlaying = it.isPlaying,
            sourceType = it.sourceType,
            audioBackend = it.audioBackend,
            sampleRate = it.sampleRate,
            channelCount = it.channelCount,
            bufferSizeFrames = it.bufferSizeFrames,
            burstFrames = it.burstFrames,
            estimatedLatencyMs = it.estimatedLatencyMs,
            dspCpuLoadPercent = it.dspCpuLoadPercent,
            activeOutputDevice = it.activeOutputDevice
        )
    }
    var selectedSourceType by remember { mutableIntStateOf(0) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomClearance = UiPacking.settingsBottomClearanceDp().dp

    DisposableEffect(selected, telemetry.isPlaying, telemetry.sourceType) {
        val metersOn = selected && telemetry.isPlaying && telemetry.sourceType != 4
        audioController.setSettingsVisible(metersOn)
        onDispose { audioController.setSettingsVisible(false) }
    }

    LaunchedEffect(telemetry.lastError) {
        val err = telemetry.lastError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            audioController.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(FxBgDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FxBgDark)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = UiPacking.SCREEN_PAD_DP.dp,
                    end = UiPacking.SCREEN_PAD_DP.dp,
                    top = 16.dp,
                    bottom = bottomClearance
                )
    ) {
        Text(
            text = stringResource(R.string.settings),
            color = FxTextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSection(title = "PROCESSING MODE") {
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
                Text(it, color = FxAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSection(title = "REAL-TIME AUDIO ENGINE") {
            TelemetryRow("Audio Backend", telemetry.audioBackend)
            TelemetryRow("Native Sample Rate", "${telemetry.sampleRate} Hz")
            TelemetryRow("Channels", "${telemetry.channelCount} (Stereo PCM Float32)")
            TelemetryRow("Buffer Size", "${telemetry.bufferSizeFrames} frames (${telemetry.burstFrames} burst)")
            TelemetryRow(
                "Estimated Latency",
                "${String.format(Locale.ROOT, "%.2f", telemetry.estimatedLatencyMs)} ms",
                highlight = telemetry.estimatedLatencyMs < 10.0f
            )
            TelemetryRow("DSP CPU Load", "${String.format(Locale.ROOT, "%.2f", telemetry.dspCpuLoadPercent)}%", highlight = true)
            TelemetryRow("Active Output Device", telemetry.activeOutputDevice)
            TelemetryRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSection(title = stringResource(R.string.test_generator).uppercase()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (telemetry.isPlaying && telemetry.sourceType != 4) {
                            audioController.stopPlayback()
                        } else {
                            audioController.startPlayback(selectedSourceType, 440.0f)
                        }
                    },
                    modifier = Modifier.size(UiPacking.PLAY_CONTROL_DP.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.isPlaying && telemetry.sourceType != 4) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = FxTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(UiPacking.TEST_CHIP_GAP_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(UiPacking.TEST_CHIP_GAP_DP.dp)
                ) {
                    listOf(
                        0 to stringResource(R.string.test_sine),
                        1 to stringResource(R.string.test_sweep),
                        2 to stringResource(R.string.test_noise)
                    ).forEach { (idx, name) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedSourceType == idx) FxInvert else FxCardElevated)
                                .fxClickable {
                                    selectedSourceType = idx
                                    audioController.setSourceType(idx, 440.0f)
                                }
                                .padding(
                                    horizontal = UiPacking.TEST_CHIP_PAD_H_DP.dp,
                                    vertical = 7.dp
                                )
                        ) {
                            Text(
                                text = name,
                                color = if (selectedSourceType == idx) FxOnInvert else FxTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (selectedSourceType == idx) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { showResetConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = FxAccent, contentColor = FxOnAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.reset_settings), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSection(title = stringResource(R.string.privacy).uppercase()) {
            Text(
                text = "All enhancement runs locally in the Rust DSP engine. No account, no cloud, no analytics, no microphone, and no internet permission.",
                color = FxTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSection(title = stringResource(R.string.about).uppercase()) {
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
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = FxCardSurface,
                contentColor = FxTextPrimary
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_reset_flat_title),
                    color = FxTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_reset_flat_message),
                    color = FxTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        presetRepository.selectPreset(BuiltInPresets.DEFAULT)
                        audioController.applyPreset(BuiltInPresets.DEFAULT)
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccent)
                ) {
                    Text(stringResource(R.string.action_reset), color = FxOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = FxTextSecondary)
                }
            },
            containerColor = FxCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            color = FxTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FxCardSurface)
                .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // iOS row-tap style: full-width row, tinted when selected, chevron-free.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FxInvert else FxCardElevated)
            .fxClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) FxOnInvert else FxBorderLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(FxOnInvert)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (selected) FxOnInvert else FxTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, highlight: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(0.5.dp))
            HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp,
            color = FxBorder.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(0.5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = FxTextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(UiPacking.LABEL_VALUE_GAP_DP.dp))
            Text(
                text = value,
                color = if (highlight) FxTextPrimary else FxTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = NumericTextStyle.fontFamily,
                maxLines = 1,
                textAlign = TextAlign.End,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
