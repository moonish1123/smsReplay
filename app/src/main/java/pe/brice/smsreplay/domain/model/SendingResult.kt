package pe.brice.smsreplay.domain.model

/**
 * Result of email sending operation
 */
sealed class SendingResult {
    data object Success : SendingResult()
    data class Failure(val error: ErrorType, val message: String) : SendingResult()
    data class Retry(val retryCount: Int, val maxRetries: Int) : SendingResult()

    enum class ErrorType {
        AUTHENTICATION_FAILED,
        NETWORK_ERROR,
        INVALID_RECIPIENT,
        SMTP_ERROR,
        UNKNOWN_ERROR
    }
}

/**
 * Result of queue processing
 */
sealed class QueueResult {
    data object Success : QueueResult()
    data class PartialSuccess(
        val sent: Int,
        val failed: Int,
        val remaining: Int
    ) : QueueResult()
    data class Failure(val error: String) : QueueResult()
}
