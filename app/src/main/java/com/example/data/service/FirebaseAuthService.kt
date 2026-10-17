package com.example.data.service

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class PhoneVerificationEvent {
    data class CodeSent(
        val verificationId: String,
        val token: PhoneAuthProvider.ForceResendingToken?
    ) : PhoneVerificationEvent()

    data class VerificationCompleted(
        val credential: PhoneAuthCredential
    ) : PhoneVerificationEvent()

    data class VerificationFailed(
        val error: PhoneAuthException
    ) : PhoneVerificationEvent()
}

class PhoneAuthException(
    val userFacingMessage: String,
    val technicalDetails: String? = null,
    cause: Throwable? = null
) : Exception(userFacingMessage, cause)

/**
 * Real Firebase Phone Authentication Service for 1campus.
 *
 * Implements genuine carrier SMS dispatch via Firebase PhoneAuthProvider,
 * ForceResendingToken handling for resend cooldowns, provider verification ID tracking,
 * and robust error mapping for rate limits, invalid formats, and unconfigured states.
 */
class FirebaseAuthService(private val context: Context) {

    // In-memory cache of ForceResendingToken mapped by normalized E.164 phone number
    private val resendingTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()

    // Optional test engine hook for local Robolectric JVM tests
    var testVerificationEngine: ((phoneNumber: String, onEvent: (PhoneVerificationEvent) -> Unit) -> Unit)? = null
    var testCredentialEngine: ((verificationId: String, code: String) -> Result<PhoneAuthCredential>)? = null
    var testSignInEngine: ((credential: PhoneAuthCredential) -> Result<FirebaseUser?>)? = null

    fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            if (isFirebaseInitialized()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Dispatches real SMS OTP verification code via Firebase PhoneAuthProvider.
     */
    fun sendVerificationCode(
        activity: Activity?,
        normalizedPhone: String,
        forceResend: Boolean = false,
        onEvent: (PhoneVerificationEvent) -> Unit
    ) {
        // If a test engine hook is configured (for JVM Robolectric unit tests), use it
        if (testVerificationEngine != null) {
            testVerificationEngine!!.invoke(normalizedPhone, onEvent)
            return
        }

        val auth = getFirebaseAuth()
        if (auth == null) {
            onEvent(
                PhoneVerificationEvent.VerificationFailed(
                    PhoneAuthException(
                        userFacingMessage = "Firebase Phone Authentication is not configured yet. Please add google-services.json to the /app directory and enable the Phone sign-in provider in Firebase Console.",
                        technicalDetails = "FirebaseApp is uninitialized or google-services.json is missing."
                    )
                )
            )
            return
        }

        if (activity == null) {
            onEvent(
                PhoneVerificationEvent.VerificationFailed(
                    PhoneAuthException(
                        userFacingMessage = "An active screen context is required for app security verification (SafetyNet / reCAPTCHA).",
                        technicalDetails = "Activity reference was null when calling sendVerificationCode."
                    )
                )
            )
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Safely retain force resending token for subsequent resend attempts
                resendingTokens[normalizedPhone] = token
                onEvent(PhoneVerificationEvent.CodeSent(verificationId, token))
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant device verification or SMS auto-retrieval
                onEvent(PhoneVerificationEvent.VerificationCompleted(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val error = mapFirebaseException(e)
                onEvent(PhoneVerificationEvent.VerificationFailed(error))
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(normalizedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (forceResend) {
                val token = resendingTokens[normalizedPhone]
                if (token != null) {
                    optionsBuilder.setForceResendingToken(token)
                }
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            onEvent(
                PhoneVerificationEvent.VerificationFailed(
                    PhoneAuthException(
                        userFacingMessage = "Failed to dispatch SMS request: ${e.localizedMessage ?: "Unknown error"}",
                        technicalDetails = e.stackTraceToString(),
                        cause = e
                    )
                )
            )
        }
    }

    /**
     * Verifies the user-entered 6-digit SMS code against the provider verification ID.
     */
    suspend fun verifyCode(
        verificationId: String,
        smsCode: String
    ): Result<PhoneAuthCredential> = withContext(Dispatchers.IO) {
        if (testCredentialEngine != null) {
            return@withContext testCredentialEngine!!.invoke(verificationId, smsCode)
        }

        if (verificationId.isBlank()) {
            return@withContext Result.failure(
                PhoneAuthException("Missing verification session ID. Please request a new verification code.")
            )
        }
        val trimmed = smsCode.trim()
        if (trimmed.length != 6 || !trimmed.all { it.isDigit() }) {
            return@withContext Result.failure(
                PhoneAuthException("Please enter a valid 6-digit SMS verification code.")
            )
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, trimmed)
            Result.success(credential)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Signs in with the provider credential to confirm phone ownership in Firebase.
     */
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        if (testSignInEngine != null) {
            return@withContext testSignInEngine!!.invoke(credential)
        }

        val auth = getFirebaseAuth()
            ?: return@withContext Result.failure(
                PhoneAuthException("Firebase is not initialized.")
            )

        try {
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Invalidates any cached resend token and cleans up pending state for the phone number.
     */
    fun invalidateSession(normalizedPhone: String) {
        resendingTokens.remove(normalizedPhone)
    }

    fun hasResendingToken(normalizedPhone: String): Boolean {
        return resendingTokens.containsKey(normalizedPhone)
    }

    private fun mapFirebaseException(e: Exception): PhoneAuthException {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                val msg = e.message ?: ""
                if (msg.contains("code", ignoreCase = true) || msg.contains("otp", ignoreCase = true)) {
                    PhoneAuthException(
                        userFacingMessage = "The 6-digit verification code entered is incorrect or expired. Please check your SMS and try again.",
                        technicalDetails = e.message,
                        cause = e
                    )
                } else {
                    PhoneAuthException(
                        userFacingMessage = "The phone number format is invalid for this region. Please verify the country dial code and phone digits.",
                        technicalDetails = e.message,
                        cause = e
                    )
                }
            }
            is FirebaseTooManyRequestsException -> {
                PhoneAuthException(
                    userFacingMessage = "Too many SMS requests from this device or project. Please wait a few minutes before trying again.",
                    technicalDetails = e.message,
                    cause = e
                )
            }
            else -> {
                val msg = e.localizedMessage ?: "An unexpected authentication error occurred."
                val userMessage = when {
                    msg.contains("reCAPTCHA", ignoreCase = true) || msg.contains("Play Integrity", ignoreCase = true) ->
                        "Device integrity check failed. Ensure Google Play Services is available and device time is accurate."
                    msg.contains("quota", ignoreCase = true) ->
                        "SMS quota exceeded for this Firebase project. Please check Firebase console quotas."
                    msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true) ->
                        "Network connection failure. Please verify your internet access and retry."
                    msg.contains("app-not-authorized", ignoreCase = true) || msg.contains("fingerprint", ignoreCase = true) ->
                        "App is not authorized for Firebase Phone Auth. SHA-1 or SHA-256 fingerprint must be added to Firebase Console."
                    else -> msg
                }
                PhoneAuthException(
                    userFacingMessage = userMessage,
                    technicalDetails = e.message,
                    cause = e
                )
            }
        }
    }
}
