package com.android.dialer.keypad.model

/** A message the keypad shows in a dialog until the user dismisses it. */
internal enum class KeypadError {
    /** Long-pressed 1 with no voicemail, because the radio is off. */
    VOICEMAIL_AIRPLANE_MODE,

    /** Long-pressed 1 with no voicemail for some other reason, such as a SIM still provisioning. */
    VOICEMAIL_NOT_READY,

    /**
     * The number matches `config_prohibited_phone_number_regexp`, a carrier or test-equipment rule
     * against dialing it by hand. The field has already been cleared.
     */
    PROHIBITED_NUMBER,
}
