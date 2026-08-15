package com.fxsound.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fxsound.mobile.ui.theme.FxCyan
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTrackInactive

@Composable
fun FxVerticalFader(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    activeColor: Color = FxCyan
) {
    fun yToValue(y: Float, height: Float): Float {
        val t = (1f - (y / height).coerceIn(0f, 1f))
        return valueRange.start + t * (valueRange.endInclusive - valueRange.start)
    }

    Box(
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Canvas(
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
        ) {
            val trackWidth = 6.dp.toPx()
            val cx = size.width / 2f
            val trackLeft = cx - trackWidth / 2f
            drawRoundRect(
                color = FxTrackInactive,
                topLeft = Offset(trackLeft, 0f),
                size = Size(trackWidth, size.height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            val span = valueRange.endInclusive - valueRange.start
            val t = if (span == 0f) 0.5f else ((value - valueRange.start) / span).coerceIn(0f, 1f)
            val y = size.height * (1f - t)
            val fillHeight = size.height - y
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(trackLeft, y),
                size = Size(trackWidth, fillHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawCircle(
                color = FxTextPrimary,
                radius = 8.dp.toPx(),
                center = Offset(cx, y)
            )
        }
    }
}
