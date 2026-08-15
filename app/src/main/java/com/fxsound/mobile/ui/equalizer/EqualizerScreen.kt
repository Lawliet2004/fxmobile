package com.fxsound.mobile.ui.equalizer

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.ui.components.FxFrequencyCurve
import com.fxsound.mobile.ui.components.FxVerticalFader
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxAccentCoralGlow
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.util.collectMappedAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun EqualizerScreen(
    audioController: AudioEngineController,
    modifier: Modifier = Modifier
) {
    val eqEnabled by audioController.parameters.collectMappedAsState { it.eqEnabled }
    val eqGains by audioController.parameters.collectMappedAsState { it.eqGains }
    val eqFrequencies by audioController.parameters.collectMappedAsState { it.eqFrequencies }
    val eqQFactors by audioController.parameters.collectMappedAsState { it.eqQFactors }
    var selectedBand by remember { mutableIntStateOf(0) }
    var fineTuneOpen by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val curveFreqs = remember {
        FloatArray(32) { i ->
            val t = i / 31f
            10f.pow(log10(20f) + t * (log10(20000f) - log10(20f)))
        }
    }
    var curveDbs by remember { mutableStateOf(List(32) { 0f }) }

    LaunchedEffect(eqGains, eqFrequencies, eqQFactors, eqEnabled) {
        if (selectedBand !in eqGains.indices) {
            selectedBand = eqGains.lastIndex.coerceAtLeast(0)
        }
        kotlinx.coroutines.delay(40)
        curveDbs = withContext(Dispatchers.Default) {
            audioController.computeMagnitudeResponse(curveFreqs)
        }
    }

    LaunchedEffect(fineTuneOpen) {
        if (fineTuneOpen) {
            kotlinx.coroutines.delay(80)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FxBgDark)
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.eq_title).uppercase(),
                    color = FxTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (eqEnabled) stringResource(R.string.eq_on) else stringResource(R.string.eq_bypass),
                    color = if (eqEnabled) FxAccentCoral else FxTextMuted,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FxCardSurface)
                        .border(1.dp, FxBorder, RoundedCornerShape(12.dp))
                        .clickable { audioController.resetAllEqBands() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, stringResource(R.string.eq_reset), tint = FxTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.eq_reset), color = FxTextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { audioController.setEqEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FxAccentCoral,
                        uncheckedThumbColor = FxTextMuted,
                        uncheckedTrackColor = FxCardSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FxFrequencyCurve(
            bandGains = eqGains,
            bandFrequencies = eqFrequencies,
            responseDbs = curveDbs,
            onBandGainChange = if (eqEnabled) { index, gainDb ->
                selectedBand = index
                audioController.setEqBandGain(index, gainDb)
            } else null,
            isInteractive = eqEnabled,
            lineColor = FxAccentCoral,
            fillColor = FxAccentCoralGlow,
            showGainLabels = true,
            showBandGuides = false,
            sampleCount = 32,
            modifier = Modifier.height(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (eqEnabled) 1f else 0.4f)
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            eqGains.forEachIndexed { index, gain ->
                val freq = eqFrequencies.getOrElse(index) { 1000f }
                val selected = selectedBand == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) FxCardSurface else Color.Transparent)
                        .clickable { selectedBand = index }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = formatGainLabel(gain),
                        color = if (selected) FxAccentCoral else FxTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FxVerticalFader(
                        value = gain,
                        valueRange = -12f..12f,
                        onValueChange = {
                            if (eqEnabled) {
                                selectedBand = index
                                audioController.setEqBandGain(index, it)
                            }
                        },
                        contentDescription = "Band ${index + 1}, ${formatBandHz(freq)}, ${String.format(Locale.ROOT, "%.1f", gain)} dB",
                        modifier = Modifier
                            .height(148.dp)
                            .fillMaxWidth(),
                        activeColor = FxAccentCoral
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFaderHz(freq),
                        color = if (selected) FxAccentCoral else FxTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FxCardSurface)
                .border(1.dp, FxBorder, RoundedCornerShape(12.dp))
                .clickable { fineTuneOpen = !fineTuneOpen }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.eq_fine_tune).uppercase(),
                color = FxTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = if (fineTuneOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.eq_fine_tune),
                tint = FxTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = fineTuneOpen && selectedBand in eqGains.indices) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                BandEditorCard(
                    index = selectedBand,
                    frequency = eqFrequencies.getOrElse(selectedBand) { 1000f },
                    gain = eqGains.getOrElse(selectedBand) { 0f },
                    q = eqQFactors.getOrElse(selectedBand) { 1.414f },
                    onFrequency = { audioController.setEqBandFrequency(selectedBand, it) },
                    onGain = { audioController.setEqBandGain(selectedBand, it) },
                    onQ = { audioController.setEqBandQ(selectedBand, it) },
                    onReset = { audioController.resetEqBand(selectedBand) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BandEditorCard(
    index: Int,
    frequency: Float,
    gain: Float,
    q: Float,
    onFrequency: (Float) -> Unit,
    onGain: (Float) -> Unit,
    onQ: (Float) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "BAND ${index + 1}",
                color = FxTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = stringResource(R.string.eq_reset_band),
                color = FxAccentCoral,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onReset() }
            )
        }
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.eq_frequency), color = FxTextSecondary, fontSize = 11.sp)
                val shown = if (frequency >= 1000f) String.format(Locale.ROOT, "%.2f kHz", frequency / 1000f)
                else String.format(Locale.ROOT, "%.0f Hz", frequency)
                Text(shown, color = FxTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = log10(frequency.coerceIn(20f, 20000f)),
                onValueChange = { onFrequency(10f.pow(it)) },
                valueRange = log10(20f)..log10(20000f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = FxAccentCoral,
                    inactiveTrackColor = FxTrackInactive
                )
            )
        }
        BandSlider(stringResource(R.string.eq_gain), gain, -12f..12f, { onGain(it) }, "dB")
        BandSlider(stringResource(R.string.eq_q), q, 0.7f..3.0f, { onQ(it) }, "")
    }
}

@Composable
private fun BandSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    unit: String
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = FxTextSecondary, fontSize = 11.sp)
            val shown = if (unit == "Hz" && value >= 1000f) String.format(Locale.ROOT, "%.2f kHz", value / 1000f)
            else String.format(Locale.ROOT, "%.2f %s", value, unit).trim()
            Text(shown, color = FxTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = FxAccentCoral,
                inactiveTrackColor = FxTrackInactive
            )
        )
    }
}

private fun formatBandHz(freq: Float): String {
    return if (freq >= 1000f) {
        val k = freq / 1000f
        if (abs(k - k.toInt()) < 0.05f) "${k.toInt()}.00 kHz"
        else String.format(Locale.ROOT, "%.2f kHz", k)
    } else {
        "${freq.toInt()} Hz"
    }
}

private fun formatFaderHz(freq: Float): String {
    return if (freq >= 1000f) {
        val k = freq / 1000f
        if (abs(k - k.toInt()) < 0.05f) "${k.toInt()}k" else String.format(Locale.ROOT, "%.1fk", k)
    } else {
        freq.toInt().toString()
    }
}

private fun formatGainLabel(gain: Float): String {
    return if (abs(gain) < 0.05f) "0"
    else if (gain > 0f) "+${String.format(Locale.ROOT, "%.0f", gain)}"
    else String.format(Locale.ROOT, "%.0f", gain)
}
