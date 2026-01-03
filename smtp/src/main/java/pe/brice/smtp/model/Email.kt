package pe.brice.smtp.model

import javax.mail.internet.InternetAddress

/**
 * Email data transfer object for SMTP sending
 * Supports multiple recipients separated by comma
 */
data class Email(
    val from: String,           // Sender email address
    val fromName: String = "",  // Sender display name (will be encoded)
    val to: String,             // Recipient emails (comma-separated)
    val subject: String,        // Email subject
    val htmlContent: String     // HTML email body
) {
    companion object {
        /**
         * Create email from SMS data
         */
        fun fromSms(
            sender: String,
            deviceAlias: String,
            timestamp: Long,
            fromEmail: String,
            toEmail: String,
            htmlTemplate: String
        ): Email {
            // Subject format: "01012345678 (2024-12-26 10:30)"
            val formattedTime = formatTimestamp(timestamp)
            val subject = "$sender => $deviceAlias at ($formattedTime)"

            return Email(
                from = fromEmail,
                fromName = "$sender ($deviceAlias)", // Separate name from address
                to = toEmail,
                subject = subject,
                htmlContent = htmlTemplate
            )
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    /**
     * Validate email fields
     */
    fun isValid(): Boolean {
        return from.isNotBlank() &&
                to.isNotBlank() &&
                subject.isNotBlank() &&
                htmlContent.isNotBlank() &&
                isValidEmail(from) &&
                areRecipientEmailsValid()
    }

    /**
     * Validate email using InternetAddress
     */
    private fun isValidEmail(email: String): Boolean {
        return try {
            InternetAddress(email).validate()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate all recipient emails using InternetAddress
     * Supports multiple emails separated by comma or semicolon
     */
    private fun areRecipientEmailsValid(): Boolean {
        return try {
            to.split(",", ";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { InternetAddress(it) }
                .onEach { it.validate() }
                .let { true }
        } catch (e: Exception) {
            false
        }
    }
}
