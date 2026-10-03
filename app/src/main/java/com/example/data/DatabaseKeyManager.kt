package com.example.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val KEY_ALIAS = "DialerDbMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "dialer_secure_device_prefs"
    private const val ENCRYPTED_DB_KEY = "encrypted_db_key"
    private const val GCM_IV = "gcm_iv"
    private const val GCM_TAG_LENGTH = 128

    // In-memory key cache to eliminate 50ms Keystore IPC overhead on every DB query
    @Volatile
    private var cachedKey: ByteArray? = null

    @Synchronized
    fun getDatabaseKey(context: Context): ByteArray {
        // 1. Fast-path: return cached key if already unlocked in memory
        cachedKey?.let {
            return it.copyOf()
        }

        // 2. Direct Boot safe context: ensures accessibility across reboots/incoming calls
        val directBootContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }

        val prefs = directBootContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_DB_KEY, null)
        val ivBase64 = prefs.getString(GCM_IV, null)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // 3. Decrypt existing key
        if (encryptedKeyBase64 != null && ivBase64 != null) {
            try {
                val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                if (secretKey != null) {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    val decrypted = cipher.doFinal(encryptedKey)

                    cachedKey = decrypted.copyOf()
                    return decrypted
                }
            } catch (_: Exception) {
                // If hardware key or prefs were corrupted/invalidated, proceed to regenerate fresh key
            }
            // Clear unrecoverable prefs entries
            prefs.edit().remove(ENCRYPTED_DB_KEY).remove(GCM_IV).commit()
        }

        // 4. Generate new 256-bit database passphrase
        val secureRandom = SecureRandom()
        val rawDbKey = ByteArray(32)
        secureRandom.nextBytes(rawDbKey)

        try {
            val secretKey = getOrCreateMasterKey(keyStore)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedKey = cipher.doFinal(rawDbKey)

            prefs.edit()
                .putString(ENCRYPTED_DB_KEY, Base64.encodeToString(encryptedKey, Base64.NO_WRAP))
                .putString(GCM_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit() // Commit immediately to ensure durability before opening DB

            cachedKey = rawDbKey.copyOf()
            return rawDbKey
        } catch (e: Exception) {
            Arrays.fill(rawDbKey, 0.toByte())
            throw IllegalStateException("Hardware-backed master key generation failed.", e)
        }
    }

    private fun getOrCreateMasterKey(keyStore: KeyStore): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        // Try StrongBox HSM first, fallback to standard TEE if unavailable
        return try {
            generateKey(isStrongBox = true)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && e is StrongBoxUnavailableException) {
                generateKey(isStrongBox = false)
            } else {
                generateKey(isStrongBox = false)
            }
        }
    }

    private fun generateKey(isStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isStrongBox) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Call this when the app process is being destroyed or the user locks the vault
     */
    @Synchronized
    fun wipeInMemoryKey() {
        cachedKey?.let {
            Arrays.fill(it, 0.toByte())
            cachedKey = null
        }
    }
}