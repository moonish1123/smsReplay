package pe.brice.smsreplay.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.usecase.SendSmsAsEmailUseCase
import timber.log.Timber

/**
 * WorkManager Worker for retrying failed SMS email sends
 * Implements exponential backoff: 1s, 5s, 10s
 */
class SmsRetryWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        const val WORK_NAME_PREFIX = "sms_retry_"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_RETRY_COUNT = "retry_count"

        /**
         * Calculate backoff delay: 1s, 5s, 10s for retries 0, 1, 2
         */
        fun getBackoffDelay(retryCount: Int): Long {
            return when (retryCount) {
                0 -> 1_000L      // 1 second
                1 -> 5_000L      // 5 seconds
                2 -> 10_000L     // 10 seconds
                else -> 10_000L   // Max 10 seconds
            }
        }

        /**
         * Get max retries (3 total attempts)
         */
        fun getMaxRetries(): Int = 3
    }

    private val sendSmsAsEmailUseCase: SendSmsAsEmailUseCase by inject()
    private val smsQueueControl: pe.brice.smsreplay.domain.service.SmsQueueControl by inject()

    override suspend fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        val retryCount = inputData.getInt(KEY_RETRY_COUNT, 0)

        Timber.d("Retry worker executing: attempt ${retryCount + 1}/${getMaxRetries()} for SMS from $sender")

        return try {
            val sms = pe.brice.smsreplay.domain.model.SmsMessage(
                sender = sender,
                body = body,
                timestamp = timestamp
            )

            val result = sendSmsAsEmailUseCase(sms)

            when (result) {
                is pe.brice.smsreplay.domain.model.SendingResult.Success -> {
                    Timber.i("Email sent successfully on retry attempt ${retryCount + 1}")
                    // ✅ Remove from queue to prevent duplicate sends
                    smsQueueControl.markAsSent(sms)
                    Result.success()
                }
                is pe.brice.smsreplay.domain.model.SendingResult.Failure -> {
                    val failure = result
                    Timber.e("Email failed: ${failure.message}")

                    // Check if should retry
                    if (retryCount < getMaxRetries() - 1 && isRetryableError(failure)) {
                        // ✅ Update retry count in queue
                        smsQueueControl.incrementRetryCount(sms)
                        Timber.w("Scheduling next retry (attempt ${retryCount + 2}/${getMaxRetries()})")
                        Result.retry()
                    } else {
                        // ✅ Mark as permanently failed (max retries reached)
                        Timber.e("Max retries reached or non-retryable error")
                        smsQueueControl.markAsFailed(sms)
                        Result.failure()
                    }
                }
                is pe.brice.smsreplay.domain.model.SendingResult.Retry -> {
                    val retryState = result
                    Timber.w("Use case requested retry: ${retryState.retryCount}/${retryState.maxRetries}")

                    if (retryState.retryCount < retryState.maxRetries) {
                        // ✅ Update retry count in queue
                        smsQueueControl.incrementRetryCount(sms)
                        Result.retry()
                    } else {
                        // ✅ Max retries reached
                        smsQueueControl.markAsFailed(sms)
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in retry worker")
            Result.failure()
        }
    }

    /**
     * Determine if error type is retryable
     */
    private fun isRetryableError(failure: pe.brice.smsreplay.domain.model.SendingResult.Failure): Boolean {
        return when (failure.error) {
            pe.brice.smsreplay.domain.model.SendingResult.ErrorType.NETWORK_ERROR,
            pe.brice.smsreplay.domain.model.SendingResult.ErrorType.SMTP_ERROR -> true

            else -> false
        }
    }
}
