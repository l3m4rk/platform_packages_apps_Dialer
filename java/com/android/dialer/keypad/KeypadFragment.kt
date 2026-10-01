package com.android.dialer.keypad

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.contacts.common.dialog.CallSubjectDialog
import com.android.dialer.R
import com.android.dialer.animation.AnimUtils
import com.android.dialer.callintent.CallInitiationType
import com.android.dialer.callintent.CallIntentBuilder
import com.android.dialer.common.Assert
import com.android.dialer.common.FragmentUtils
import com.android.dialer.common.LogUtil
import com.android.dialer.dialpadview.SpecialCharSequenceMgr
import com.android.dialer.keypad.model.KeypadScreenEffect
import com.android.dialer.keypad.ui.KeypadEntranceState
import com.android.dialer.keypad.ui.KeypadPlacement
import com.android.dialer.keypad.ui.KeypadScreen
import com.android.dialer.keypad.ui.keypadStrings
import com.android.dialer.precall.PreCall
import com.android.dialer.theme.compose.DialerTheme
import com.android.dialer.ui.core.CollectEvents
import com.android.dialer.util.ViewUtil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Hosts the Compose keypad for `MainSearchController`.
 *
 * The view model is the activity's: the host hides this fragment rather than removing it, so the
 * number survives the keypad being dismissed.
 */
class KeypadFragment : Fragment() {

    interface HostListener {
        fun onDialpadShown()

        fun onCallPlacedFromDialpad()
    }

    fun interface OnQueryChangedListener {
        fun onDialpadQueryChanged(query: String)
    }

    private val viewModel: KeypadViewModel by lazy {
        ViewModelProvider(requireActivity())[KeypadViewModel::class.java]
    }

    var isDialpadSlideUp: Boolean = false
        private set

    var animate: Boolean = false

    var startedFromNewIntent: Boolean = false

    val query: String
        get() = viewModel.uiState.value.digits

    private var firstLaunch = false

    private val keyEntrance = KeypadEntranceState()

    /** Bumped on each show, so the number takes focus for hardware keyboards. */
    private var shows by mutableIntStateOf(0)

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
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DialerTheme {
                KeypadHost(
                    screenModel = viewModel,
                    entranceState = keyEntrance,
                    focusRequests = shows,
                    onEffect = ::handleEffect,
                    onQueryChanged = { query ->
                        parent<OnQueryChangedListener>().onDialpadQueryChanged(query)
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
        // The framework skips onHiddenChanged on the first attach, and the host slides up from it.
        if (firstLaunch) {
            onHiddenChanged(false)
        }
        firstLaunch = false
    }

    /**
     * Fills the keypad from an intent new to it only: the activity keeps its intent across resumes,
     * and refilling would overwrite what the user typed since.
     */
    private fun configureFromIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        if (isAddCallMode(intent)) {
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
        viewModel.onHostPaused()
        // Cancels a SIM contact lookup, whose progress dialog must not outlive the activity.
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
            if (animate) {
                keyEntrance.play()
            }
            shows++
            parent<HostListener>().onDialpadShown()
        }
    }

    /**
     * Empties the field, as when the host closes search.
     *
     * The host used to hide the field from accessibility around this call, so TalkBack would not
     * announce the deletion. Compose has no such switch, and none is needed: the host only clears
     * a field that is already empty, a keypad that is hidden, or one whose activity is pausing
     * after a call, so there is never a visible deletion to announce.
     */
    fun clearDialpad() {
        // Before the commit attaches it, nothing has been shown and there is no view model to reach.
        if (activity == null) {
            return
        }
        viewModel.clearDigits()
    }

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

    /** The host hides this fragment from [listener]'s `onAnimationEnd`. */
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
            is KeypadScreenEffect.CallWithNote -> {
                CallSubjectDialog.start(requireActivity(), effect.number)
                parent<HostListener>().onCallPlacedFromDialpad()
            }
            is KeypadScreenEffect.RunSpecialCode -> runSpecialCode(effect.input)
        }
    }

    private fun runSpecialCode(input: String) {
        val handled = try {
            SpecialCharSequenceMgr.handleChars(requireActivity(), input) { number ->
                viewModel.insertSimContactNumber(number)
            }
        } catch (e: SecurityException) {
            // *#06# needs READ_PRIVILEGED_PHONE_STATE, which only a system install holds.
            LogUtil.w(TAG, "Cannot run the special code: $e")
            false
        }
        if (handled) {
            viewModel.clearDigits()
        }
    }

    private fun placeCall(builder: CallIntentBuilder) {
        PreCall.start(requireActivity(), builder)
        parent<HostListener>().onCallPlacedFromDialpad()
    }

    private inline fun <reified T> parent(): T = FragmentUtils.getParentUnsafe(this, T::class.java)

    companion object {
        private const val TAG = "KeypadFragment"

        private const val EXTRA_ADD_CALL_MODE = "add_call_mode"

        /** Whether [intent] is the in-call screen's "add call", which opens an empty keypad. */
        @JvmStatic
        fun isAddCallMode(intent: Intent?): Boolean = when (intent?.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW ->
                intent.getBooleanExtra(EXTRA_ADD_CALL_MODE, false)
            else -> false
        }

        private const val KEY_IS_DIALPAD_SLIDE_UP = "pref_is_dialpad_slide_out"
    }
}

@Composable
private fun KeypadHost(
    screenModel: KeypadScreenModel,
    entranceState: KeypadEntranceState,
    focusRequests: Int,
    onEffect: (KeypadScreenEffect) -> Unit,
    onQueryChanged: (String) -> Unit,
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    CollectEvents(events = screenModel.effects, onEvent = onEffect)

    // Remembered: a new flow on each recomposition would restart the collection.
    val queries = remember(screenModel) {
        screenModel.uiState
            .map { state -> state.digits }
            .distinctUntilChanged()
    }
    CollectEvents(events = queries, onEvent = onQueryChanged)

    KeypadPlacement { placement ->
        KeypadScreen(
            uiState = uiState,
            strings = keypadStrings(),
            onAction = screenModel::onAction,
            modifier = placement,
            entranceState = entranceState,
            focusRequests = focusRequests,
        )
    }
}
