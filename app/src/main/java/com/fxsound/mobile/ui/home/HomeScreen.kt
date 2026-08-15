package com.fxsound.mobile.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.fxsound.mobile.core.presets.PresetCompare
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.core.runtime.ProcessingStatusResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fxsound.mobile.ui.components.FxFrequencyCurve
import com.fxsound.mobile.ui.components.FxLevelMeter
import com.fxsound.mobile.ui.components.FxPowerButton
import com.fxsound.mobile.ui.components.FxSlider
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxAccentCoralGlow
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxCyan
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.util.collectMappedAsState
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun HomeScreen(
    audioController: AudioEngineController,
    presetRepository: PresetRepository,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) audioController.openAudioFile(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FxBgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HomeHeader(audioController: AudioEngineController) {
    val powerEnabled by audioController.parameters.collectMappedAsState { it.powerEnabled }
    val deviceName by audioController.telemetry.collectMappedAsState { it.activeOutputDevice }
    val isBluetooth by audioController.telemetry.collectMappedAsState { it.isBluetooth }
    val lastError by audioController.telemetry.collectMappedAsState { it.lastError }

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
                tint = if (isBluetooth) FxCyan else FxAccentCoral,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = deviceName,
                color = FxTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            FxPowerButton(
                enabled = powerEnabled,
                onToggle = { enabled -> audioController.setPowerEnabled(enabled) },
                compact = true
            )
        }
        lastError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = err,
                color = FxAccentCoral,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { audioController.clearError() }
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
    val modified = presetRepository.isModified(activePreset, params)
    var comparing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val isSelected = activePreset.id == preset.id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) FxAccentCoral else FxCardSurface)
                    .border(1.dp, if (isSelected) FxAccentCoral else FxBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        comparing = false
                        presetRepository.selectPreset(preset)
                        audioController.applyPreset(preset)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isSelected && modified) "${preset.name}*" else preset.name,
                    color = if (isSelected) FxTextPrimary else FxTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(FxCardElevated)
                .border(1.dp, FxBorder, RoundedCornerShape(16.dp))
                .clickable {
                    comparing = false
                    presetRepository.selectPreset(BuiltInPresets.FLAT)
                    audioController.applyPreset(BuiltInPresets.FLAT)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.reset_to_flat),
                    tint = FxTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Flat", color = FxTextSecondary, fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (comparing) FxCyan else FxCardElevated)
                .border(1.dp, if (comparing) FxCyan else FxBorder, RoundedCornerShape(16.dp))
                .clickable {
                    comparing = !comparing
                    audioController.applyPreset(PresetCompare.target(comparing, activePreset))
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (comparing) stringResource(R.string.compare_on) else stringResource(R.string.compare_preset),
                color = FxTextPrimary,
                fontSize = 12.sp,
                fontWeight = if (comparing) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
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
        color = if (mode == ProcessingMode.PLAYER_SESSION && !sessionAttached) FxAccentCoral else FxTextSecondary,
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
    val clarity by audioController.parameters.collectMappedAsState { it.clarity }
    val bass by audioController.parameters.collectMappedAsState { it.bass }
    val ambience by audioController.parameters.collectMappedAsState { it.ambience }
    val surround by audioController.parameters.collectMappedAsState { it.surround }
    val dynamicBoost by audioController.parameters.collectMappedAsState { it.dynamicBoost }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.enhancement_effects).uppercase(),
            color = FxTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        FxSlider(label = stringResource(R.string.clarity), value = clarity, onValueChange = { audioController.setClarity(it) })
        FxSlider(label = stringResource(R.string.bass_boost), value = bass, onValueChange = { audioController.setBass(it) })
        FxSlider(label = stringResource(R.string.ambience), value = ambience, onValueChange = { audioController.setAmbience(it) })
        FxSlider(label = stringResource(R.string.surround), value = surround, onValueChange = { audioController.setSurround(it) })
        FxSlider(label = stringResource(R.string.dynamic_boost), value = dynamicBoost, onValueChange = { audioController.setDynamicBoost(it) })
    }
}

@Composable
private fun EqPreviewCard(
    audioController: AudioEngineController,
    onNavigateToEqualizer: () -> Unit
) {
    val eqEnabled by audioController.parameters.collectMappedAsState { it.eqEnabled }
    val eqGains by audioController.parameters.collectMappedAsState { it.eqGains }
    val eqFrequencies by audioController.parameters.collectMappedAsState { it.eqFrequencies }
    val eqQFactors by audioController.parameters.collectMappedAsState { it.eqQFactors }
    val curveFreqs = remember {
        FloatArray(16) { i ->
            val t = i / 15f
            10f.pow(log10(20f) + t * (log10(20000f) - log10(20f)))
        }
    }
    var curveDbs by remember { mutableStateOf(List(16) { 0f }) }
    LaunchedEffect(eqGains, eqFrequencies, eqQFactors, eqEnabled) {
        kotlinx.coroutines.delay(50)
        curveDbs = withContext(Dispatchers.Default) {
            audioController.computeMagnitudeResponse(curveFreqs)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .clickable { onNavigateToEqualizer() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = stringResource(R.string.equalizer),
                    tint = FxCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.equalizer).uppercase(),
                    color = FxTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = if (eqEnabled) stringResource(R.string.eq_active) else stringResource(R.string.eq_bypass),
                color = if (eqEnabled) FxCyan else FxTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FxFrequencyCurve(
            bandGains = eqGains,
            bandFrequencies = eqFrequencies,
            responseDbs = curveDbs,
            isInteractive = false,
            lineColor = FxAccentCoral,
            fillColor = FxAccentCoralGlow,
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
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.output).uppercase(),
                color = FxTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.output),
                tint = FxTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) FxAccentCoral else FxCardElevated)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = FxTextPrimary
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
                    maxLines = 1
                )
            }
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.open_audio),
                    tint = FxCyan
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
                    activeTrackColor = FxCyan,
                    inactiveTrackColor = FxTrackInactive
                )
            )
        }
    }
}
