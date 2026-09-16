package com.android.dialer.keypad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.VoicemailAvailability
import com.android.dialer.keypad.model.DialpadDigits
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.keypad.model.PAUSE
import com.android.dialer.keypad.model.WAIT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class KeypadViewModel @Inject constructor(
    private val tonePlayer: DtmfTonePlayer,
    private val voicemailAvailability: VoicemailAvailability,
    private val emergencyCallWarning: EmergencyCallWarning,
    private val phoneNumberFormatting: PhoneNumberFormatting,
) : ViewModel(),
    KeypadScreenModel {

    private val digits = DialpadDigits()

    /**
     * Which keys are currently held.
     *
     * A tone runs until every finger is lifted, so releasing one key of two must not silence the
     * other. The View keypad tracked this the same way, and getting it wrong leaves a tone playing
     * or cuts it off early.
     */
    private val pressedKeys = mutableSetOf<KeypadKey>()

    /** Re-read by [refreshEmergencyCallWarning] rather than queried while mapping state. */
    private val isEmergencyCallWarningActive = MutableStateFlow(false)

    /**
     * Derived from its inputs rather than pushed by each action, so a new action cannot forget to
     * republish.
     *
     * Eagerly, not `WhileSubscribed`: that exists to release expensive upstreams such as database
     * cursors, and this one is a cheap combine. Staying active also keeps `value` correct for
     * readers that never collect.
     */
    override val uiState: StateFlow<KeypadUiState> = combine(
        digits.text,
        isEmergencyCallWarningActive,
    ) { text, warningActive ->
        KeypadUiState(
            digits = text,
            isDeleteEnabled = text.isNotEmpty(),
            isOverflowVisible = text.isNotEmpty(),
            // The hint renders inside the empty digits field, so it has nowhere else to go.
            showsEmergencyCallWarning = text.isEmpty() && warningActive,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = KeypadUiState(),
        )

    // Buffered so that emitting never suspends the coroutine that does it. Nothing replays:
    // an effect is acted on once, and re-delivering one after a configuration change would
    // place a second call.
    private val _effects = MutableSharedFlow<KeypadScreenEffect>(extraBufferCapacity = 1)
    override val effects: Flow<KeypadScreenEffect> = _effects.asSharedFlow()

    init {
        // The keypad accepts input before this arrives; formatting simply starts applying once it
        // does, which is what the fragment did too.
        viewModelScope.launch {
            phoneNumberFormatting.createWatcher()?.let(digits::addFormattingWatcher)
        }

        // Re-read whenever the hint could become visible rather than caching it once: carrier
        // config, service state and permissions all change outside this screen. Mirrors the
        // fragment, which called updateDialpadHint from onTextChanged whenever emptiness flipped.
        digits.text
            .map { text -> text.isEmpty() }
            .distinctUntilChanged()
            .filter { isEmpty -> isEmpty }
            .onEach { refreshEmergencyCallWarning() }
            .launchIn(viewModelScope)
    }

    override fun onHostStarted() {
        tonePlayer.acquire()
        // Airplane mode, permissions and service state can all have changed while the keypad was
        // away, and the digits are unchanged, so nothing else would trigger a re-read.
        refreshEmergencyCallWarning()
    }

    override fun onHostStopped() {
        tonePlayer.release()
        pressedKeys.clear()
    }

    override fun onKeyPressed(key: KeypadKey) {
        pressedKeys += key
        tonePlayer.play(tone = key.tone)
        digits.append(key)
    }

    override fun onKeyReleased(key: KeypadKey) {
        pressedKeys -= key
        if (pressedKeys.isEmpty()) {
            tonePlayer.stop()
        }
    }

    override fun onVoicemailKeyLongPressed() {
        // Anything else in the field means the user is dialing a number that starts with 1, not
        // reaching for voicemail. "1" and "11" are here because a press has usually already typed
        // one, and touch exploration types two.
        if (digits.text.value !in VOICEMAIL_LONG_PRESS_ALLOWED) {
            return
        }
        digits.removePreviousDigitIfPossible('1')
        digits.removePreviousDigitIfPossible('1')

        emitEffect(
            when {
                voicemailAvailability.isVoicemailReachable() -> KeypadScreenEffect.CallVoicemail
                voicemailAvailability.isAirplaneModeOn() ->
                    KeypadScreenEffect.ShowVoicemailAirplaneModeError
                else -> KeypadScreenEffect.ShowVoicemailNotReadyError
            },
        )
    }

    override fun onPlusKeyLongPressed() {
        // Only undo the typed zero when the key is genuinely held. An accessibility service can
        // deliver a long press without a preceding press, and there is then nothing to remove.
        if (KeypadKey.ZERO in pressedKeys) {
            digits.removePreviousDigitIfPossible('0')
            digits.removePreviousDigitIfPossible('0')
        }
        digits.append('+')
        tonePlayer.stop()
        pressedKeys -= KeypadKey.ZERO
    }

    override fun onDeleteClicked() {
        digits.delete()
    }

    override fun onDeleteLongPressed() {
        digits.clear()
    }

    override fun onPauseClicked() {
        digits.insertDialStringChar(PAUSE)
    }

    override fun onWaitClicked() {
        digits.insertDialStringChar(WAIT)
    }

    private fun refreshEmergencyCallWarning() {
        isEmergencyCallWarningActive.value = emergencyCallWarning.shouldShow()
    }

    private fun emitEffect(effect: KeypadScreenEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private companion object {
        private val VOICEMAIL_LONG_PRESS_ALLOWED = setOf("", "1", "11")
    }
}
