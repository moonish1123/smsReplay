package pe.brice.smsreplay.domain.service

/**
 * Domain service interface for email template generation
 *
 * This interface abstracts the email template building logic, allowing the Domain Layer
 * to remain independent of external libraries (SMTP module).
 *
 * Clean Architecture: Domain Layer defines the interface, Data Layer provides implementation.
 *
 * @see pe.brice.smsreplay.data.service.EmailTemplateServiceImpl
 */
interface EmailTemplateService {
    /**
     * Build HTML email template for SMS forwarding
     *
     * @param sender SMS sender phone number
     * @param body SMS message body
     * @param timestamp Formatted timestamp string
     * @param subject Email subject line
     * @param deviceAlias Device alias for identification
     * @return HTML string for email content
     */
    fun buildSmsTemplate(
        sender: String,
        body: String,
        timestamp: String,
        subject: String,
        deviceAlias: String
    ): String
}
