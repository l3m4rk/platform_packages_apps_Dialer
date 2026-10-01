package com.android.dialer.keypad.domain

import android.media.ToneGenerator

/** Plays until [DtmfTonePlayer.stop]. */
internal const val TONE_LENGTH_INFINITE = -1

internal const val TONE_LENGTH_MS = 150

/** Local key feedback only; tones sent down a call are Telecom's. */
internal interface DtmfTonePlayer {

    fun acquire()

    fun release()

    fun play(tone: Int, durationMs: Int = TONE_LENGTH_INFINITE)

    fun stop()
}

/** A test seam: [ToneGenerator] is native audio that cannot be observed off-device. */
internal fun interface ToneGeneratorFactory {
    fun create(): ToneGenerator
}
