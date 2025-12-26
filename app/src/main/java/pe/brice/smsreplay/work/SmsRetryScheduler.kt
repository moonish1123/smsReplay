package pe.brice.smsreplay.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import pe.brice.smsreplay.domain.model.SmsMessage
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Scheduler for SMS retry workers
 * Manages exponential backoff and retry logic
 */
class SmsRetryScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule retry for failed SMS email send
     * @param sms SMS message to retry
     * @param retryCount Current retry attempt (0-indexed)
     */
    fun scheduleRetry(sms: SmsMessage, retryCount: Int = 0) {
        if (retryCount >= SmsRetryWorker.getMaxRetries()) {
            Timber.e("Cannot schedule retry: max retries ($retryCount) reached for SMS from ${sms.sender}")
            return
        }

        val workName = "${SmsRetryWorker.WORK_NAME_PREFIX}${sms.timestamp}_${sms.sender}"

        // Input data
        val inputData = Data.Builder()
            .putString(SmsRetryWorker.KEY_SENDER, sms.sender)
            .putString(SmsRetryWorker.KEY_BODY, sms.body)
            .putLong(SmsRetryWorker.KEY_TIMESTAMP, sms.timestamp)
            .putInt(SmsRetryWorker.KEY_RETRY_COUNT, retryCount)
            .build()

        // Constraints: Network required
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Backoff delay
        val backoffDelay = SmsRetryWorker.getBackoffDelay(retryCount)

        // Build work request
        val workRequest = OneTimeWorkRequestBuilder<SmsRetryWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(backoffDelay, TimeUnit.MILLISECONDS)
            .addTag(getRetryTag(sms.timestamp, sms.sender))
            .build()

        // Enqueue work
        workManager.enqueue(workRequest)

        Timber.i("Scheduled retry #$retryCount for SMS from ${sms.sender} in ${backoffDelay}ms")
    }

    /**
     * Cancel all pending retries for a specific SMS
     */
    fun cancelRetries(sms: SmsMessage) {
        val tag = getRetryTag(sms.timestamp, sms.sender)
        workManager.cancelAllWorkByTag(tag)
        Timber.d("Cancelled all retries for SMS from ${sms.sender}")
    }

    /**
     * Cancel all pending SMS retries
     */
    fun cancelAllRetries() {
        workManager.cancelAllWorkByTag(SmsRetryWorker.WORK_NAME_PREFIX)
        Timber.i("Cancelled all pending SMS retries")
    }

    /**
     * Get unique tag for SMS retry work
     */
    private fun getRetryTag(timestamp: Long, sender: String): String {
        return "${SmsRetryWorker.WORK_NAME_PREFIX}_${timestamp}_${sender}"
    }
}
