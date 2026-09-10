package com.fxsound.mobile.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.model.ProcessingMode
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetCompare
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.core.runtime.ProcessingStatusResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fxsound.mobile.ui.components.FxFrequencyCurve
import com.fxsound.mobile.ui.components.FxLevelMeter
import com.fxsound.mobile.ui.components.FxPowerButton
import com.fxsound.mobile.ui.components.FxSlider
import com.fxsound.mobile.ui.theme.FxAccent
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxCurveFill
import com.fxsound.mobile.ui.theme.FxDanger
import com.fxsound.mobile.ui.theme.FxInvert
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.FxOnInvert
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.theme.reduceMotion
import com.fxsound.mobile.ui.util.UiPacking
import com.fxsound.mobile.ui.util.collectMappedAsState
import com.fxsound.mobile.ui.util.fxClickable
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun HomeScreen(
    audioController: AudioEngineController,
    presetRepository: PresetRepository,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val lastError by audioController.telemetry.collectMappedAsState { it.lastError }
    val fileOpenedMessage = stringResource(R.string.file_opened)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            audioController.openAudioFile(uri)
            snackbarScope.launch { snackbarHostState.showSnackbar(fileOpenedMessage) }
        }
    }

    LaunchedEffect(lastError) {
        val err = lastError
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
                    top = 12.dp,
                    bottom = UiPacking.settingsBottomClearanceDp().dp
                )
        ) {
            HomeHeader(audioController)
            Spacer(modifier = Modifier.height(8.dp))
            ProcessingStatusBanner(audioController)
            Spacer(modifier = Modifier.height(12.dp))
            NowPlayingCard(
                audioController = audioController,
                onOpen = { picker.launch(arrayOf("audio/*")) },
                onToggle = { audioController.toggleMediaPlayback() },
                onSeek = { audioController.seekMedia(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PresetChipRow(audioController, presetRepository)
            Spacer(modifier = Modifier.height(12.dp))
            EffectSliders(audioController)
            Spacer(modifier = Modifier.height(12.dp))
            EqPreviewCard(audioController, onNavigateToEqualizer)
            Spacer(modifier = Modifier.height(12.dp))
            OutputSection(audioController)
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
}

private data class HeaderTelemetry(
    val deviceName: String,
    val isBluetooth: Boolean,
    val lastError: String?
)

@Composable
private fun HomeHeader(audioController: AudioEngineController) {
    // One subscription for the whole header slice. The data-class payload keeps
    // distinctUntilChanged cheap so only meaningful changes recompose this scope.
    val header by audioController.telemetry.collectMappedAsState {
        HeaderTelemetry(
            deviceName = it.activeOutputDevice,
            isBluetooth = it.isBluetooth,
            lastError = it.lastError
        )
    }
    val powerEnabled by audioController.parameters.collectMappedAsState { it.powerEnabled }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FxCardSurface)
                .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = stringResource(R.string.audio_output),
                tint = if (header.isBluetooth) FxTextSecondary else FxTextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = header.deviceName,
                color = FxTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FxPowerButton(
                enabled = powerEnabled,
                onToggle = { enabled -> audioController.setPowerEnabled(enabled) },
                compact = true
            )
        }
        header.lastError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = err,
                color = FxAccent,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fxClickable { audioController.clearError() },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PresetChipRow(
    audioController: AudioEngineController,
    presetRepository: PresetRepository
) {
    val presets by presetRepository.presets.collectMappedAsState { it }
    val activePreset by presetRepository.activePreset.collectMappedAsState { it }
    val params by audioController.parameters.collectMappedAsState { it }
    var comparing by remember { mutableStateOf(false) }
    val modified = !comparing && presetRepository.isModified(activePreset, params)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(UiPacking.CHIP_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val isSelected = activePreset.id == preset.id
            val chipLabel = if (isSelected && modified) "${preset.name}*" else preset.name
            PresetChip(
                label = chipLabel,
                selected = isSelected,
                onClick = {
                    comparing = false
                    presetRepository.selectPreset(preset)
                    audioController.applyPreset(preset)
                }
            )
        }

        PresetChip(
            label = "Flat",
            selected = false,
            onClick = {
                comparing = false
                presetRepository.selectPreset(BuiltInPresets.DEFAULT)
                audioController.applyPreset(BuiltInPresets.DEFAULT)
            },
            unselectedContainer = FxCardElevated,
            leading = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.reset_to_flat),
                    tint = FxTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        )
        PresetChip(
            label = if (comparing) stringResource(R.string.compare_on) else stringResource(R.string.compare_preset),
            selected = comparing,
            onClick = {
                comparing = !comparing
                audioController.applyPreset(PresetCompare.target(comparing, activePreset))
            },
            selectedContainer = FxTextSecondary,
            unselectedContainer = FxCardElevated,
            selectedContent = FxOnInvert
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainer: Color = FxInvert,
    selectedContent: Color = FxOnInvert,
    unselectedContainer: Color = FxCardSurface,
    leading: @Composable (() -> Unit)? = null
) {
    val reduced = reduceMotion()
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedContainer else unselectedContainer,
        animationSpec = FxMotion.colorSpec(reduced),
        label = "chipContainer"
    )
    val content by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedContent else FxTextSecondary,
        animationSpec = FxMotion.colorSpec(reduced),
        label = "chipContent"
    )
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.dp, if (selected) selectedContainer else FxBorder, RoundedCornerShape(16.dp))
            .fxClickable(onClick = onClick)
            .padding(horizontal = UiPacking.CHIP_PAD_H_DP.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = content,
            fontSize = UiPacking.CHIP_FONT_SP.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                softWrap = false,
                overflow = TextOverflow.Visible
        )
    }
}

