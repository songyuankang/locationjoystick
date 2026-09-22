package com.locationjoystick.license

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LicenseStorage"
private const val KEY_ALIAS = "coco_keygen_license_key_alias"
private const val PREFS_NAME = "coco_secure_license_prefs"
private const val KEY_ENCRYPTED_LICENSE = "encrypted_license_key"
private const val KEY_IV = "encrypted_license_iv"

/**
 * Secure local storage for Keygen License Key using Android Keystore + AES/GCM.
 * Never logs or exposes the plain-text license key.
 */
@Singleton
class LicenseStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun saveLicenseKey(licenseKey: String) {
            try {
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(licenseKey.toByteArray(Charsets.UTF_8))

                val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

                prefs.edit()
                    .putString(KEY_ENCRYPTED_LICENSE, encryptedBase64)
                    .putString(KEY_IV, ivBase64)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save encrypted license key", e)
            }
        }

        fun getLicenseKey(): String? {
            val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_LICENSE, null) ?: return null
            val ivBase64 = prefs.getString(KEY_IV, null) ?: return null

            return try {
                val secretKey = getOrCreateSecretKey()
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val decryptedBytes = cipher.doFinal(encryptedBytes)
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt saved license key", e)
                null
            }
        }

        fun clearLicenseKey() {
            prefs.edit().clear().apply()
        }

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val parameterSpec =
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
    }
