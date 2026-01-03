package pe.brice.smsreplay.domain.service

import kotlinx.coroutines.flow.StateFlow
import pe.brice.smsreplay.domain.model.SmsMessage

/**
 * Domain service for SMS queue management
 *
 * Abstracts queue persistence and retry logic, allowing the Domain Layer
 * to manage SMS queue without depending on Android persistence mechanisms.
 *
 * Clean Architecture: Domain Layer defines the interface,
 * Infrastructure Layer (SmsQueueManager) provides implementation.
 */
interface SmsQueueControl {
    /**
     * Observable state of current queue size
     */
    val queueSize: StateFlow<Int>

    /**
     * Enqueue SMS for later retry
     * @param sms SMS message to enqueue
     * @return true if enqueued successfully, false otherwise
     */
    suspend fun enqueue(sms: SmsMessage): Boolean

    /**
     * Mark SMS as sent and remove from queue
     * @param sms SMS message to mark as sent
     * @return true if marked successfully, false otherwise
     */
    suspend fun markAsSent(sms: SmsMessage): Boolean

    /**
     * Increment retry count for SMS
     * @param sms SMS message to increment retry count
     * @return true if incremented successfully, false otherwise
     */
    suspend fun incrementRetryCount(sms: SmsMessage): Boolean

    /**
     * Mark SMS as failed (max retries reached)
     * @param sms SMS message to mark as failed
     * @return true if marked successfully, false otherwise
     */
    suspend fun markAsFailed(sms: SmsMessage): Boolean

    /**
     * Get next pending SMS message from queue without removing it
     * Used for queue flushing operations
     * @return Next pending SMS message, or null if queue is empty
     */
    suspend fun getNextPendingMessage(): SmsMessage?
}
