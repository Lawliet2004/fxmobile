package com.fxsound.mobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCyan
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.theme.FxTrackInactive
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
    accentColor: Color = FxAccentCoral,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val formattedValue = when {
        unit == "dB" -> {
            val sign = if (value > 0.05f) "+" else ""
            String.format(Locale.ROOT, "%s%.1f %s", sign, value, unit)
        }
        valueRange.endInclusive <= 4.0f -> String.format(Locale.ROOT, "%.1f", value)
        else -> String.format(Locale.ROOT, "%.1f", value)
    }

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
                modifier = Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onDoubleClick = { onValueChange(defaultValue) },
                    onClick = {}
                )
            )

            Text(
                text = formattedValue,
                color = if (value > defaultValue + 0.01f || value < defaultValue - 0.01f) {
                    accentColor
                } else {
                    FxTextPrimary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val a11yValue = formattedValue
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = FxTrackInactive,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label, currently $a11yValue" }
        )
    }
}
