package pe.brice.smsreplay.domain.model

/**
 * Domain model for SMTP configuration
 */
data class SmtpConfig(
    val serverAddress: String,
    val port: Int,
    val username: String,
    val password: String, // This will be encrypted in storage
    val senderEmail: String,
    val recipientEmail: String
) {
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

    fun isConfigured(): Boolean {
        return serverAddress.isNotBlank() ||
                username.isNotBlank() ||
                senderEmail.isNotBlank()
    }
}
