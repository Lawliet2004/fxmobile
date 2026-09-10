package com.fxsound.mobile.ui.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.ui.components.FxFrequencyCurve
import com.fxsound.mobile.ui.components.FxVerticalFader
import com.fxsound.mobile.ui.theme.FxAccent
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxCurveFill
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.FxOnAccent
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.theme.reduceMotion
import com.fxsound.mobile.ui.util.UiPacking
import com.fxsound.mobile.ui.util.collectMappedAsState
import com.fxsound.mobile.ui.util.fxClickable
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
    var selectedBand by rememberSaveable { mutableIntStateOf(0) }
    var fineTuneOpen by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val curveFreqs = remember {
        FloatArray(32) { i ->
            val t = i / 31f
            10f.pow(log10(20f) + t * (log10(20000f) - log10(20f)))
        }
    }
    var curveDbs by remember { mutableStateOf(List(32) { 0f }) }

    // Stable key for LaunchedEffect: snapshot eqGains into a list whose equality
    // is content-based rather than identity-based.
    val stableGains = remember(eqGains) { eqGains.toList() }
    LaunchedEffect(stableGains, eqFrequencies, eqQFactors, eqEnabled) {
        if (selectedBand !in stableGains.indices) {
            selectedBand = stableGains.lastIndex.coerceAtLeast(0)
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
            .padding(
                start = UiPacking.SCREEN_PAD_DP.dp,
                end = UiPacking.SCREEN_PAD_DP.dp,
                top = 16.dp,
                bottom = UiPacking.settingsBottomClearanceDp().dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.eq_title),
                    color = FxTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (eqEnabled) stringResource(R.string.eq_on) else stringResource(R.string.eq_bypass),
                    color = if (eqEnabled) FxTextPrimary else FxTextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FxCardSurface)
                        .border(1.dp, FxBorder, RoundedCornerShape(12.dp))
                        .fxClickable { showResetConfirm = true }
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
                        checkedThumbColor = FxOnAccent,
                        checkedTrackColor = FxAccent,
                        uncheckedThumbColor = FxTextSecondary,
                        uncheckedTrackColor = FxCardElevated
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
            lineColor = FxTextPrimary,
            fillColor = FxCurveFill,
            showGainLabels = true,
            showBandGuides = false,
            sampleCount = 32,
            modifier = Modifier.height(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        key(eqGains, "faders") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (eqEnabled) 1f else 0.4f)
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                eqGains.forEachIndexed { index, gain ->
                    BandFader(
                        index = index,
                        gain = gain,
                        frequency = eqFrequencies.getOrElse(index) { 1000f },
                        isSelected = selectedBand == index,
                        onSelect = { selectedBand = index },
                        onGainChange = { newGain ->
                            if (eqEnabled) {
                                selectedBand = index
                                audioController.setEqBandGain(index, newGain)
                            }
                        }
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
                .fxClickable { fineTuneOpen = !fineTuneOpen }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.eq_fine_tune).uppercase(),
                color = FxTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (fineTuneOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.eq_fine_tune),
                tint = FxTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        val reduced = reduceMotion()
        AnimatedVisibility(
            visible = fineTuneOpen && selectedBand in eqGains.indices,
            enter = if (reduced) fadeIn(tween(0)) else fadeIn() + expandVertically(
                animationSpec = spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS)
            ),
            exit = if (reduced) fadeOut(tween(0)) else fadeOut() + shrinkVertically(
                animationSpec = spring(dampingRatio = FxMotion.SMOOTH_DAMPING, stiffness = FxMotion.SMOOTH_STIFFNESS)
            )
        ) {
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
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_reset_eq_title),
                    color = FxTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_reset_eq_message),
                    color = FxTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        audioController.resetAllEqBands()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccent)
                ) {
                    Text(stringResource(R.string.action_reset), color = FxTextPrimary, fontWeight = FontWeight.Bold)
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
private fun RowScope.BandFader(
    index: Int,
    gain: Float,
    frequency: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onGainChange: (Float) -> Unit
) {
    val labelColor = if (isSelected) FxTextPrimary else FxTextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .background(
                color = if (isSelected) FxTextPrimary.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .fxClickable { onSelect() }
    ) {
        FaderTickLabel(
            text = formatGainLabel(gain),
            color = labelColor,
            bold = true
        )
        Spacer(modifier = Modifier.height(2.dp))
        FxVerticalFader(
            value = gain,
            valueRange = -12f..12f,
            onValueChange = onGainChange,
            contentDescription = stringResource(
                R.string.a11y_band_slider,
                index + 1,
                formatBandHz(frequency),
                String.format(Locale.ROOT, "%.1f", gain)
            ),
            modifier = Modifier
                .height(148.dp)
                .fillMaxWidth(),
            activeColor = FxTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        FaderTickLabel(
            text = formatFaderHz(frequency),
            color = labelColor,
            bold = isSelected
        )
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
        FineTuneValueRow(
            label = "BAND ${index + 1}",
            value = stringResource(R.string.eq_reset_band),
            labelColor = FxTextPrimary,
            valueColor = FxTextPrimary,
            labelWeight = FontWeight.Bold,
            labelSizeSp = 13,
            onValueClick = onReset
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            val shown = if (frequency >= 1000f) String.format(Locale.ROOT, "%.2f kHz", frequency / 1000f)
            else String.format(Locale.ROOT, "%.0f Hz", frequency)
            FineTuneValueRow(
                label = stringResource(R.string.eq_frequency),
                value = shown
            )
            Slider(
                value = log10(frequency.coerceIn(20f, 20000f)),
                onValueChange = { onFrequency(10f.pow(it)) },
                onValueChangeFinished = { },
                valueRange = log10(20f)..log10(20000f),
                colors = SliderDefaults.colors(
                    thumbColor = FxTextPrimary,
                    activeTrackColor = FxTextPrimary,
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
        val shown = if (unit == "Hz" && value >= 1000f) String.format(Locale.ROOT, "%.2f kHz", value / 1000f)
        else String.format(Locale.ROOT, "%.2f %s", value, unit).trim()
        FineTuneValueRow(label = label, value = shown)
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = { },
            valueRange = range,
            colors = SliderDefaults.colors(
                    thumbColor = FxTextPrimary,
                    activeTrackColor = FxTextPrimary,
                    inactiveTrackColor = FxTrackInactive
            )
        )
    }
}

@Composable
private fun FaderTickLabel(
    text: String,
    color: Color,
    bold: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnDp = maxWidth.value.toInt()
        val overflow = if (UiPacking.faderLabelFits(text, columnDp)) {
            TextOverflow.Clip
        } else {
            TextOverflow.Ellipsis
        }
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = overflow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FineTuneValueRow(
    label: String,
    value: String,
    labelColor: Color = FxTextSecondary,
    valueColor: Color = FxTextPrimary,
    labelWeight: FontWeight = FontWeight.Normal,
    labelSizeSp: Int = 11,
    onValueClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiPacking.LABEL_VALUE_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = labelSizeSp.sp,
            fontWeight = labelWeight,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .then(
                    if (onValueClick != null) {
                        Modifier.fxClickable(onClick = onValueClick)
                    } else {
                        Modifier
                    }
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
