package com.android.dialer.keypad.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.android.dialer.R

/**
 * The screen's localized text, passed in rather than resolved inside the composables.
 *
 * Unit tests run without an Android resource table — `isIncludeAndroidResources` is off, because
 * Robolectric rejects this module's `minSdkVersion`. A `stringResource` call inside the keypad
 * would therefore throw in every Robolectric test. Threading the strings through one boundary keeps
 * the composables renderable there while leaving the strings themselves in resources, where they
 * stay translatable. The real wiring is covered by an instrumented test instead.
 */
@Immutable
internal data class KeypadStrings(
    val voicemailKeyAction: String,
    val plusKeyAction: String,
    val deleteButton: String,
    val overflowButton: String,
    val call: String,
    val emergencyCallWarning: String,
    val addPause: String,
    val addWait: String,
)

@Composable
internal fun keypadStrings(): KeypadStrings = KeypadStrings(
    voicemailKeyAction = stringResource(R.string.description_voicemail_button),
    plusKeyAction = stringResource(R.string.description_image_button_plus),
    deleteButton = stringResource(R.string.description_delete_button),
    overflowButton = stringResource(R.string.description_dialpad_overflow),
    call = stringResource(R.string.call),
    emergencyCallWarning = stringResource(R.string.dialpad_hint_emergency_calling_not_available),
    addPause = stringResource(R.string.add_2sec_pause),
    addWait = stringResource(R.string.add_wait),
)
