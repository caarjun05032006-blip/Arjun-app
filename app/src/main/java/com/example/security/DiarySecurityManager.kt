package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object DiarySecurityManager {
    private const val PREFS_NAME = "diary_security_prefs"
    private const val KEY_PIN_HASH = "diary_pin_hash"
    private const val KEY_SALT = "diary_salt"
    private const val KEY_IS_SETUP = "diary_is_setup"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private var sessionKey: ByteArray? = null
    var isUnlocked: Boolean = false
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPinConfigured(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SETUP, false)
    }

    fun setupPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = hashPinWithSalt(pin, salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_SALT, saltBase64)
            .putBoolean(KEY_IS_SETUP, true)
            .apply()

        unlockWithPin(context, pin)
        return true
    }

    fun unlockWithPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

        val candidateHash = hashPinWithSalt(pin, salt)
        if (candidateHash == savedHash) {
            // Derive 256-bit AES key from pin + salt
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            sessionKey = digest.digest(pin.toByteArray(Charsets.UTF_8))
            isUnlocked = true
            return true
        }
        return false
    }

    fun lock() {
        sessionKey = null
        isUnlocked = false
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashed = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashed, Base64.NO_WRAP)
    }

    data class EncryptedResult(val cipherTextBase64: String, val ivBase64: String)

    fun encrypt(plainText: String): EncryptedResult {
        val keyBytes = sessionKey ?: throw IllegalStateException("Diary is locked")
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptedResult(
            cipherTextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(cipherTextBase64: String, ivBase64: String): String {
        val keyBytes = sessionKey ?: return "[[LOCKED CONTENT]]"
        try {
            val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherBytes)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return "[[Decryption Error]]"
        }
    }
}
