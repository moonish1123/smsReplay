package pe.brice.smsreplay.domain.usecase

import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.service.SmsQueueControl
import timber.log.Timber

/**
 * Use case for handling SMS sending result
 *
 * Contains business logic for retry decisions and queue management.
 * Extracted from Service Layer to Domain Layer for better testability.
 *
 * Clean Architecture: Domain Layer business logic
 * Responsibility: Only enqueue failed messages, no processing logic
 */
class HandleSmsSendingResultUseCase(
    private val smsQueueControl: SmsQueueControl
) {
    /**
     * Handle sending result with business logic:
     * - Success: Return success (queue flushing handled by Service)
     * - Failure: Enqueue for retry (queue flushing handled by Service)
     * - Retry: Enqueue for explicit retry (queue flushing handled by Service)
     *
     * @param sms SMS message that was sent
     * @param result Sending result from email service
     * @return HandleResult indicating outcome
     */
    suspend operator fun invoke(
        sms: SmsMessage,
        result: SendingResult
    ): HandleResult {
        return when (result) {
            is SendingResult.Success -> {
                Timber.i("✅ SMS sent successfully from ${sms.sender}")
                HandleResult.Success(sms)
            }
            is SendingResult.Failure -> {
                // Enqueue for later retry
                val enqueued = smsQueueControl.enqueue(sms)
                if (enqueued) {
                    Timber.w("⚠️ SMS sending failed from ${sms.sender}, enqueued for retry: ${result.message}")
                    HandleResult.Queued(sms, "Failed: ${result.message}")
                } else {
                    Timber.e("❌ Failed to enqueue SMS from ${sms.sender}")
                    HandleResult.Error(sms, "Failed to enqueue")
                }
            }
            is SendingResult.Retry -> {
                // Explicitly requested retry
                val enqueued = smsQueueControl.enqueue(sms)
                if (enqueued) {
                    Timber.i("⏰ SMS from ${sms.sender} scheduled for retry")
                    HandleResult.Queued(sms, "Scheduled retry")
                } else {
                    Timber.e("❌ Failed to enqueue SMS from ${sms.sender} for retry")
                    HandleResult.Error(sms, "Failed to enqueue for retry")
                }
            }
        }
    }

    /**
     * Result of handling SMS sending
     */
    sealed class HandleResult {
        data class Success(val sms: SmsMessage) : HandleResult()
        data class Queued(val sms: SmsMessage, val reason: String) : HandleResult()
        data class Error(val sms: SmsMessage, val message: String) : HandleResult()
    }
}
