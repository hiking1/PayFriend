package com.payfriend.core.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strong-biometric confirmation that is cryptographically bound to the Keystore.
 *
 * The prompt is created with a [BiometricPrompt.CryptoObject]; success is only reported after
 * the unlocked cipher has actually been used. A hooked callback alone therefore cannot fake it.
 *
 * Note: this proves the *device* authenticated the user. For production, bind this to a
 * server-verified challenge (e.g. signed with a Keystore key attested to the backend).
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    private val keystore: KeystoreManager
) {
    private val random = SecureRandom()

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "Verify your identity to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            keystore.newAuthCipher()
        } catch (e: KeyPermanentlyInvalidatedException) {
            keystore.deleteAuthKey() // biometrics changed: the old key is dead, a new one is made next time
            onError("Your biometrics changed. Please try again to re-secure the app.")
            return
        } catch (e: Exception) {
            onError("Secure key unavailable. Set up a screen lock and biometrics, then try again.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val unlocked = result.cryptoObject?.cipher
                    val proven = try {
                        // Using the key proves the Keystore itself released it after authentication.
                        unlocked?.doFinal(ByteArray(16).also(random::nextBytes))
                        unlocked != null
                    } catch (e: Exception) {
                        false
                    }
                    if (proven) onSuccess() else onError("Biometric verification failed.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}
