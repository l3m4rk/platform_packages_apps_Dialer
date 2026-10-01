package com.android.dialer.keypad

import android.content.Intent
import android.media.ToneGenerator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dialer.keypad.domain.CallWithNoteAvailability
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibited
import com.android.dialer.keypad.domain.DialIntentNumber
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.LastOutgoingCall
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.TONE_LENGTH_MS
import com.android.dialer.keypad.domain.Vibration
import com.android.dialer.keypad.domain.VoicemailAvailability
import com.android.dialer.keypad.model.DialpadDigits
import com.android.dialer.keypad.model.KeypadAction
import com.android.dialer.keypad.model.KeypadError
import com.android.dialer.keypad.model.KeypadKey
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.model.KeypadUiState
import com.android.dialer.keypad.model.PAUSE
import com.android.dialer.keypad.model.PseudoEmergency
import com.android.dialer.keypad.model.WAIT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

@Suppress("LongParameterList")
@HiltViewModel
internal class KeypadViewModel @Inject constructor(
    private val tonePlayer: DtmfTonePlayer,
    private val voicemailAvailability: VoicemailAvailability,
    private val emergencyCallWarning: EmergencyCallWarning,
    private val phoneNumberFormatting: PhoneNumberFormatting,
    private val lastOutgoingCall: LastOutgoingCall,
    private val checkIfNumberIsProhibited: CheckIfNumberIsProhibited,
    private val dialIntentNumber: DialIntentNumber,
    private val vibration: Vibration,
    private val callWithNoteAvailability: CallWithNoteAvailability,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    KeypadScreenModel {

    private val digits = DialpadDigits()

    /** Held keys: the tone stops only when the last finger lifts. */
    private val pressedKeys = mutableSetOf<KeypadKey>()

    private var lastDialedNumber: String? = null

    /** Cancelled by a newer intent or a clear, which a late result would otherwise overwrite. */
    private var fillJob: Job? = null

    /**
     * Whether another app supplied the number. Special codes only run for typed input, or any app
     * could send a `tel:` link that runs an MMI code; saved so a restored number stays inert.
     */
    private var isFilledByIntent: Boolean
        get() = savedStateHandle[KEY_FILLED_BY_INTENT] ?: false
        set(value) {
            savedStateHandle[KEY_FILLED_BY_INTENT] = value
        }

    private var pseudoEmergencyPulses: Job? = null

    private val isEmergencyCallWarningActive = MutableStateFlow(false)

    private val isCallWithNoteAvailable = MutableStateFlow(false)

    private val error = MutableStateFlow(
        savedStateHandle.get<String>(KEY_ERROR)?.let(KeypadError::valueOf),
    )

    // Eagerly: the upstream is cheap, and the host reads `value` without collecting.
    override val uiState: StateFlow<KeypadUiState> = combine(
        digits.value,
        isEmergencyCallWarningActive,
        isCallWithNoteAvailable,
        error,
    ) { value, warningActive, callWithNoteAvailable, error ->
        val text = value.text
        KeypadUiState(
            digits = text,
            selectionStart = value.selectionStart,
            selectionEnd = value.selectionEnd,
            isDeleteEnabled = text.isNotEmpty(),
            isOverflowVisible = text.isNotEmpty(),
            showsEmergencyCallWarning = text.isEmpty() && warningActive,
            isPseudoEmergencyNumber = PseudoEmergency.matches(text),
            isCallWithNoteAvailable = callWithNoteAvailable,
            error = error,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = KeypadUiState(),
        )

    // No replay: re-delivering a PlaceCall after rotation would place a second call.
    private val _effects = MutableSharedFlow<KeypadScreenEffect>(extraBufferCapacity = 1)
    override val effects: Flow<KeypadScreenEffect> = _effects.asSharedFlow()

    init {
        restoreDigits()

        digits.value
            .onEach { value ->
                savedStateHandle[KEY_DIGITS] = value.text
                savedStateHandle[KEY_SELECTION_START] = value.selectionStart
                savedStateHandle[KEY_SELECTION_END] = value.selectionEnd
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            phoneNumberFormatting.createWatcher()?.let(digits::addFormattingWatcher)
        }

        // Re-read whenever the hint could appear: carrier config and service state change elsewhere.
        digits.text
            .map { text -> text.isEmpty() }
            .distinctUntilChanged()
            .filter { isEmpty -> isEmpty }
            .onEach { refreshEmergencyCallWarning() }
            .launchIn(viewModelScope)

        digits.text
            .onEach { text ->
                when {
                    text.isEmpty() -> isFilledByIntent = false
                    !isFilledByIntent -> emitEffect(KeypadScreenEffect.RunSpecialCode(text))
                }
            }
            .launchIn(viewModelScope)

        digits.text
            .map(PseudoEmergency::matches)
            .distinctUntilChanged()
            .onEach { matches ->
                if (matches) startPseudoEmergencyPulses() else stopPseudoEmergencyPulses()
            }
            .launchIn(viewModelScope)
    }

    override fun onHostStarted() {
        tonePlayer.acquire()
        // Service state, SIMs and permissions may have changed while the keypad was away.
        refreshEmergencyCallWarning()
        isCallWithNoteAvailable.value = callWithNoteAvailability.isAvailable()
        viewModelScope.launch { lastDialedNumber = lastOutgoingCall() }
    }

    override fun onHostPaused() {
        tonePlayer.stop()
        pressedKeys.clear()
    }

    override fun onHostStopped() {
        tonePlayer.release()
        pressedKeys.clear()
    }

    override fun clearDigits() {
        fillJob?.cancel()
        digits.clear()
    }

    override fun fillFromDialIntent(intent: Intent) {
        fillJob?.cancel()
        fillJob = viewModelScope.launch {
            dialIntentNumber(intent)?.let { number ->
                // Before the text changes, so the change is never taken for typing.
                isFilledByIntent = true
                showNumber(number)
            }
        }
    }

    override fun insertSimContactNumber(number: String) {
        digits.insertAtStart(number)
    }

    override fun onAction(action: KeypadAction) {
        when (action) {
            is KeypadAction.KeyPressed -> onKeyPressed(action.key)
            is KeypadAction.KeyReleased -> onKeyReleased(action.key)
            KeypadAction.VoicemailKeyLongPressed -> onVoicemailKeyLongPressed()
            KeypadAction.PlusKeyLongPressed -> onPlusKeyLongPressed()
            KeypadAction.DeleteClicked -> digits.delete()
            KeypadAction.DeleteLongPressed -> digits.clear()
            KeypadAction.PauseClicked -> digits.insertDialStringChar(PAUSE)
            KeypadAction.WaitClicked -> digits.insertDialStringChar(WAIT)
            KeypadAction.CallClicked -> onCallClicked()
            KeypadAction.ErrorDismissed -> showError(null)
            is KeypadAction.CharacterTyped -> digits.append(action.char)
            is KeypadAction.DigitsEdited ->
                digits.applyEdit(action.text, action.selectionStart, action.selectionEnd)
            KeypadAction.CallWithNoteClicked ->
                emitEffect(KeypadScreenEffect.CallWithNote(digits.text.value))
        }
    }

    private fun onKeyPressed(key: KeypadKey) {
        pressedKeys += key
        tonePlayer.play(tone = key.tone)
        digits.append(key)
    }

    private fun onKeyReleased(key: KeypadKey) {
        pressedKeys -= key
        if (pressedKeys.isEmpty()) {
            tonePlayer.stop()
        }
    }

    private fun onVoicemailKeyLongPressed() {
        // The press itself has usually typed a 1, and touch exploration types two.
        if (digits.text.value !in VOICEMAIL_LONG_PRESS_ALLOWED) {
            return
        }
        digits.removePreviousDigitIfPossible('1')
        digits.removePreviousDigitIfPossible('1')

        when {
            voicemailAvailability.isVoicemailReachable() ->
                emitEffect(KeypadScreenEffect.CallVoicemail)
            voicemailAvailability.isAirplaneModeOn() ->
                showError(KeypadError.VOICEMAIL_AIRPLANE_MODE)
            else -> showError(KeypadError.VOICEMAIL_NOT_READY)
        }
    }

    private fun onPlusKeyLongPressed() {
        // An accessibility long press arrives without a press, so there is no typed zero to undo.
        if (KeypadKey.ZERO in pressedKeys) {
            digits.removePreviousDigitIfPossible('0')
            digits.removePreviousDigitIfPossible('0')
        }
        digits.append('+')
        tonePlayer.stop()
        pressedKeys -= KeypadKey.ZERO
    }

    private fun onCallClicked() {
        val number = digits.text.value
        when {
            number.isEmpty() -> recallLastDialedNumber()
            checkIfNumberIsProhibited(number) -> {
                digits.clear()
                showError(KeypadError.PROHIBITED_NUMBER)
            }
            else -> emitEffect(KeypadScreenEffect.PlaceCall(number))
        }
    }

    private fun recallLastDialedNumber() {
        val number = lastDialedNumber
        if (number.isNullOrEmpty()) {
            tonePlayer.play(tone = ToneGenerator.TONE_PROP_NACK, durationMs = TONE_LENGTH_MS)
        } else {
            showNumber(number)
        }
    }

    private fun showNumber(number: String) {
        digits.setText(number)
        // The formatted text can be longer than what was set.
        digits.setSelection(digits.length)
    }

    private fun startPseudoEmergencyPulses() {
        pseudoEmergencyPulses = viewModelScope.launch {
            repeat(PseudoEmergency.PULSES - 1) {
                delay(PseudoEmergency.PULSE_MS.milliseconds)
                vibration.vibrate(PseudoEmergency.PULSE_MS)
            }
            delay(PseudoEmergency.PULSE_MS.milliseconds)
            buzzOnceMore()
        }
    }

    private fun stopPseudoEmergencyPulses() {
        val pulses = pseudoEmergencyPulses ?: return
        pseudoEmergencyPulses = null
        // A pulse cut short still ends with the final buzz; a finished one has already sent it.
        if (pulses.isActive) {
            pulses.cancel()
            buzzOnceMore()
        }
    }

    private fun buzzOnceMore() {
        viewModelScope.launch {
            delay(PseudoEmergency.FINAL_BUZZ_DELAY_MS.milliseconds)
            vibration.vibrate(PseudoEmergency.PULSE_MS)
        }
    }

    // Runs before anything watches the digits, so a restored number is never taken for typing.
    private fun restoreDigits() {
        val text = savedStateHandle.get<String>(KEY_DIGITS)
        if (text.isNullOrEmpty()) {
            return
        }
        digits.setText(text)
        digits.setSelection(
            start = savedStateHandle.get<Int>(KEY_SELECTION_START)?.coerceIn(0, digits.length)
                ?: digits.length,
            end = savedStateHandle.get<Int>(KEY_SELECTION_END)?.coerceIn(0, digits.length)
                ?: digits.length,
        )
    }

    private fun showError(value: KeypadError?) {
        error.value = value
        savedStateHandle[KEY_ERROR] = value?.name
    }

    private fun refreshEmergencyCallWarning() {
        isEmergencyCallWarningActive.value = emergencyCallWarning.shouldShow()
    }

    private fun emitEffect(effect: KeypadScreenEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private companion object {
        private val VOICEMAIL_LONG_PRESS_ALLOWED = setOf("", "1", "11")

        private const val KEY_DIGITS = "keypad_digits"
        private const val KEY_SELECTION_START = "keypad_selection_start"
        private const val KEY_SELECTION_END = "keypad_selection_end"
        private const val KEY_FILLED_BY_INTENT = "keypad_filled_by_intent"
        private const val KEY_ERROR = "keypad_error"
    }
}
