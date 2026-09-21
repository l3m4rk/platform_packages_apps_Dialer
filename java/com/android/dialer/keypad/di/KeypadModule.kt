package com.android.dialer.keypad.di

import com.android.dialer.keypad.domain.CheckIfNumberIsProhibited
import com.android.dialer.keypad.domain.CheckIfNumberIsProhibitedImpl
import com.android.dialer.keypad.domain.DtmfTonePlayer
import com.android.dialer.keypad.domain.EmergencyCallWarning
import com.android.dialer.keypad.domain.LastOutgoingCall
import com.android.dialer.keypad.domain.PhoneNumberFormatting
import com.android.dialer.keypad.domain.SystemDtmfTonePlayer
import com.android.dialer.keypad.domain.SystemEmergencyCallWarning
import com.android.dialer.keypad.domain.SystemLastOutgoingCall
import com.android.dialer.keypad.domain.SystemPhoneNumberFormatting
import com.android.dialer.keypad.domain.SystemToneGeneratorFactory
import com.android.dialer.keypad.domain.SystemVoicemailAvailability
import com.android.dialer.keypad.domain.ToneGeneratorFactory
import com.android.dialer.keypad.domain.VoicemailAvailability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bindings for the keypad's domain layer.
 *
 * Only adapters that need nothing more than the application context live here. Anything that has
 * to touch an `Activity` — placing a call through `PreCall`, `SpecialCharSequenceMgr`, the error
 * dialogs — is reported by the view model as an effect and carried out by the keypad fragment
 * instead, so that no `Activity` is ever reachable from a `ViewModel`.
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
}
