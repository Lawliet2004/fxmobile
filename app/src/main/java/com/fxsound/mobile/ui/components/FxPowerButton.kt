package com.fxsound.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardElevated
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxTextMuted

@Composable
fun FxPowerButton(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val ringColor by animateColorAsState(
        targetValue = if (enabled) FxAccentCoral else FxBorder,
        animationSpec = tween(durationMillis = 200),
        label = "ringColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (enabled) Color.White else FxTextMuted,
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )
    val size: Dp = if (compact) 40.dp else 64.dp
    val iconSize: Dp = if (compact) 22.dp else 30.dp
    val borderWidth: Dp = if (compact) 2.dp else 3.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) FxCardElevated else FxCardSurface)
            .border(width = borderWidth, color = ringColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onToggle(!enabled)
            }
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (enabled) "Turn Master Power OFF" else "Turn Master Power ON",
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
