package com.fxsound.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fxsound.mobile.R
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxCurveFill
import com.fxsound.mobile.ui.theme.FxTextPrimary
import androidx.compose.ui.res.stringResource
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun FxFrequencyCurve(
    bandGains: List<Float>,
    bandFrequencies: List<Float>,
    responseDbs: List<Float> = emptyList(),
    onBandGainChange: ((Int, Float) -> Unit)? = null,
    isInteractive: Boolean = true,
    lineColor: Color = FxTextPrimary,
    fillColor: Color = FxCurveFill,
    showGainLabels: Boolean = false,
    showBandGuides: Boolean = false,
    sampleCount: Int = 32,
    modifier: Modifier = Modifier
) {
    val minFreq = 20.0f
    val maxFreq = 20000.0f
    val minDb = -12.0f
    val maxDb = 12.0f

    val fillPath = remember { Path() }
    val strokePath = remember { Path() }
    val xs = remember { FloatArray(64) }
    val ys = remember { FloatArray(64) }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f) }
    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 11.dp.toPx() }
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(230, 255, 255, 255)
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = labelTextSizePx
            isAntiAlias = true
            isFakeBoldText = true
            isSubpixelText = true
            isLinearText = true
        }
    }

    val latestOnGain by rememberUpdatedState(onBandGainChange)

    // Hold selected band across tab/drag cycles (move out of pointerInput so it survives).
    var selectedBandIndex by remember { mutableIntStateOf(0) }

    // Precompute frequency response whenever the band parameters change.
    // We always use the precomputed list — there is no per-frame fallback.
    val samplePoints = remember(sampleCount) {
        FloatArray(sampleCount.coerceIn(8, 64)) { i ->
            val t = i / (sampleCount.coerceAtLeast(2) - 1f)
            10f.pow(log10(minFreq) + t * (log10(maxFreq) - log10(minFreq)))
        }
    }
    var computedDbs by remember { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(samplePoints.size, bandGains, bandFrequencies) {
        if (samplePoints.isEmpty() || bandFrequencies.isEmpty()) {
            computedDbs = List(samplePoints.size) { 0f }
            return@LaunchedEffect
        }
        val freqs = bandFrequencies
        val gains = bandGains
        val newDbs = FloatArray(samplePoints.size) { i ->
            val testFreq = samplePoints[i]
            var approx = 0.0f
            freqs.forEachIndexed { index, bFreq ->
                val gain = if (index < gains.size) gains[index] else 0.0f
                val octaveDist = kotlin.math.abs(log10(testFreq) - log10(bFreq.coerceAtLeast(1f)))
                val weight = kotlin.math.exp(-3.5f * octaveDist * octaveDist)
                approx += gain * weight
            }
            approx.coerceIn(minDb, maxDb)
        }
        computedDbs = newDbs.toList()
    }
    val dbs = if (responseDbs.size >= samplePoints.size) responseDbs else computedDbs

    val curveDescription = stringResource(R.string.cd_eq_curve)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FxCardSurface)
            .border(1.dp, FxBorder, RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) { contentDescription = curveDescription }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInteractive && onBandGainChange != null) {
                        Modifier.pointerInput(bandFrequencies) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    val width = size.width.toFloat()
                                    val tappedLogFreq = (start.x / width) * (log10(maxFreq) - log10(minFreq)) + log10(minFreq)
                                    val tappedFreq = 10f.pow(tappedLogFreq)
                                    var closestIndex = 0
                                    var minDistance = Float.MAX_VALUE
                                    bandFrequencies.forEachIndexed { index, freq ->
                                        val dist = kotlin.math.abs(log10(freq.coerceAtLeast(1f)) - log10(tappedFreq))
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIndex = index
                                        }
                                    }
                                    selectedBandIndex = closestIndex
                                },
                                onDrag = { change, _ ->
                                    val y = change.position.y
                                    val height = size.height.toFloat()
                                    if (height > 0) {
                                        val clampedY = y.coerceIn(0f, height)
                                        val gainDb = maxDb - (clampedY / height) * (maxDb - minDb)
                                        latestOnGain?.invoke(selectedBandIndex, gainDb.coerceIn(-12f, 12f))
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            val width = size.width
            val height = size.height
            val zeroY = height * 0.5f
            val gridColor = FxBorder.copy(alpha = 0.5f)

            drawLine(FxBorder, Offset(0f, zeroY), Offset(width, zeroY), strokeWidth = 1.5f)
            val plus6Y = height * (maxDb - 6.0f) / (maxDb - minDb)
            val minus6Y = height * (maxDb - (-6.0f)) / (maxDb - minDb)
            drawLine(gridColor, Offset(0f, plus6Y), Offset(width, plus6Y), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, minus6Y), Offset(width, minus6Y), strokeWidth = 1f)

            if (showBandGuides && bandFrequencies.isNotEmpty()) {
                bandFrequencies.forEach { freq ->
                    val f = freq.coerceIn(minFreq, maxFreq)
                    val normX = (log10(f) - log10(minFreq)) / (log10(maxFreq) - log10(minFreq))
                    drawLine(
                        color = gridColor,
                        start = Offset(normX * width, 0f),
                        end = Offset(normX * width, height),
                        strokeWidth = 1.2f,
                        pathEffect = dash
                    )
                }
            } else {
                listOf(100f, 1000f, 10000f).forEach { freq ->
                    val normX = (log10(freq) - log10(minFreq)) / (log10(maxFreq) - log10(minFreq))
                    drawLine(
                        gridColor,
                        Offset(normX * width, 0f),
                        Offset(normX * width, height),
                        strokeWidth = 1f
                    )
                }
            }

            val n = dbs.size.coerceAtLeast(0).coerceAtMost(xs.size)
            val last = (n - 1).coerceAtLeast(1)
            for (i in 0 until n) {
                xs[i] = (i.toFloat() / last) * width
                ys[i] = height * (maxDb - dbs[i].coerceIn(minDb, maxDb)) / (maxDb - minDb)
            }

            if (n >= 2) {
                fillPath.reset()
                strokePath.reset()
                fillPath.moveTo(xs[0], ys[0])
                strokePath.moveTo(xs[0], ys[0])
                for (i in 1 until n) {
                    fillPath.lineTo(xs[i], ys[i])
                    strokePath.lineTo(xs[i], ys[i])
                }
                fillPath.lineTo(width, height)
                fillPath.lineTo(0f, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillColor, Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (isInteractive) {
                bandFrequencies.forEachIndexed { index, bFreq ->
                    val gain = if (index < bandGains.size) bandGains[index] else 0.0f
                    val normX = (log10(bFreq.coerceIn(minFreq, maxFreq)) - log10(minFreq)) / (log10(maxFreq) - log10(minFreq))
                    val nx = normX * width
                    val ny = height * (maxDb - gain) / (maxDb - minDb)
                    drawCircle(color = FxCardSurface, radius = 7.dp.toPx(), center = Offset(nx, ny))
                    drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(nx, ny))
                    if (showGainLabels) {
                        val label = if (kotlin.math.abs(gain) < 0.05f) "0"
                        else if (gain > 0f) "+${gain.toInt()}"
                        else "${gain.toInt()}"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            nx,
                            (ny - 14.dp.toPx()).coerceAtLeast(22f),
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}
