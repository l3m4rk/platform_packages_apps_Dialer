package com.android.dialer.keypad.domain

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings
import com.android.dialer.common.LogUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the dialing-tone setting and ringer mode on every [play], as either can change without
 * leaving the keypad.
 *
 * [stop] is deliberately unguarded: a held key's tone is [TONE_LENGTH_INFINITE], and a stop that
 * checked the setting would strand it if the setting changed mid-press.
 */
internal class SystemDtmfTonePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val toneGeneratorFactory: ToneGeneratorFactory,
) : DtmfTonePlayer {

    private val lock = Any()

    private var toneGenerator: ToneGenerator? = null

    override fun acquire() {
        synchronized(lock) {
            if (toneGenerator == null) {
                toneGenerator = createToneGenerator()
            }
        }
    }

    override fun release() {
        synchronized(lock) {
            toneGenerator?.release()
            toneGenerator = null
        }
    }

    override fun play(tone: Int, durationMs: Int) {
        if (!isDtmfToneEnabled() || isSilenced()) {
            return
        }
        synchronized(lock) {
            val generator = toneGenerator
            if (generator == null) {
                LogUtil.w(TAG, "toneGenerator == null, dropping tone")
            } else {
                // Starting a tone stops whichever one is already playing.
                generator.startTone(tone, durationMs)
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            toneGenerator?.stopTone()
        }
    }

    // ToneGenerator throws when the platform cannot hand out an audio session.
    @Suppress("TooGenericExceptionCaught")
    private fun createToneGenerator(): ToneGenerator? = try {
        toneGeneratorFactory.create()
    } catch (e: RuntimeException) {
        LogUtil.e(TAG, "Failed to create the local tone generator", e)
        null
    }

    private fun isDtmfToneEnabled(): Boolean =
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.DTMF_TONE_WHEN_DIALING,
            1,
        ) == 1

    private fun isSilenced(): Boolean {
        val ringerMode = context.getSystemService(AudioManager::class.java).ringerMode
        return ringerMode == AudioManager.RINGER_MODE_SILENT ||
            ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    private companion object {
        private const val TAG = "SystemDtmfTonePlayer"
    }
}

internal class SystemToneGeneratorFactory @Inject constructor() : ToneGeneratorFactory {

    override fun create(): ToneGenerator =
        ToneGenerator(AudioManager.STREAM_DTMF, TONE_RELATIVE_VOLUME)

    private companion object {
        private const val TONE_RELATIVE_VOLUME = 80
    }
}