@Composable
private fun ProcessingStatusBanner(audioController: AudioEngineController) {
    val mode by audioController.telemetry.collectMappedAsState { it.processingMode }
    val isPlaying by audioController.telemetry.collectMappedAsState { it.isPlaying }
    val sessionAttached by audioController.telemetry.collectMappedAsState { it.sessionAttached }
    val sessionLabel by audioController.telemetry.collectMappedAsState { it.sessionLabel }
    val status = ProcessingStatusResolver.resolve(mode, isPlaying, sessionAttached)
    val message = ProcessingStatusResolver.message(status, sessionLabel)
    Text(
        text = message,
        color = if (mode == ProcessingMode.PLAYER_SESSION && !sessionAttached) FxDanger else FxTextSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun EffectSliders(audioController: AudioEngineController) {
    // Card chrome lives in the parent; each child re-collects only its own DSP slice.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.enhancement_effects),
            color = FxTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        ClaritySlider(audioController)
        BassSlider(audioController)
        AmbienceSlider(audioController)
        SurroundSlider(audioController)
        DynamicBoostSlider(audioController)
    }
}

@Composable
private fun ClaritySlider(audioController: AudioEngineController) {
    val v by audioController.parameters.collectMappedAsState { it.clarity }
    FxSlider(label = stringResource(R.string.clarity), value = v, onValueChange = { audioController.setClarity(it) })
}

@Composable
private fun BassSlider(audioController: AudioEngineController) {
    val v by audioController.parameters.collectMappedAsState { it.bass }
    FxSlider(label = stringResource(R.string.bass_boost), value = v, onValueChange = { audioController.setBass(it) })
}

@Composable
private fun AmbienceSlider(audioController: AudioEngineController) {
    val v by audioController.parameters.collectMappedAsState { it.ambience }
    FxSlider(label = stringResource(R.string.ambience), value = v, onValueChange = { audioController.setAmbience(it) })
}

@Composable
private fun SurroundSlider(audioController: AudioEngineController) {
    val v by audioController.parameters.collectMappedAsState { it.surround }
    FxSlider(label = stringResource(R.string.surround), value = v, onValueChange = { audioController.setSurround(it) })
}

@Composable
private fun DynamicBoostSlider(audioController: AudioEngineController) {
    val v by audioController.parameters.collectMappedAsState { it.dynamicBoost }
    FxSlider(label = stringResource(R.string.dynamic_boost), value = v, onValueChange = { audioController.setDynamicBoost(it) })
}

@Composable
private fun EqPreviewCard(
    audioController: AudioEngineController,
    onNavigateToEqualizer: () -> Unit
) {
    val eqEnabled by audioController.parameters.collectMappedAsState { it.eqEnabled }
    val eqGains by audioController.parameters.collectMappedAsState { it.eqGains }
    val eqFrequencies by audioController.parameters.collectMappedAsState { it.eqFrequencies }
    val curveFreqs = remember {
        FloatArray(16) { i ->
            val t = i / 15f
            10f.pow(log10(20f) + t * (log10(20000f) - log10(20f)))
        }
    }
    var curveDbs by remember { mutableStateOf(List(16) { 0f }) }
    LaunchedEffect(eqGains, eqFrequencies, eqEnabled) {
        kotlinx.coroutines.delay(50)
        curveDbs = withContext(Dispatchers.Default) {
            audioController.computeMagnitudeResponse(curveFreqs)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
            .fxClickable(onClick = onNavigateToEqualizer)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = stringResource(R.string.equalizer),
                    tint = FxTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.equalizer),
                color = FxTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (eqEnabled) stringResource(R.string.eq_active) else stringResource(R.string.eq_bypass),
                color = if (eqEnabled) FxTextSecondary else FxTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FxFrequencyCurve(
            bandGains = eqGains,
            bandFrequencies = eqFrequencies,
            responseDbs = curveDbs,
            isInteractive = false,
            lineColor = FxTextPrimary,
            fillColor = FxCurveFill,
            showBandGuides = false,
            sampleCount = 16,
            modifier = Modifier.height(88.dp)
        )
    }
}

@Composable
private fun OutputSection(audioController: AudioEngineController) {
    var expanded by remember { mutableStateOf(false) }
    val masterGain by audioController.parameters.collectMappedAsState { it.masterGainDb }
    val balance by audioController.parameters.collectMappedAsState { it.balance }
    val leveling by audioController.parameters.collectMappedAsState { it.volumeLeveling }
    val isPlaying by audioController.telemetry.collectMappedAsState { it.isPlaying }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fxClickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.output),
                color = FxTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.output),
                tint = FxTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        val reduced = reduceMotion()
        AnimatedVisibility(
            visible = expanded,
            enter = if (reduced) fadeIn(tween(0)) else fadeIn() + expandVertically(
                animationSpec = spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS)
            ),
            exit = if (reduced) fadeOut(tween(0)) else fadeOut() + shrinkVertically(
                animationSpec = spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                FxSlider(
                    label = stringResource(R.string.master_gain),
                    value = masterGain,
                    valueRange = -20.0f..20.0f,
                    onValueChange = { audioController.setMasterGainDb(it) },
                    unit = "dB"
                )
                FxSlider(
                    label = stringResource(R.string.balance),
                    value = balance,
                    valueRange = -20.0f..20.0f,
                    onValueChange = { audioController.setBalance(it) },
                    unit = "dB"
                )
                FxSlider(
                    label = stringResource(R.string.volume_leveling),
                    value = leveling,
                    valueRange = 0.0f..4.0f,
                    onValueChange = { audioController.setVolumeLeveling(it) }
                )
                if (isPlaying) {
                    LiveMeters(audioController)
                }
            }
        }
    }
}

