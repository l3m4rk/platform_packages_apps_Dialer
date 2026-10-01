package com.android.dialer.keypad.ui

/**
 * Fixed text for the keypad's Compose tests. Unit tests have no resource table, so the real
 * [keypadStrings] cannot run there; these stand in, and the real wiring is covered on a device.
 */
internal val TEST_KEYPAD_STRINGS = KeypadStrings(
    voicemailKeyAction = "call voicemail",
    plusKeyAction = "dial plus",
    deleteButton = "backspace",
    overflowButton = "More options",
    call = "Call",
    emergencyCallWarning = "no emergency calls over wifi",
    addPause = "Add 2-sec pause",
    addWait = "Add wait",
    callWithNote = "Call with a note",
    voicemailAirplaneModeError = "Turn off airplane mode to call voicemail.",
    voicemailNotReadyError = "Voicemail is not set up yet.",
    prohibitedNumberError = "This number cannot be dialed.",
    ok = "OK",
)
