package pe.brice.smsreplay.work

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.repository.SmsQueueRepository
import timber.log.Timber

/**
 * Manager for SMS queue and retry logic
 * Integrates Room database with WorkManager for offline retry
 */
class SmsQueueManager(
    private val context: Context
) : KoinComponent {

    private val smsQueueRepository: SmsQueueRepository by inject()
    private val retryScheduler = SmsRetryScheduler(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    init {
        // Observe queue size changes
        scope.launch {
            smsQueueRepository.getAllPendingSms().collect { pendingSmsList ->
                _queueSize.value = pendingSmsList.size
                Timber.d("Queue size updated: ${pendingSmsList.size}")
            }
        }
    }

    /**
     * Enqueue SMS for sending
     * Returns true if enqueued successfully
     */
    suspend fun enqueue(sms: SmsMessage): Boolean {
        return try {
            val id = smsQueueRepository.enqueue(sms)
            if (id > 0) {
                Timber.i("SMS enqueued successfully (ID: $id) from ${sms.sender}")
                // Trigger retry scheduling immediately
                processNext()
                true
            } else {
                Timber.e("Failed to enqueue SMS from ${sms.sender}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error enqueuing SMS from ${sms.sender}")
            false
        }
    }

    /**
     * Process next SMS in queue
     */
    suspend fun processNext(): Boolean {
        val nextSms = smsQueueRepository.getNextPendingSms()
        if (nextSms == null) {
            Timber.d("No SMS in queue to process")
            return false
        }

        Timber.d("Processing next SMS from ${nextSms.sender} (retry count: ${nextSms.retryCount})")

        // Trigger retry worker
        val domainSms = SmsMessage(
            sender = nextSms.sender,
            body = nextSms.body,
            timestamp = nextSms.timestamp
        )

        retryScheduler.scheduleRetry(domainSms, nextSms.retryCount)
        return true
    }

    /**
     * Mark SMS as sent and remove from queue
     */
    suspend fun markAsSent(sms: SmsMessage): Boolean {
        return try {
            // Find and delete from queue
            val pendingSms = smsQueueRepository.findByTimestamp(sms.timestamp)
            if (pendingSms != null) {
                smsQueueRepository.delete(pendingSms.id)
                Timber.i("SMS marked as sent and removed from queue: ${sms.sender}")
                true
            } else {
                Timber.w("SMS not found in queue: ${sms.sender}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error marking SMS as sent: ${sms.sender}")
            false
        }
    }

    /**
     * Increment retry count for SMS
     */
    suspend fun incrementRetryCount(sms: SmsMessage): Boolean {
        return try {
            val pendingSms = smsQueueRepository.findByTimestamp(sms.timestamp)
            if (pendingSms != null) {
                val newCount = pendingSms.retryCount + 1
                smsQueueRepository.updateRetryCount(pendingSms.id, newCount)
                Timber.d("Retry count incremented to $newCount for SMS from ${sms.sender}")
                true
            } else {
                Timber.w("SMS not found in queue for retry update: ${sms.sender}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error incrementing retry count: ${sms.sender}")
            false
        }
    }

    /**
     * Remove failed SMS from queue (max retries reached)
     * For the new "Flush" strategy, we might want to KEEP it in queue instead of deleting,
     * For the new "Flush" strategy, we might want to KEEP it in queue instead of deleting,
     * so it can be picked up later when network is restored.
     * But currently, let's just log it and NOT delete it, effectively "parking" it.
     */
    suspend fun markAsFailed(sms: SmsMessage): Boolean {
        return try {
            Timber.e("SMS max retries reached: ${sms.sender}. Keeping in queue for later flush.")
            // Do NOT delete from queue, so it can be retried later when network is restored
            // smsQueueRepository.delete(pendingSms.id) 
            true
        } catch (e: Exception) {
            Timber.e(e, "Error marking SMS as failed: ${sms.sender}")
            false
        }
    }

    /**
     * Flush queue: Try to send all pending messages immediately
     * Called when a new SMS is successfully sent or network is restored
     */
    suspend fun flushQueue(sendUseCase: pe.brice.smsreplay.domain.usecase.SendSmsAsEmailUseCase) {
        scope.launch {
            try {
                // Get ALL pending messages, not just one
                // We need to fetch them from repository manually because getAllPendingSms() is a Flow
                // Assuming repository has a suspend function to get list. If not, we might need to add it or take from Flow.
                // For now, let's process one by one using a loop until queue is empty or error occurs.
                
                Timber.i("Flushing SMS queue...")
                
                var processedCount = 0
                while (true) {
                    val nextSms = smsQueueRepository.getNextPendingSms() ?: break
                    
                    Timber.d("Flushing: Processing SMS from ${nextSms.sender} (retry: ${nextSms.retryCount})")
                    
                    val domainSms = SmsMessage(
                        sender = nextSms.sender,
                        body = nextSms.body,
                        timestamp = nextSms.timestamp
                    )
                    
                    val result = sendUseCase(domainSms)
                    
                    if (result is pe.brice.smsreplay.domain.model.SendingResult.Success) {
                        markAsSent(domainSms)
                        processedCount++
                    } else {
                        Timber.w("Flush interrupted: Failed to send SMS from ${nextSms.sender}. Stopping flush.")
                        incrementRetryCount(domainSms)
                        break // Stop flushing on first error to prevent spamming failures
                    }
                }
                
                if (processedCount > 0) {
                    Timber.i("Flushed $processedCount messages from queue")
                } else {
                    Timber.d("Queue flush finished (no messages sent)")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error flushing queue")
            }
        }
    }

    /**
     * Clear entire queue
     */
    suspend fun clearQueue(): Boolean {
        return try {
            smsQueueRepository.clearQueue()
            Timber.i("Queue cleared successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error clearing queue")
            false
        }
    }

    /**
     * Cancel all pending retries
     */
    fun cancelAllRetries() {
        retryScheduler.cancelAllRetries()
        Timber.i("All pending retries cancelled")
    }
}
