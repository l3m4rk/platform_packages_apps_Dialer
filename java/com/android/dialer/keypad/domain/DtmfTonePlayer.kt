package com.android.dialer.keypad.domain

import android.media.ToneGenerator

/** Play a tone until [DtmfTonePlayer.stop] is called. What a held keypad key uses. */
internal const val TONE_LENGTH_INFINITE = -1

/** The length of one-shot DTMF tones, in milliseconds. */
internal const val TONE_LENGTH_MS = 150

/**
 * Plays the local DTMF feedback tones the keypad makes as keys are pressed.
 *
 * These are a local audio signal only — the tones that travel down an active call are Telecom's
 * business, not the keypad's. Playback is suppressed when the user has turned off
 * `Settings.System.DTMF_TONE_WHEN_DIALING` or the device is in silent or vibrate mode.
 *
 * A key press starts a tone of [TONE_LENGTH_INFINITE] and the release stops it, so [play] and
 * [stop] must stay balanced. The keypad tracks which keys are down so that lifting one finger of
 * several does not cut the tone short.
 */
internal interface DtmfTonePlayer {

    /** Creates the underlying generator. Cheap to call again; a second call is a no-op. */
    fun acquire()

    /** Releases the generator. [play] does nothing until [acquire] is called again. */
    fun release()

    fun play(tone: Int, durationMs: Int = TONE_LENGTH_INFINITE)

    fun stop()
}

/**
 * Creates [ToneGenerator]s.
 *
 * A seam purely for testing: [ToneGenerator] is a thin wrapper over native audio that cannot be
 * meaningfully constructed or observed off-device.
 */
internal fun interface ToneGeneratorFactory {
    fun create(): ToneGenerator
}
