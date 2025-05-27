package com.example.frontend

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val loginPrefs: SharedPreferences =
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .apply()
    }

    fun getRemember(): Boolean {
        return loginPrefs.getBoolean("remember", false)
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            "login_key_alias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return if (keyStore.containsAlias("login_key_alias")) {
            (keyStore.getEntry("login_key_alias", null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            generateSecretKey()
        }
    }

    fun encryptAndSave(username: String, password: String, remember: Boolean) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val combined = "$username:$password"
        val encrypted = cipher.doFinal(combined.toByteArray(Charsets.UTF_8))

        loginPrefs.edit()
            .putString("credentials", Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString("iv", Base64.encodeToString(iv, Base64.DEFAULT))
            .putBoolean("remember", remember)
            .apply()
    }

    fun decryptCredentials(): Pair<String, String> {
        val encrypted = loginPrefs.getString("credentials", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val iv = loginPrefs.getString("iv", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        val (username, password) = decrypted.split(":", limit = 2)
        return Pair(username, password)
    }

    fun clearLoginData() {
        loginPrefs.edit().clear().apply()
        prefs.edit().clear().apply()
    }
}
