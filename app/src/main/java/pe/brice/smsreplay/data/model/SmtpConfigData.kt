package pe.brice.smsreplay.data.model

/**
 * Data layer model for SMTP configuration
 * Stored securely using EncryptedSharedPreferences
 */
data class SmtpConfigData(
    val serverAddress: String = "",
    val port: Int = 587,
    val username: String = "",
    val password: String = "", // Used for transfer, stored encrypted
    val senderEmail: String = "",
    val recipientEmail: String = "",
    val deviceAlias: String = "" // New field
)
