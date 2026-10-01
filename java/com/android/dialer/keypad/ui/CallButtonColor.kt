package com.android.dialer.keypad.ui

import android.animation.ArgbEvaluator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.android.dialer.keypad.model.PseudoEmergency
import com.android.dialer.theme.compose.DialerColors

private val pulseInterpolator = AccelerateDecelerateInterpolator()

private val pulseColors = ArgbEvaluator()

// ValueAnimator's default interpolator.
private val PulseEasing = Easing(pulseInterpolator::getInterpolation)

/** Green, or a single blue-red pulse once the pseudo-emergency number is spelled. */
@Composable
internal fun callButtonContainerColor(isPseudoEmergencyNumber: Boolean): Color {
    val pulse = remember { Animatable(0f) }
    var isPulsing by remember { mutableStateOf(false) }

    LaunchedEffect(isPseudoEmergencyNumber) {
        if (!isPseudoEmergencyNumber) {
            return@LaunchedEffect
        }
        isPulsing = true
        try {
            pulse.snapTo(0f)
            // One reversing animation, so no frame is lost between legs.
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = repeatable(
                    iterations = PseudoEmergency.PULSES,
                    animation = tween(
                        durationMillis = PseudoEmergency.PULSE_MS.toInt(),
                        easing = PulseEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
        } finally {
            isPulsing = false
        }
    }

    return when {
        isPulsing -> Color(
            pulseColors.evaluate(
                pulse.value,
                android.graphics.Color.BLUE,
                android.graphics.Color.RED,
            ) as Int,
        )
        else -> DialerColors.callContainer()
    }
}
