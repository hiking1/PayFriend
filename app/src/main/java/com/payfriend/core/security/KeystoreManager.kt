package com.payfriend.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore wrapper. Keys are AES-256-GCM and never leave the secure hardware.
 *
 * Two keys are used:
 *  - a *storage* key for encrypting small sensitive local values ([encrypt] / [decrypt]);
 *  - an *auth* key that can only be used right after a strong biometric check
 *    (see [BiometricAuthManager]), which is what makes biometric confirmation meaningful.
 *
 * StrongBox is requested where supported and silently falls back to the TEE otherwise.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    /** Encrypts with the storage key. Output layout: IV (12 bytes) followed by ciphertext+tag. */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(STORAGE_KEY_ALIAS, authRequired = false))
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > GCM_IV_BYTES) { "Ciphertext too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(STORAGE_KEY_ALIAS, authRequired = false),
            GCMParameterSpec(GCM_TAG_BITS, blob, 0, GCM_IV_BYTES)
        )
        return cipher.doFinal(blob, GCM_IV_BYTES, blob.size - GCM_IV_BYTES)
    }

    /**
     * Returns an initialised encrypt cipher bound to the auth key. It becomes usable only after the
     * user passes BiometricPrompt with this cipher as the CryptoObject.
     *
     * @throws android.security.keystore.KeyPermanentlyInvalidatedException if biometrics were re-enrolled.
     */
    fun newAuthCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(AUTH_KEY_ALIAS, authRequired = true))
        return cipher
    }

    fun deleteAuthKey() {
        if (keyStore.containsAlias(AUTH_KEY_ALIAS)) keyStore.deleteEntry(AUTH_KEY_ALIAS)
    }

    private fun getOrCreateKey(alias: String, authRequired: Boolean): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return try {
            generateKey(alias, authRequired, strongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        } catch (e: ProviderException) {
            // StrongBoxUnavailableException extends ProviderException: retry without StrongBox.
            generateKey(alias, authRequired, strongBox = false)
        }
    }

    private fun generateKey(alias: String, authRequired: Boolean, strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (authRequired) {
            builder.setUserAuthenticationRequired(true)
            builder.setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Timeout 0 = authentication is required for every single use of the key.
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            }
        }
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val STORAGE_KEY_ALIAS = "payfriend_storage_key"
        const val AUTH_KEY_ALIAS = "payfriend_auth_key"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
