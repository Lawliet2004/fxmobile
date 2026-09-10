package com.fxsound.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fxsound.mobile.R
import com.fxsound.mobile.ui.theme.FxBorderLight
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.reduceMotion
import com.fxsound.mobile.ui.util.fxPress

@Composable
fun FxPowerButton(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val reduced = reduceMotion()
    val onAmt by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = FxMotion.floatSpec(reduced),
        label = "powerOn"
    )
    val buttonSize: Dp = if (compact) 48.dp else 72.dp
    val descriptionText = stringResource(
        if (enabled) R.string.cd_power_off else R.string.cd_power_on
    )

    Canvas(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .fxPress(interactionSource)
            .size(buttonSize)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = descriptionText
                role = Role.Switch
                toggleableState = ToggleableState(enabled)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!reduced) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(!enabled)
            }
    ) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val face = r * 0.82f
        val symbolR = face * 0.42f
        val stroke = (r * 0.075f).coerceAtLeast(2f)
        val symbol = lerp(Color(0xFF9E9E9E), Color.White, onAmt)

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(FxBorderLight, Color(0xFF161616)),
                start = Offset(c.x - r, c.y - r),
                end = Offset(c.x + r, c.y + r)
            ),
            radius = r,
            center = c
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1C1C1C), Color(0xFF0A0A0A)),
                center = Offset(c.x, c.y - face * 0.3f),
                radius = face
            ),
            radius = face,
            center = c
        )
        if (onAmt > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f + 0.10f * onAmt),
                        Color.Transparent
                    ),
                    center = c,
                    radius = face
                ),
                radius = face,
                center = c
            )
        }
        drawLine(
            color = symbol,
            start = Offset(c.x, c.y - symbolR * 1.2f),
            end = Offset(c.x, c.y + symbolR * 0.08f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawArc(
            color = symbol,
            startAngle = 300f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(c.x - symbolR, c.y - symbolR),
            size = Size(symbolR * 2f, symbolR * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
