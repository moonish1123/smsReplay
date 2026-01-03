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
     * Tries TLS first, falls back to SSL if needed
     * @return Result<Unit> for success or error
     */
    suspend fun sendEmail(email: Email): Result<Unit> = withContext(Dispatchers.IO) {
        // Validate email
        if (!email.isValid()) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid email data")
            )
        }

        // Try TLS first (preferred), then SSL fallback
        val tlsResult: Result<Unit> = trySendWithEmail(email, useSsl = false)

        tlsResult.fold(
            onSuccess = { Result.success(it) },
            onFailure = { tlsException ->
                // TLS failed, try SSL as fallback
                val sslResult: Result<Unit> = trySendWithEmail(email, useSsl = true)
                sslResult.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { sslException ->
                        // Both failed, return the original TLS error with SSL context
                        Result.failure(
                            SmtpException(
                                "TLS failed: ${tlsException.message}. SSL fallback also failed: ${sslException.message}",
                                tlsException
                            )
                        )
                    }
                )
            }
        )
    }

    /**
     * Try sending email with specified SSL/TLS mode
     */
    private suspend fun trySendWithEmail(email: Email, useSsl: Boolean): Result<Unit> {
        return try {
            val session = createSession(useSsl)
            val message = createMimeMessage(session, email)
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
                SmtpException("SMTP ${if (useSsl) "SSL" else "TLS"} error: ${e.message}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                SmtpException("Unexpected error: ${e.message}", e)
            )
        }
    }

    /**
     * Create JavaMail Session with authentication
     * Tries TLS first, falls back to SSL if needed
     */
    private fun createSession(useSsl: Boolean = false): Session {
        val props = Properties().apply {
            // SMTP server settings
            put("mail.smtp.host", serverAddress)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")

            // SSL/TLS settings
            if (useSsl) {
                // SSL mode (for port 465 or fallback)
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.ssl.checkserveridentity", "true")
                put("mail.smtp.socketFactory.port", port.toString())
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                // TLS mode (STARTTLS) - preferred for security
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            // Force TLSv1.2 or higher for security (disable SSLv3)
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")

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
            // Set sender with UTF-8 encoding for personal name
            if (email.fromName.isNotBlank()) {
                setFrom(InternetAddress(email.from, email.fromName, "UTF-8"))
            } else {
                setFrom(InternetAddress(email.from))
            }

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
     * Tries TLS first, falls back to SSL if needed
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        // Try TLS first (preferred), then SSL fallback
        val tlsResult: Result<Unit> = tryConnect(useSsl = false)

        tlsResult.fold(
            onSuccess = { Result.success(it) },
            onFailure = { tlsException ->
                // TLS failed, try SSL as fallback
                val sslResult: Result<Unit> = tryConnect(useSsl = true)
                sslResult.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { sslException ->
                        // Both failed, return the original TLS error with SSL context
                        Result.failure(
                            SmtpException(
                                "TLS failed: ${tlsException.message}. SSL fallback also failed: ${sslException.message}",
                                tlsException
                            )
                        )
                    }
                )
            }
        )
    }

    /**
     * Try connection with specified SSL/TLS mode
     */
    private suspend fun tryConnect(useSsl: Boolean): Result<Unit> {
        return try {
            val session = createSession(useSsl)
            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                SmtpException("SMTP ${if (useSsl) "SSL" else "TLS"} connection failed: ${e.message}", e)
            )
        }
    }
}

/**
 * Custom SMTP exception
 */
class SmtpException(message: String, cause: Throwable? = null) : Exception(message, cause)
