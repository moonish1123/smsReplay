package pe.brice.smsreplay.domain.model

/**
 * Domain model for Email message
 */
data class EmailMessage(
    val from: String,
    val to: String,
    val subject: String,
    val htmlContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val senderPhone: String = ""  // SMS sender phone number for From header
) {
    fun isValid(): Boolean {
        return from.isNotBlank() &&
                to.isNotBlank() &&
                subject.isNotBlank() &&
                htmlContent.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(from).matches() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()
    }
}
