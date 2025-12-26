package pe.brice.smsreplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.MainActivity
import pe.brice.smsreplay.R
import timber.log.Timber

/**
 * Foreground Service for SMS monitoring
 * Maintains persistent notification: "SMS 수신 대기중입니다"
 */
class SmsForegroundService : android.app.Service(), KoinComponent {

    private val serviceManager: ServiceManager by inject()

    companion object {
        const val CHANNEL_ID = "sms_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "pe.brice.smsreplay.ACTION_START_SERVICE"
        const val ACTION_STOP = "pe.brice.smsreplay.ACTION_STOP_SERVICE"

        fun startService(context: Context) {
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
            val intent = Intent(context, SmsForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("SmsForegroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Timber.d("Starting foreground service")
                startForeground(NOTIFICATION_ID, createNotification())
                serviceManager.startMonitoring()
            }
            ACTION_STOP -> {
                Timber.d("Stopping foreground service")
                serviceManager.stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SmsForegroundService destroyed")
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
