package com.android.dialer.keypad

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.dialer.R
import com.android.dialer.animation.AnimUtils
import com.android.dialer.callintent.CallInitiationType
import com.android.dialer.callintent.CallIntentBuilder
import com.android.dialer.common.Assert
import com.android.dialer.common.FragmentUtils
import com.android.dialer.common.LogUtil
import com.android.dialer.dialpadview.DialpadFragment
import com.android.dialer.dialpadview.SpecialCharSequenceMgr
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.ui.KeypadScreen
import com.android.dialer.keypad.ui.keypadStrings
import com.android.dialer.precall.PreCall
import com.android.dialer.theme.compose.DialerTheme
import com.android.dialer.ui.core.CollectEvents
import com.android.dialer.util.ViewUtil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Hosts the Compose keypad in place of `DialpadFragment`.
 *
 * An AndroidX [Fragment], so it lives in the activity's support `FragmentManager` while the search
 * fragment beside it stays in the framework one. The two managers coexist; the host only has to
 * commit to each separately.
 *
 * The public surface mirrors the parts of `DialpadFragment` that `MainSearchController` actually
 * calls, and the host callbacks are the same `DialpadFragment` interfaces, so switching the host
 * over is a type change rather than a rewrite. `getLastOutgoingCall` is the one callback no longer
 * used: the view model reads the call log itself.
 *
 * The view model is the activity's rather than this fragment's. That matches how the host uses the
 * fragment: it hides it rather than removing it, specifically so that the number survives the
 * keypad being dismissed.
 */
class KeypadFragment : Fragment() {

    // The activity is a Hilt entry point, so its default factory builds the Hilt view model.
    private val viewModel: KeypadViewModel by lazy {
        ViewModelProvider(requireActivity())[KeypadViewModel::class.java]
    }

    /** Whether the keypad has slid onto the screen. Only the host's slide calls change it. */
    var isDialpadSlideUp: Boolean = false
        private set

    /** Whether the next show should animate. Set by the host just before it hides the keypad. */
    var animate: Boolean = false

    /** Whether the keypad was opened by an incoming `ACTION_DIAL` rather than by the user. */
    var startedFromNewIntent: Boolean = false

    /** The number as typed, formatted. Read by the host to carry it into the search bar. */
    val query: String
        get() = viewModel.uiState.value.digits

    private var firstLaunch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstLaunch = savedInstanceState == null
        isDialpadSlideUp = savedInstanceState?.getBoolean(KEY_IS_DIALPAD_SLIDE_UP) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        // Dispose with the fragment's view rather than on detach, which a hide does not cause.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DialerTheme {
                KeypadHost(
                    screenModel = viewModel,
                    onEffect = ::handleEffect,
                    onQueryChanged = { query ->
                        parent<DialpadFragment.OnDialpadQueryChangedListener>()
                            .onDialpadQueryChanged(query)
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onHostStarted()
    }

    override fun onResume() {
        super.onResume()
        configureFromIntent(requireActivity().intent)
        // The framework does not call onHiddenChanged on the first attach, so do it here, exactly
        // as DialpadFragment did; without it the host never slides the keypad up.
        if (firstLaunch) {
            onHiddenChanged(false)
        }
        firstLaunch = false
    }

    /**
     * Port of `DialpadFragment.configureScreenFromIntent`, less its dialpad chooser, which no live
     * host shows.
     *
     * Only an intent that is new to the keypad fills it: the activity keeps its intent across
     * resumes, and refilling from it would overwrite what the user typed since.
     */
    private fun configureFromIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        // Add call brings up an empty keypad; nothing to fill. The flag is left set, as
        // DialpadFragment left it.
        if (DialpadFragment.isAddCallMode(intent)) {
            startedFromNewIntent = true
            return
        }
        if (firstLaunch || startedFromNewIntent) {
            viewModel.fillFromDialIntent(intent)
        }
        startedFromNewIntent = false
    }

