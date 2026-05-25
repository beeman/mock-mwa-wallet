package com.solana.mwallet.usecase

import android.content.Context
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object UserAuthenticationUseCase {
    private const val AUTHENTICATED_UNTIL_KEY = "authenticated_until"
    private const val AUTHENTICATION_VALIDITY_MILLIS = 15 * 60 * 1000L
    private const val PREFERENCES_NAME = "user_authentication"

    private val promptInfo: BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Log in to mwallet")
        .setSubtitle("Log in securely access your accounts")
        .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
        .build()

    fun isAuthenticated(context: Context): Boolean =
        getAuthenticatedUntil(context) > System.currentTimeMillis()

    fun unauthenticate(context: Context) {
        getPreferences(context).edit()
            .remove(AUTHENTICATED_UNTIL_KEY)
            .apply()
    }

    fun authenticate(fragment: Fragment, callback: (BiometricPrompt.AuthenticationResult?, Error?) -> Unit) {
        val context = fragment.requireContext()
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(fragment, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                callback.invoke(null, Error(errString.toString()))
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                markAuthenticated(context)
                callback.invoke(result, null)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback.invoke(null, null)
            }
        })
        biometricPrompt.authenticate(promptInfo)
    }

    fun authenticate(activity: FragmentActivity, callback: (BiometricPrompt.AuthenticationResult?, Error?) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                callback.invoke(null, Error(errString.toString()))
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                markAuthenticated(activity)
                callback.invoke(result, null)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback.invoke(null, null)
            }
        })
        biometricPrompt.authenticate(promptInfo)
    }

    private fun getAuthenticatedUntil(context: Context): Long =
        getPreferences(context).getLong(AUTHENTICATED_UNTIL_KEY, 0L)

    private fun getPreferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun markAuthenticated(context: Context) {
        getPreferences(context).edit()
            .putLong(AUTHENTICATED_UNTIL_KEY, System.currentTimeMillis() + AUTHENTICATION_VALIDITY_MILLIS)
            .apply()
    }
}
