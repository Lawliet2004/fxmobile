package com.fxsound.mobile.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import java.util.Locale

@Composable
fun FxVerticalFader(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    activeColor: Color = FxTextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val describedContentDescription = contentDescription

    fun yToValue(y: Float, height: Float): Float {
        val t = (1f - (y / height).coerceIn(0f, 1f))
        return valueRange.start + t * (valueRange.endInclusive - valueRange.start)
    }

    val span = valueRange.endInclusive - valueRange.start
    val step = (span / 100f).coerceAtLeast(0.1f)
    val stateLabel = String.format(Locale.ROOT, "%.1f dB", value)
    val rangeInfo = ProgressBarRangeInfo(
        current = value,
        range = valueRange,
        steps = (span / step).toInt().coerceAtLeast(0)
    )

    val density = LocalDensity.current
    val trackWidthPx = with(density) { 6.dp.toPx() }
    val radiusThumbPx = with(density) { 8.dp.toPx() }
    val cornerRadiusPx = with(density) { 3.dp.toPx() }

    Box(
        modifier = modifier
            .focusable(interactionSource = interactionSource)
            .semantics(mergeDescendants = true) {
                this.contentDescription = describedContentDescription
                this.stateDescription = stateLabel
            }
            .semantics {
                this.progressBarRangeInfo = rangeInfo
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val newValue = when (event.key) {
                    Key.DirectionUp, Key.DirectionRight -> (value + step).coerceAtMost(valueRange.endInclusive)
                    Key.DirectionDown, Key.DirectionLeft -> (value - step).coerceAtLeast(valueRange.start)
                    Key.PageUp -> (value + step * 10f).coerceAtMost(valueRange.endInclusive)
                    Key.PageDown -> (value - step * 10f).coerceAtLeast(valueRange.start)
                    Key.Home -> valueRange.start
                    Key.MoveEnd -> valueRange.endInclusive
                    else -> return@onKeyEvent false
                }
                if (newValue != value) {
                    onValueChange(newValue)
                    true
                } else {
                    false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        onValueChange(yToValue(offset.y, size.height.toFloat()))
                    }
                }
                .pointerInput(valueRange) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValueChange(yToValue(change.position.y, size.height.toFloat()))
                    }
                }
                .drawWithCache {
                    val spanLocal = valueRange.endInclusive - valueRange.start
                    val cx = size.width / 2f
                    val trackLeft = cx - trackWidthPx / 2f
                    val t = if (spanLocal == 0f) 0.5f
                    else ((value - valueRange.start) / spanLocal).coerceIn(0f, 1f)
                    val y = size.height * (1f - t)
                    val fillHeight = size.height - y
                    val radius = minOf(radiusThumbPx, (size.width / 2f - 1f).coerceAtLeast(0f))
                    val thumbY = y.coerceIn(radius, (size.height - radius).coerceAtLeast(radius))
                    onDrawBehind {
                        drawRoundRect(
                            color = FxTrackInactive,
                            topLeft = Offset(trackLeft, 0f),
                            size = Size(trackWidthPx, size.height),
                            cornerRadius = CornerRadius(cornerRadiusPx)
                        )
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(trackLeft, y),
                            size = Size(trackWidthPx, fillHeight),
                            cornerRadius = CornerRadius(cornerRadiusPx)
                        )
                        drawCircle(
                            color = FxTextPrimary,
                            radius = radius,
                            center = Offset(cx, thumbY)
                        )
                        drawCircle(
                            color = FxBgDark,
                            radius = radius * 0.35f,
                            center = Offset(cx, thumbY)
                        )
                    }
                }
        )
    }
}
