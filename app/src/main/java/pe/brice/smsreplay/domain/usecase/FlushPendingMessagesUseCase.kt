package pe.brice.smsreplay.domain.usecase

import kotlinx.coroutines.coroutineScope
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.service.SmsQueueControl
import timber.log.Timber

/**
 * Use case for flushing pending SMS messages
 *
 * Coordinates queue flushing when network is restored or email sending succeeds.
 * Processes all pending messages in queue until empty or first error.
 *
 * Clean Architecture: Domain Layer business logic for queue management
 */
class FlushPendingMessagesUseCase(
    private val smsQueueControl: SmsQueueControl,
    private val sendSmsAsEmailUseCase: SendSmsAsEmailUseCase
) {
    /**
     * Flush all pending messages from queue
     *
     * Processes messages one by one until:
     * - Queue is empty
     * - First error occurs (to prevent spamming failures)
     *
     * @return FlushResult indicating outcome
     */
    suspend operator fun invoke(): FlushResult {
        return try {
            Timber.i("Starting to flush SMS queue...")

            var processedCount = 0
            var hasMessages = true

            // Process messages until queue is empty or error occurs
            while (hasMessages) {
                // Try to get next message from queue
                // Note: We need a way to check if there are messages without removing them
                // Since SmsQueueControl doesn't provide peek(), we'll use a different approach

                // For now, we'll use a simple strategy: process until no more messages
                // In a real implementation, we might add a peek() method to SmsQueueControl
                val nextMessage = getNextPendingMessage() ?: break

                Timber.d("Flushing: Processing SMS from ${nextMessage.sender}")

                // Try to send the message
                val result = sendSmsAsEmailUseCase(nextMessage)

                when (result) {
                    is SendingResult.Success -> {
                        Timber.i("✅ Flush: Sent SMS from ${nextMessage.sender}")
                        smsQueueControl.markAsSent(nextMessage)
                        processedCount++
                    }
                    is SendingResult.Failure -> {
                        Timber.w("⚠️ Flush interrupted: Failed to send SMS from ${nextMessage.sender}. Stopping flush.")
                        smsQueueControl.incrementRetryCount(nextMessage)
                        return FlushResult.Partial(processedCount) // Stop on first error
                    }
                    is SendingResult.Retry -> {
                        Timber.w("⚠️ Flush: Retry requested for ${nextMessage.sender}")
                        smsQueueControl.incrementRetryCount(nextMessage)
                        return FlushResult.Partial(processedCount)
                    }
                }
            }

            Timber.i("✅ Queue flush completed: $processedCount messages sent")

            if (processedCount == 0) {
                return FlushResult.Empty
            } else {
                return FlushResult.Success(processedCount)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error during queue flush")
            return FlushResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get next pending message from queue
     * Uses SmsQueueControl interface method
     */
    private suspend fun getNextPendingMessage(): SmsMessage? {
        return smsQueueControl.getNextPendingMessage()
    }

    /**
     * Result of flushing pending messages
     */
    sealed class FlushResult {
        object Empty : FlushResult()
        data class Success(val processed: Int) : FlushResult()
        data class Partial(val processed: Int) : FlushResult()
        data class Error(val message: String) : FlushResult()
    }
}
