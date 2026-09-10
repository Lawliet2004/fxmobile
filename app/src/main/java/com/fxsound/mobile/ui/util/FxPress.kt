package com.fxsound.mobile.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.reduceMotion

fun Modifier.fxPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier = composed {
    val reduced = reduceMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) FxMotion.PRESS_SCALE else 1f,
        animationSpec = FxMotion.floatSpec(reduced),
        label = "fxPress"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.fxClickable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    heavy: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val reduced = reduceMotion()
    fxPress(interactionSource, enabled)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) {
            if (haptic && !reduced) {
                hapticFeedback.performHapticFeedback(
                    if (heavy) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                )
            }
            onClick()
        }
}
