package pe.brice.smsreplay.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.brice.smsreplay.data.model.SmtpConfigData

/**
 * Secure preferences manager using EncryptedSharedPreferences
 * Stores SMTP credentials securely using AndroidX Security library
 */
class SecurePreferencesManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_smtp_prefs"

        // Preference keys
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SENDER_EMAIL = "sender_email"
        private const val KEY_RECIPIENT_EMAIL = "recipient_email"

        // Security confirmation
        private const val KEY_SECURITY_CONFIRMED = "security_confirmed"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save SMTP configuration securely
     */
    suspend fun saveSmtpConfig(config: SmtpConfigData) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().apply {
            putString(KEY_SERVER_ADDRESS, config.serverAddress)
            putInt(KEY_PORT, config.port)
            putString(KEY_USERNAME, config.username)
            putString(KEY_PASSWORD, config.password) // Automatically encrypted
            putString(KEY_SENDER_EMAIL, config.senderEmail)
            putString(KEY_RECIPIENT_EMAIL, config.recipientEmail)
        }.apply()
    }

    /**
     * Load SMTP configuration
     */
    suspend fun getSmtpConfig(): SmtpConfigData = withContext(Dispatchers.IO) {
        SmtpConfigData(
            serverAddress = encryptedPrefs.getString(KEY_SERVER_ADDRESS, "") ?: "",
            port = encryptedPrefs.getInt(KEY_PORT, 587),
            username = encryptedPrefs.getString(KEY_USERNAME, "") ?: "",
            password = encryptedPrefs.getString(KEY_PASSWORD, "") ?: "",
            senderEmail = encryptedPrefs.getString(KEY_SENDER_EMAIL, "") ?: "",
            recipientEmail = encryptedPrefs.getString(KEY_RECIPIENT_EMAIL, "") ?: ""
        )
    }

    /**
     * Clear all SMTP configuration
     */
    suspend fun clearSmtpConfig() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Check if SMTP config exists
     */
    suspend fun hasSmtpConfig(): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_USERNAME, null)?.isNotBlank() == true
    }

    /**
     * Save security confirmation
     */
    suspend fun setSecurityConfirmed(confirmed: Boolean) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putBoolean(KEY_SECURITY_CONFIRMED, confirmed).apply()
    }

    /**
     * Check if security was confirmed
     */
    suspend fun isSecurityConfirmed(): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.getBoolean(KEY_SECURITY_CONFIRMED, false)
    }
}
