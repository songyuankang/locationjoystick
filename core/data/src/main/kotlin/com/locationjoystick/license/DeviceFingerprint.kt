package com.locationjoystick.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceFingerprint {
    /**
     * Computes a stable, unique device fingerprint using:
     * SHA-256(packageName + ":" + Settings.Secure.ANDROID_ID)
     *
     * Returns a 64-character lowercase hex string. Does not request any sensitive permissions.
     */
    fun getFingerprint(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val rawString = "${context.packageName}:$androidId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawString.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Formats the fingerprint for safe UI display (e.g. "a1b2c3d4...e5f6g7h8").
     */
    fun getMaskedFingerprint(fingerprint: String): String {
        if (fingerprint.length <= 16) return fingerprint
        return "${fingerprint.take(8)}...${fingerprint.takeLast(8)}"
    }

    /**
     * Copies the full fingerprint string to system clipboard.
     */
    fun copyToClipboard(
        context: Context,
        fingerprint: String,
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Device Fingerprint", fingerprint)
        clipboard?.setPrimaryClip(clip)
    }
}
