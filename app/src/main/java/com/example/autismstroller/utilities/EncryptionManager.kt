package com.example.autismstroller.utilities

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val ALGORITHM = "AES"
    // WARNING: In a real production app, store this in C++ NDK or Android Keystore
    private const val APP_SECRET = "MySuperSecretSaltKey2024"

    // Generate a Key specific to the Room ID
    private fun generateKey(roomId: String): SecretKeySpec {
        val combinedKey = roomId + APP_SECRET
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(combinedKey.toByteArray(Charsets.UTF_8))
        // Use only first 16 bytes (128 bit AES)
        return SecretKeySpec(keyBytes.copyOf(16), ALGORITHM)
    }

    fun encrypt(text: String, roomId: String): String {
        try {
            val key = generateKey(roomId)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(text.toByteArray())

            // USE NO_WRAP (Prevents random newlines in the encrypted string)
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }

    fun decrypt(encryptedText: String, roomId: String): String {
        try {
            val key = generateKey(roomId)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key)

            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            // DEBUG LOG: Check your Logcat for "DecryptionError"
            android.util.Log.e("DecryptionError", "Failed to decrypt: ${e.message}")
            return encryptedText
        }
    }
}