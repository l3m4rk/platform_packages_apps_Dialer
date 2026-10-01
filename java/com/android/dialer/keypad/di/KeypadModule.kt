package com.android.dialer.keypad.di

import com.android.dialer.keypad.domain.CallWithNoteAvailability
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibited
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibitedImpl
import com.android.dialer.keypad.domain.DialIntentNumber
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.LastOutgoingCall
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.SystemCallWithNoteAvailability
import com.android.dialer.keypad.domain.SystemDialIntentNumber
import com.android.dialer.keypad.domain.SystemDtmfTonePlayer
import com.android.dialer.keypad.domain.SystemEmergencyCallWarning
import com.android.dialer.keypad.domain.SystemLastOutgoingCall
import com.android.dialer.keypad.domain.SystemPhoneNumberFormatting
import com.android.dialer.keypad.domain.SystemToneGeneratorFactory
import com.android.dialer.keypad.domain.SystemVibration
import com.android.dialer.keypad.domain.SystemVoicemailAvailability
import com.android.dialer.keypad.domain.ToneGeneratorFactory
import com.android.dialer.keypad.domain.Vibration
import com.android.dialer.keypad.domain.VoicemailAvailability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Application-context bindings only. What needs an `Activity` (`PreCall`, `SpecialCharSequenceMgr`)
 * is an effect carried out by the fragment, never a binding.
 */
@Suppress("Unused")
@Module
@InstallIn(SingletonComponent::class)
internal interface KeypadModule {

    @Binds
    fun bindDtmfTonePlayer(player: SystemDtmfTonePlayer): DtmfTonePlayer

    @Binds
    fun bindToneGeneratorFactory(factory: SystemToneGeneratorFactory): ToneGeneratorFactory

    @Binds
    fun bindVoicemailAvailability(availability: SystemVoicemailAvailability): VoicemailAvailability

    @Binds
    fun bindLastOutgoingCall(lastOutgoingCall: SystemLastOutgoingCall): LastOutgoingCall

    @Binds
    fun bindEmergencyCallWarning(warning: SystemEmergencyCallWarning): EmergencyCallWarning

    @Binds
    fun bindPhoneNumberFormatting(formatting: SystemPhoneNumberFormatting): PhoneNumberFormatting

    @Binds
    fun bindCheckIfNumberIsProhibited(
        check: CheckIfNumberIsProhibitedImpl,
    ): CheckIfNumberIsProhibited

    @Binds
    fun bindDialIntentNumber(number: SystemDialIntentNumber): DialIntentNumber

    @Binds
    fun bindVibration(vibration: SystemVibration): Vibration

    @Binds
    fun bindCallWithNoteAvailability(
        availability: SystemCallWithNoteAvailability,
    ): CallWithNoteAvailability
}
