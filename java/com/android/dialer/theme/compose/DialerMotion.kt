package com.android.dialer.theme.compose

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

// Copied verbatim from material3's ExpressiveMotionTokens (VERSION v0_14_0), the token set that
// MotionScheme.expressive() wraps. Both that scheme and MaterialTheme.motionScheme are internal in
// material3 1.4.0, so Dialer carries the constants itself. See DialerTheme for the wider note.
private const val DEFAULT_SPATIAL_DAMPING = 0.8f
private const val DEFAULT_SPATIAL_STIFFNESS = 380.0f
private const val FAST_SPATIAL_DAMPING = 0.6f
private const val FAST_SPATIAL_STIFFNESS = 800.0f
private const val DEFAULT_EFFECTS_DAMPING = 1.0f
private const val DEFAULT_EFFECTS_STIFFNESS = 1600.0f

/**
 * Material 3 Expressive's motion springs.
 *
 * Spatial specs animate position, size and shape; effects specs animate color and alpha. Replace
 * this object with `MaterialTheme.motionScheme` once a stable material3 makes it public.
 */
internal object DialerMotion {

    /** Position, size and shape changes that are not immediate responses to a touch. */
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = DEFAULT_SPATIAL_DAMPING,
        stiffness = DEFAULT_SPATIAL_STIFFNESS,
    )

    /** Small, immediate responses to a touch, such as a keypad key's press shape morph. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = FAST_SPATIAL_DAMPING,
        stiffness = FAST_SPATIAL_STIFFNESS,
    )

    /** Color and alpha changes. */
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = DEFAULT_EFFECTS_DAMPING,
        stiffness = DEFAULT_EFFECTS_STIFFNESS,
    )
}
