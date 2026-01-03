package pe.brice.smsreplay.domain.model

/**
 * Domain model for SMTP configuration
 */
data class SmtpConfig(
    val serverAddress: String = "",
    val port: Int = 587,
    val username: String = "",
    val password: String = "", // Will be encrypted in data layer
    val senderEmail: String = "",
    val recipientEmail: String = "",
    val deviceAlias: String = "" // New: User-defined device name or number
) {
    fun isValid(): Boolean {
        return serverAddress.isNotBlank() &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                recipientEmail.isNotBlank()
    }
}
