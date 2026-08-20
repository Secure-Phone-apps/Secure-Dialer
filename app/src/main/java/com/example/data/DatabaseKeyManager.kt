package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val KEY_ALIAS = "DialerDbMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "dialer_secure_prefs"
    private const val ENCRYPTED_DB_KEY = "encrypted_db_key"
    private const val GCM_IV = "gcm_iv"

    @Synchronized
    fun getDatabaseKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_DB_KEY, null)
        val ivBase64 = prefs.getString(GCM_IV, null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            try {
                val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                if (secretKey != null) {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    return cipher.doFinal(encryptedKey)
                }
            } catch (e: Exception) {
                try {
                    prefs.edit().remove(ENCRYPTED_DB_KEY).remove(GCM_IV).apply()
                } catch (_: Exception) {}
            }
        }

        // Generate a new database key
        val secureRandom = SecureRandom()
        val dbKey = ByteArray(32)
        secureRandom.nextBytes(dbKey)

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }

            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedKey = cipher.doFinal(dbKey)

            prefs.edit()
                .putString(ENCRYPTED_DB_KEY, Base64.encodeToString(encryptedKey, Base64.DEFAULT))
                .putString(GCM_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            throw IllegalStateException("AndroidKeyStore hardware security module unavailable or corrupted. Database access halted to protect PII.")
        }

        return dbKey
    }
}
