package pe.brice.smtp.sender

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.brice.smtp.model.Email
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * SMTP Mail Sender using AndroidJavaMail
 * Handles email sending with TLS/SSL support
 */
class MailSender(
    private val serverAddress: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {

    companion object {
        private const val TIMEOUT = 30000 // 30 seconds
    }

    /**
     * Send email asynchronously
     * @return Result<Unit> for success or error
     */
    suspend fun sendEmail(email: Email): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate email
            if (!email.isValid()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid email data")
                )
            }

            // Create mail session
            val session = createSession()

            // Create message
            val message = createMimeMessage(session, email)

            // Send message
            Transport.send(message)

            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            Result.failure(
                SmtpException("Authentication failed: Invalid username or password", e)
            )
        } catch (e: SendFailedException) {
            Result.failure(
                SmtpException("Failed to send email: Invalid recipient address", e)
            )
        } catch (e: MessagingException) {
            Result.failure(
                SmtpException("SMTP error: ${e.message}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                SmtpException("Unexpected error: ${e.message}", e)
            )
        }
    }

    /**
     * Create JavaMail Session with authentication
     */
    private fun createSession(): Session {
        val props = Properties().apply {
            // SMTP server settings
            put("mail.smtp.host", serverAddress)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")

            // SSL/TLS settings based on port
            when (port) {
                465 -> {
                    // SSL for port 465 (Daum, Gmail SSL, etc.)
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.ssl.checkserveridentity", "true")
                    put("mail.smtp.socketFactory.port", port.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                }
                587, 25 -> {
                    // TLS (STARTTLS) for port 587 or 25
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                }
            }

            // Timeout settings
            put("mail.smtp.connectiontimeout", TIMEOUT.toString())
            put("mail.smtp.timeout", TIMEOUT.toString())
            put("mail.smtp.writetimeout", TIMEOUT.toString())

            // Debug mode (disable in production)
            // put("mail.debug", "true")
        }

        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })
    }

    /**
     * Create MIME message from email data
     */
    private fun createMimeMessage(session: Session, email: Email): MimeMessage {
        val message = MimeMessage(session).apply {
            // Set sender
            setFrom(InternetAddress(email.from))

            // Set recipient
            setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(email.to)
            )

            // Set subject
            setSubject(email.subject, "UTF-8")

            // Set content (HTML)
            setContent(email.htmlContent, "text/html; charset=UTF-8")

            // Set sent date
            sentDate = java.util.Date()
        }

        return message
    }

    /**
     * Test SMTP connection
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = createSession()
            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                SmtpException("Connection test failed: ${e.message}", e)
            )
        }
    }
}

/**
 * Custom SMTP exception
 */
class SmtpException(message: String, cause: Throwable? = null) : Exception(message, cause)
