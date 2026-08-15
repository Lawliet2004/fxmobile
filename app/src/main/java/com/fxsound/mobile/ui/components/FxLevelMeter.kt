package com.fxsound.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.ui.theme.FxGreen
import com.fxsound.mobile.ui.theme.FxRed
import com.fxsound.mobile.ui.theme.FxTextMuted
import com.fxsound.mobile.ui.theme.FxTrackInactive
import com.fxsound.mobile.ui.theme.FxYellow

@Composable
fun FxLevelMeter(
    peakLeft: Float,
    peakRight: Float,
    isClipping: Boolean,
    modifier: Modifier = Modifier
) {
    val meterGradient = remember {
        Brush.horizontalGradient(
            0.0f to FxGreen,
            0.75f to FxGreen,
            0.88f to FxYellow,
            1.0f to FxRed
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "L",
            color = FxTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        SingleMeterBar(
            level = peakLeft.coerceIn(0f, 1f),
            gradient = meterGradient,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "R",
            color = FxTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        SingleMeterBar(
            level = peakRight.coerceIn(0f, 1f),
            gradient = meterGradient,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isClipping) FxRed else FxTrackInactive)
        )
    }
}

@Composable
private fun SingleMeterBar(
    level: Float,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(FxTrackInactive)
    ) {
        if (level > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = level)
                    .clip(RoundedCornerShape(3.dp))
                    .background(gradient)
            )
        }
    }
}
