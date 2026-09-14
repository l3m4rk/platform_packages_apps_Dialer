package com.android.dialer.keypad.domain

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings
import com.android.dialer.common.LogUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Plays DTMF tones through a [ToneGenerator] on [AudioManager.STREAM_DTMF].
 *
 * Port of `DialpadFragment`'s tone handling, with two deliberate differences.
 *
 * The fragment cached the DTMF setting in `onResume`; this reads it, and the ringer mode, afresh on
 * every [play]. The fragment already re-read the ringer mode every time, noting that silent mode
 * can be toggled without leaving the activity, and the same is true of the setting.
 *
 * [stop] is unguarded. The fragment checked the cached setting there too, but that check could
 * never matter: with the flag false no tone had been started, so there was nothing to stop. Read
 * live the same check becomes a way to strand a tone — a key press starts one of
 * [TONE_LENGTH_INFINITE], so if the setting flips off before the release the tone would play
 * forever. Stopping is a safety operation and must not depend on anything.
 *
 * A generator that fails to construct is tolerated rather than fatal: these tones are local
 * feedback, less important than the call itself.
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

/** The real factory: a generator on the DTMF stream at [TONE_RELATIVE_VOLUME]. */
internal class SystemToneGeneratorFactory @Inject constructor() : ToneGeneratorFactory {

    override fun create(): ToneGenerator =
        ToneGenerator(AudioManager.STREAM_DTMF, TONE_RELATIVE_VOLUME)

    private companion object {
        /** The DTMF tone volume, relative to other sounds in the stream. */
        private const val TONE_RELATIVE_VOLUME = 80
    }
}
