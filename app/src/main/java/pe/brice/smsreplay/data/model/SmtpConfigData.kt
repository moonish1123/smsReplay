package pe.brice.smsreplay.data.model

/**
 * Data layer model for SMTP configuration
 * Stored securely using EncryptedSharedPreferences
 */
data class SmtpConfigData(
    val serverAddress: String = "",
    val port: Int = 587, // Default SMTP port
    val username: String = "",
    val password: String = "", // This will be encrypted
    val senderEmail: String = "", // Email to send from
    val recipientEmail: String = "" // Email to send to
) {
    /**
     * Check if config is valid and complete
     */
    fun isValid(): Boolean {
        return serverAddress.isNotBlank() &&
                port in 1..65535 &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                senderEmail.isNotBlank() &&
                recipientEmail.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()
    }

    /**
     * Check if config has been set up (even if not complete)
     */
    fun isConfigured(): Boolean {
        return serverAddress.isNotBlank() ||
                username.isNotBlank() ||
                senderEmail.isNotBlank()
    }
}
