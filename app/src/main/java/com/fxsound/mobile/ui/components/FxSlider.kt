package com.fxsound.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.fxsound.mobile.ui.theme.FxMotion
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.theme.NumericTextStyle
import com.fxsound.mobile.ui.theme.reduceMotion
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FxSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0.0f..10.0f,
    onValueChange: (Float) -> Unit,
    defaultValue: Float = 0.0f,
    unit: String = "",
    accentColor: androidx.compose.ui.graphics.Color = FxTextPrimary,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val reduced = reduceMotion()

    val formattedValue = when {
        unit == "dB" -> {
            val sign = if (value > 0.05f) "+" else ""
            String.format(Locale.ROOT, "%s%.1f %s", sign, value, unit)
        }
        else -> String.format(Locale.ROOT, "%.1f", value)
    }

    val modified = value > defaultValue + 0.01f || value < defaultValue - 0.01f
    val valueColor by animateColorAsState(
        targetValue = if (modified) accentColor else FxTextSecondary,
        animationSpec = FxMotion.colorSpec(reduced),
        label = "valueColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(Locale.ROOT),
                color = FxTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onDoubleClick = { onValueChange(defaultValue) },
                        onClick = {}
                    )
            )

            Text(
                text = formattedValue,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NumericTextStyle.fontFamily,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        val a11yValue = formattedValue
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                if (!reduced) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            valueRange = valueRange,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = FxTrackInactive,
                activeTickColor = androidx.compose.ui.graphics.Color.Transparent,
                inactiveTickColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label, currently $a11yValue" }
        )
    }
}
