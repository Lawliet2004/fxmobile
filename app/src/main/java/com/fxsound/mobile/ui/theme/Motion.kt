package com.fxsound.mobile.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object FxMotion {
    const val TAB_MS = 180
    const val PRESS_SCALE = 0.97f
    const val SNAPPY_DAMPING = 0.85f
    const val SNAPPY_STIFFNESS = 400f
    const val SMOOTH_DAMPING = 0.9f
    const val SMOOTH_STIFFNESS = 300f

    val snappy: SpringSpec<Float> = spring(
        dampingRatio = SNAPPY_DAMPING,
        stiffness = SNAPPY_STIFFNESS
    )
    val smooth: SpringSpec<Float> = spring(
        dampingRatio = SMOOTH_DAMPING,
        stiffness = SMOOTH_STIFFNESS
    )
    val settle: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 200f
    )
    val snappyColor: SpringSpec<Color> = spring(
        dampingRatio = SNAPPY_DAMPING,
        stiffness = SNAPPY_STIFFNESS
    )
    val smoothColor: SpringSpec<Color> = spring(
        dampingRatio = SMOOTH_DAMPING,
        stiffness = SMOOTH_STIFFNESS
    )

    fun floatSpec(reduced: Boolean): AnimationSpec<Float> =
        if (reduced) tween(0) else snappy

    fun colorSpec(reduced: Boolean): AnimationSpec<Color> =
        if (reduced) tween(0) else snappyColor
}

@Composable
fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
