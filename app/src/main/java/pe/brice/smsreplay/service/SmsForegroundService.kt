package pe.brice.smsreplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.MainActivity
import pe.brice.smsreplay.R
import pe.brice.smsreplay.domain.model.SendingResult
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.usecase.FlushPendingMessagesUseCase
import pe.brice.smsreplay.domain.usecase.HandleSmsSendingResultUseCase
import pe.brice.smsreplay.domain.usecase.SendSmsAsEmailUseCase
import timber.log.Timber

/**
 * Foreground Service for SMS monitoring
 *
 * Clean Architecture: Infrastructure Layer only
 * Responsibilities:
 * - Maintains persistent notification: "SMS 수신 대기중입니다"
 * - Receives SMS events from BroadcastReceiver
 * - Delegates SMS processing to Domain Layer UseCases
 * - Shows Toast for send results (UI notification)
 *
 * Business Logic Extraction:
 * - Retry logic: Moved to HandleSmsSendingResultUseCase
 * - Queue flushing: Moved to FlushPendingMessagesUseCase
 * - Service only handles Android-specific concerns (coroutines, lifecycle, Toast)
 *
 * Service persistence strategy:
 * - stopWithTask="false" in Manifest: Service continues when app is swiped away
 * - START_STICKY: System restarts service if killed by memory pressure
 * - onTaskRemoved(): Restarts service when app task is removed
 * - MainActivity: Checks and restarts service when app launches
 */
class SmsForegroundService : android.app.Service(), KoinComponent {

