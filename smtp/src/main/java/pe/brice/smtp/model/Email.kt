package pe.brice.smtp.model

/**
 * Email data transfer object for SMTP sending
 * Supports multiple recipients separated by comma
 */
data class Email(
    val from: String,           // Sender email
    val to: String,             // Recipient emails (comma-separated)
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

            // From format: "01012345678 <email@domain.com>"
            val fromAddress = "\"$sender\" <$fromEmail>"

            return Email(
                from = fromAddress,
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
     * Supports multiple recipients separated by comma or semicolon
     * Supports "Name <email>" format for sender
     */
    fun isValid(): Boolean {
        return from.isNotBlank() &&
                to.isNotBlank() &&
                subject.isNotBlank() &&
                htmlContent.isNotBlank() &&
                isValidFromEmail(from) &&
                areRecipientEmailsValid()
    }

    /**
     * Validate sender email
     * Supports both "email@domain.com" and "Name <email@domain.com>" formats
     */
    private fun isValidFromEmail(email: String): Boolean {
        val trimmed = email.trim()

        // Extract email from "Name <email@domain.com>" format
        val emailOnly = when {
            trimmed.contains("<") && trimmed.contains(">") -> {
                // Extract part between < and >
                val start = trimmed.indexOf("<")
                val end = trimmed.indexOf(">")
                if (start < end) trimmed.substring(start + 1, end).trim() else trimmed
            }
            else -> trimmed
        }

        return android.util.Patterns.EMAIL_ADDRESS.matcher(emailOnly).matches()
    }

    /**
     * Validate all recipient emails
     * Supports multiple emails separated by comma or semicolon
     */
    private fun areRecipientEmailsValid(): Boolean {
        return to.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .all { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }
    }
}
