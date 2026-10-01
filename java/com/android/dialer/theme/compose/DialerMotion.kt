package com.android.dialer.theme.compose

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

// material3's ExpressiveMotionTokens (v0_14_0), internal in 1.4.0.
private const val DEFAULT_SPATIAL_DAMPING = 0.8f
private const val DEFAULT_SPATIAL_STIFFNESS = 380.0f
private const val FAST_SPATIAL_DAMPING = 0.6f
private const val FAST_SPATIAL_STIFFNESS = 800.0f
private const val DEFAULT_EFFECTS_DAMPING = 1.0f
private const val DEFAULT_EFFECTS_STIFFNESS = 1600.0f

/** Replace with `MaterialTheme.motionScheme` once a stable material3 makes it public. */
internal object DialerMotion {

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = DEFAULT_SPATIAL_DAMPING,
        stiffness = DEFAULT_SPATIAL_STIFFNESS,
    )

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = FAST_SPATIAL_DAMPING,
        stiffness = FAST_SPATIAL_STIFFNESS,
    )

    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = DEFAULT_EFFECTS_DAMPING,
        stiffness = DEFAULT_EFFECTS_STIFFNESS,
    )
}
