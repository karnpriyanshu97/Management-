package com.example.data.service

import android.content.Context

/**
 * Legacy SmsService facade migrated to real Firebase Phone Authentication.
 *
 * Local OTP generation and local Android notification simulation have been removed.
 * All phone authentication operations are now powered by [FirebaseAuthService].
 */
class SmsService(
    private val context: Context,
    val firebaseAuthService: FirebaseAuthService = FirebaseAuthService(context)
) {
    // Deprecated helpers maintained only for interface compatibility during migration
    fun isProviderReady(): Boolean = firebaseAuthService.isFirebaseInitialized()
}