    override fun onPause() {
        super.onPause()
        // Cancels a SIM contact lookup still in flight, so it does not try to dismiss its progress
        // dialog after the activity has gone. DialpadFragment did the same.
        SpecialCharSequenceMgr.cleanup()
    }

    override fun onStop() {
        viewModel.onHostStopped()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_DIALPAD_SLIDE_UP, isDialpadSlideUp)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (activity != null && view != null && !hidden) {
            parent<DialpadFragment.DialpadListener>().onDialpadShown()
        }
    }

    /**
     * Empties the field, as when the host closes search.
     *
     * The host wrapped the same call on `DialpadFragment` with `setImportantForAccessibility(NO)`
     * and back on the digits `EditText`, to stop TalkBack announcing the emptied field. That has no
     * equivalent here and needs none: the digits are a plain `Text` — not focusable, not editable,
     * not a live region — so clearing them raises no text-changed event. The bracket could not work
     * in Compose anyway, since the text changes on the next frame, after it has been restored.
     */
    fun clearDialpad() {
        // The host may call this in the gap between creating the fragment and its asynchronous
        // commit attaching it. Unattached there is no view model to reach and nothing has ever been
        // shown, so there is nothing to clear. DialpadFragment likewise skipped clearing before its
        // view existed.
        if (activity == null) {
            return
        }
        viewModel.clearDigits()
    }

    /**
     * Receives the normalized query from the host on every change.
     *
     * `DialpadFragment` named this `process_quote_emergency_unquote` and ran its pseudo-emergency
     * Easter egg from it. The Easter egg has not been ported yet; this keeps the host contract
     * intact until it is.
     */
    @Suppress("UNUSED_PARAMETER")
    fun processPseudoEmergencyQuery(query: String) = Unit

    /** Slides the keypad onto the screen. Port of `DialpadFragment.slideUp`. */
    fun slideUp(animated: Boolean) {
        Assert.checkArgument(!isDialpadSlideUp)
        isDialpadSlideUp = true
        val animation = when {
            !isLandscape() -> R.anim.dialpad_slide_in_bottom
            ViewUtil.isRtl() -> R.anim.dialpad_slide_in_left
            else -> R.anim.dialpad_slide_in_right
        }
        startSlide(animation = animation, animated = animated, listener = null, easeIn = true)
    }

    /**
     * Slides the keypad off the screen. The host hides this fragment from [listener]'s
     * `onAnimationEnd`. Port of `DialpadFragment.slideDown`.
     */
    fun slideDown(animated: Boolean, listener: Animation.AnimationListener?) {
        Assert.checkArgument(isDialpadSlideUp)
        isDialpadSlideUp = false
        val animation = when {
            !isLandscape() -> R.anim.dialpad_slide_out_bottom
            ViewUtil.isRtl() -> R.anim.dialpad_slide_out_left
            else -> R.anim.dialpad_slide_out_right
        }
        startSlide(
            animation = animation,
            animated = animated,
            listener = listener,
            easeIn = false,
        )
    }

    private fun startSlide(
        animation: Int,
        animated: Boolean,
        listener: Animation.AnimationListener?,
        easeIn: Boolean,
    ) {
        val slide = AnimationUtils.loadAnimation(requireContext(), animation).apply {
            interpolator = if (easeIn) AnimUtils.EASE_IN else AnimUtils.EASE_OUT
            duration = when {
                animated -> resources.getInteger(R.integer.dialpad_slide_in_duration).toLong()
                else -> 0L
            }
            listener?.let(::setAnimationListener)
        }
        view?.startAnimation(slide)
    }

    private fun isLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun handleEffect(effect: KeypadScreenEffect) {
        if (!isAdded) {
            return
        }
        when (effect) {
            is KeypadScreenEffect.PlaceCall ->
                placeCall(CallIntentBuilder(effect.number, CallInitiationType.Type.DIALPAD))
            KeypadScreenEffect.CallVoicemail ->
                placeCall(CallIntentBuilder.forVoicemail(CallInitiationType.Type.DIALPAD))
            KeypadScreenEffect.ShowVoicemailAirplaneModeError -> showError(
                message = R.string.dialog_voicemail_airplane_mode_message,
                tag = VOICEMAIL_AIRPLANE_MODE_DIALOG_TAG,
            )
            KeypadScreenEffect.ShowVoicemailNotReadyError -> showError(
                message = R.string.dialog_voicemail_not_ready_message,
                tag = VOICEMAIL_NOT_READY_DIALOG_TAG,
            )
            KeypadScreenEffect.ShowProhibitedNumberError -> showError(
                message = R.string.dialog_phone_call_prohibited_message,
                tag = PROHIBITED_NUMBER_DIALOG_TAG,
            )
            is KeypadScreenEffect.RunSpecialCode -> runSpecialCode(effect.input)
        }
    }

    private fun runSpecialCode(input: String) {
        // The lookup can outlive this fragment's view, so hold the activity-scoped view model
        // rather than the fragment.
        val handled = try {
            SpecialCharSequenceMgr.handleChars(requireActivity(), input) { number ->
                viewModel.insertSimContactNumber(number)
            }
        } catch (e: SecurityException) {
            // Some codes need permissions only a privileged install holds. *#06# is the known one:
            // getDeviceId needs READ_PRIVILEGED_PHONE_STATE since API 29, and throws before any
            // dialog is shown. Unhandled, it crashed the dialer; now the code stays in the field.
            LogUtil.w(TAG, "Cannot run the special code: $e")
            false
        }
        if (handled) {
            viewModel.clearDigits()
        }
    }

    private fun placeCall(builder: CallIntentBuilder) {
        PreCall.start(requireActivity(), builder)
        parent<DialpadFragment.DialpadListener>().onCallPlacedFromDialpad()
    }

    // The legacy dialog is still a framework DialogFragment, so it needs the framework manager.
    // Replaced along with the rest of DialpadFragment.
    @Suppress("DEPRECATION")
    private fun showError(message: Int, tag: String) {
        DialpadFragment.ErrorDialogFragment.newInstance(message)
            .show(requireActivity().fragmentManager, tag)
    }

    private inline fun <reified T> parent(): T = FragmentUtils.getParentUnsafe(this, T::class.java)

    private companion object {
        private const val TAG = "KeypadFragment"

        private const val KEY_IS_DIALPAD_SLIDE_UP = "pref_is_dialpad_slide_out"

        // Fragment tags for the error dialogs, unchanged from DialpadFragment.
        private const val VOICEMAIL_AIRPLANE_MODE_DIALOG_TAG =
            "voicemail_request_during_airplane_mode"
        private const val VOICEMAIL_NOT_READY_DIALOG_TAG = "voicemail_not_ready"
        private const val PROHIBITED_NUMBER_DIALOG_TAG = "phone_prohibited_dialog"
    }
}

/**
 * Binds the screen to its model: state in, actions out, effects handed to the fragment, and every
 * change of number reported to the host so it can search on it.
 */
@Composable
private fun KeypadHost(
    screenModel: KeypadScreenModel,
    onEffect: (KeypadScreenEffect) -> Unit,
    onQueryChanged: (String) -> Unit,
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    CollectEvents(events = screenModel.effects, onEvent = onEffect)

    // Remembered so recomposition hands CollectEvents the same flow, which it would otherwise
    // restart collecting.
    val queries = remember(screenModel) {
        screenModel.uiState
            .map { state -> state.digits }
            .distinctUntilChanged()
    }
    CollectEvents(events = queries, onEvent = onQueryChanged)

    // Fills the host's full-screen container but only draws at the bottom. The empty space above
    // handles no touches, so they fall through to the search results underneath, as the host
    // expects.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        KeypadScreen(
            uiState = uiState,
            strings = keypadStrings(),
            onAction = screenModel::onAction,
        )
    }
}