@Composable
private fun LiveMeters(audioController: AudioEngineController) {
    val peakLeft by audioController.telemetry.collectMappedAsState { it.peakLeft }
    val peakRight by audioController.telemetry.collectMappedAsState { it.peakRight }
    val isClipping by audioController.telemetry.collectMappedAsState { it.isClipping }
    DisposableEffect(Unit) {
        audioController.setMetersVisible(true)
        onDispose { audioController.setMetersVisible(false) }
    }
    FxLevelMeter(
        peakLeft = peakLeft,
        peakRight = peakRight,
        isClipping = isClipping
    )
}

@Composable
private fun NowPlayingCard(
    audioController: AudioEngineController,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val title by audioController.telemetry.collectMappedAsState { it.mediaTitle }
    val isPlaying by audioController.telemetry.collectMappedAsState { it.isPlaying && it.sourceType == 4 }
    val positionMs by audioController.telemetry.collectMappedAsState { it.mediaPositionMs }
    val durationMs by audioController.telemetry.collectMappedAsState { it.mediaDurationMs }
    val canToggle = title != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggle,
                enabled = canToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !canToggle -> FxCardElevated
                            isPlaying -> FxInvert
                            else -> FxCardElevated
                        }
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = if (isPlaying) FxOnInvert else if (canToggle) FxTextPrimary else FxTextMuted
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing),
                    color = FxTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title ?: stringResource(R.string.no_file),
                    color = FxTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.open_audio),
                    tint = FxTextSecondary
                )
            }
        }
        if (durationMs > 0L) {
            Slider(
                value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = FxTextPrimary,
                    activeTrackColor = FxTextPrimary,
                    inactiveTrackColor = FxTrackInactive
                )
            )
        }
    }
}