    companion object {
        const val CHANNEL_ID = "sms_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "pe.brice.smsreplay.ACTION_START_SERVICE"
        const val ACTION_STOP = "pe.brice.smsreplay.ACTION_STOP_SERVICE"

        // Action for processing SMS from BroadcastReceiver
        const val ACTION_PROCESS_SMS = "pe.brice.smsreplay.ACTION_PROCESS_SMS"

        // Extras for SMS data
        const val EXTRA_SMS_SENDER = "extra_sms_sender"
        const val EXTRA_SMS_BODY = "extra_sms_body"
        const val EXTRA_SMS_TIMESTAMP = "extra_sms_timestamp"

        fun startService(context: Context) {
            Timber.i("Starting SMS monitoring service")
            val intent = Intent(context, SmsForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            Timber.i("Stopping SMS monitoring service")
            val intent = Intent(context, SmsForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * Send SMS to service for processing
         * Called from BroadcastReceiver when SMS is received
         */
        fun processSms(context: Context, sender: String, body: String, timestamp: Long) {
            val intent = Intent(context, SmsForegroundService::class.java).apply {
                action = ACTION_PROCESS_SMS
                putExtra(EXTRA_SMS_SENDER, sender)
                putExtra(EXTRA_SMS_BODY, body)
                putExtra(EXTRA_SMS_TIMESTAMP, timestamp)
            }
            // Use startService with component to ensure service receives the intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    // Domain Layer UseCases - business logic
    private val sendSmsAsEmailUseCase: SendSmsAsEmailUseCase by inject()
    private val handleSmsSendingResultUseCase: HandleSmsSendingResultUseCase by inject()
    private val flushPendingMessagesUseCase: FlushPendingMessagesUseCase by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("SmsForegroundService created")
        
        // Explicitly enable SmsReceiver to ensure system delivers broadcasts
        try {
            val componentName = android.content.ComponentName(this, pe.brice.smsreplay.receiver.SmsReceiver::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            Timber.i("SmsReceiver explicitly enabled via PackageManager")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable SmsReceiver")
        }

        createNotificationChannel()

        // Try to flush queue on service start (e.g. after reboot or crash restart)
        serviceScope.launch {
            Timber.i("Service started. Checking for pending messages...")
            flushPendingMessagesUseCase()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Timber.d("Starting foreground service")
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP -> {
                Timber.d("Stopping foreground service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY  // Don't restart if explicitly stopped
            }
            ACTION_PROCESS_SMS -> {
                startForeground(NOTIFICATION_ID, createNotification())

                // Process SMS from BroadcastReceiver
                val sender = intent.getStringExtra(EXTRA_SMS_SENDER) ?: ""
                val body = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""
                val timestamp = intent.getLongExtra(EXTRA_SMS_TIMESTAMP, System.currentTimeMillis())

                Timber.d("Processing SMS from service: sender=$sender")
                processSms(sender, body, timestamp)
            }
            null -> {
                // Service restarted by system (START_STICKY), restore foreground state
                Timber.d("Service restarted by system")
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }

        // START_STICKY: System will restart service if killed by memory pressure
        return START_REDELIVER_INTENT
    }

    /**
     * Process SMS and send email
     *
     * Clean Architecture: Service delegates to Domain Layer UseCases
     * - Infrastructure: Coroutine scope management, Toast notifications
     * - Domain: Business logic (sending, retry decisions, queue management)
     *
     * Called in service scope which is safe for long-running operations
     */
    private fun processSms(sender: String, body: String, timestamp: Long) {
        serviceScope.launch {
            try {
                val sms = SmsMessage(
                    sender = sender,
                    body = body,
                    timestamp = timestamp
                )

                Timber.d("SMS received from ${sms.sender}: ${sms.body}")

                // 1. Send SMS as email (Domain Layer business logic)
                Timber.i("Calling sendSmsAsEmailUseCase...")
                val result = sendSmsAsEmailUseCase(sms)
                Timber.i("sendSmsAsEmailUseCase result: $result")

                // 2. Handle result with business logic use case (retry decisions, queue management)
                val handleResult = handleSmsSendingResultUseCase(sms, result)

                // 3. Show Toast based on result (Infrastructure concern: Android UI)
                showToast(result, handleResult)

                // 4. Flush pending messages (always trigger to process queue)
                //    This handles both success (process other queued messages)
                //    and failure (process the just-enqueued message + others)
                flushPendingMessagesUseCase()
            } catch (e: Exception) {
                Timber.e(e, "❌ Error processing SMS")
                showToast(null, null)
            }
        }
    }

    /**
     * Show Toast message based on sending result
     *
     * @param result Sending result from email service
     * @param handleResult Optional handle result from business logic (for logging)
     */
    private fun showToast(
        result: SendingResult?,
        handleResult: pe.brice.smsreplay.domain.usecase.HandleSmsSendingResultUseCase.HandleResult? = null
    ) {
        try {
            val message = when (result) {
                is SendingResult.Success -> {
                    "✅ SMS 이메일 전송 성공"
                }
                is SendingResult.Failure -> {
                    val failure = result as SendingResult.Failure
                    when (failure.error) {
                        SendingResult.ErrorType.AUTHENTICATION_FAILED -> "❌ SMTP 인증 실패 (비밀번호 확인)"
                        SendingResult.ErrorType.INVALID_RECIPIENT -> "❌ 수신자 이메일 오류"
                        SendingResult.ErrorType.SMTP_ERROR -> "❌ SMTP 서버 오류"
                        SendingResult.ErrorType.NETWORK_ERROR -> "❌ 네트워크 오류"
                        else -> "❌ 이메일 전송 실패: ${failure.message}"
                    }
                }
                is SendingResult.Retry -> {
                    "⏰ SMS 이메일 재시도 예약"
                }
                null -> {
                    "❌ SMS 처리 오류"
                }
            }

            // Log handle result if available (for debugging)
            handleResult?.let {
                when (it) {
                    is pe.brice.smsreplay.domain.usecase.HandleSmsSendingResultUseCase.HandleResult.Success -> {
                        Timber.i("✅ HandleResult: Success")
                    }
                    is pe.brice.smsreplay.domain.usecase.HandleSmsSendingResultUseCase.HandleResult.Queued -> {
                        Timber.i("💾 HandleResult: Queued - ${it.reason}")
                    }
                    is pe.brice.smsreplay.domain.usecase.HandleSmsSendingResultUseCase.HandleResult.Error -> {
                        Timber.e("❌ HandleResult: Error - ${it.message}")
                    }
                }
            }

            // Show toast on main thread
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@SmsForegroundService, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to show toast")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("Task removed - app was swiped away or force-quit")

        // Restart service using AlarmManager (More reliable than direct startService)
        try {
            val restartIntent = Intent(applicationContext, SmsForegroundService::class.java).apply {
                action = ACTION_START
            }
            
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                1,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Schedule restart after 1 second
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 3000,
                pendingIntent
            )
            
            Timber.i("Scheduled service restart via AlarmManager in 1s")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule service restart on task removed")
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SmsForegroundService destroyed")
        // Cancel coroutine job
        serviceJob.cancel()
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SMS 수신 대기 서비스"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create persistent notification
     * Content: "SMS 수신 대기중입니다"
     * Tap action: Navigate to MainActivity (settings)
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Replay")
            .setContentText("SMS 수신 대기중입니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
