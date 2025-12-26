package pe.brice.smtp.model

/**
 * Email data transfer object for SMTP sending
 */
data class Email(
    val from: String,           // Sender email
    val to: String,             // Recipient email
    val subject: String,        // Email subject (sender + timestamp)
    val htmlContent: String     // HTML email body
) {
    companion object {
        /**
         * Create email from SMS data
         */
        fun fromSms(
            sender: String,
            body: String,
            timestamp: Long,
            fromEmail: String,
            toEmail: String,
            htmlTemplate: String
        ): Email {
            // Subject format: "01012345678 (2024-12-26 10:30)"
            val formattedTime = formatTimestamp(timestamp)
            val subject = "$sender ($formattedTime)"

            return Email(
                from = fromEmail,
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
                isValidEmail(to)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
