package com.android.dialer.keypad.domain

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface Vibration {
    fun vibrate(durationMs: Long)
}

internal class SystemVibration @Inject constructor(
    @ApplicationContext context: Context,
) : Vibration {
    private val vibrator: Vibrator? =
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    override fun vibrate(durationMs: Long) {
        val vibration = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator?.vibrate(vibration)
    }
}
