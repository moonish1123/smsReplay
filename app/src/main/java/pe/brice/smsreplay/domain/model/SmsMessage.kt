package pe.brice.smsreplay.domain.model

/**
 * Domain model for SMS message
 * Pure Kotlin class without external dependencies
 */
data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long
) {
    /**
     * Format timestamp for display
     */
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Validate SMS message
     */
    fun isValid(): Boolean {
        return sender.isNotBlank() && body.isNotBlank() && timestamp > 0
    }
}
