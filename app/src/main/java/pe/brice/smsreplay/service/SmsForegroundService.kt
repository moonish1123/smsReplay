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
import pe.brice.smsreplay.MainActivity
import pe.brice.smsreplay.R
import timber.log.Timber

/**
 * Foreground Service for SMS monitoring
 * Maintains persistent notification: "SMS 수신 대기중입니다"
 *
 * Service persistence strategy:
 * - stopWithTask="false" in Manifest: Service continues when app is swiped away
 * - START_STICKY: System restarts service if killed by memory pressure
 * - onTaskRemoved(): Restarts service when app task is removed
 * - MainActivity: Checks and restarts service when app launches
 */
class SmsForegroundService : android.app.Service() {

    companion object {
        const val CHANNEL_ID = "sms_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "pe.brice.smsreplay.ACTION_START_SERVICE"
        const val ACTION_STOP = "pe.brice.smsreplay.ACTION_STOP_SERVICE"

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
            }
            ACTION_STOP -> {
                Timber.d("Stopping foreground service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY  // Don't restart if explicitly stopped
            }
            null -> {
                // Service restarted by system (START_STICKY), restore foreground state
                Timber.d("Service restarted by system")
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }

        // START_STICKY: System will restart service if killed by memory pressure
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("Task removed - app was swiped away or force-quit")

        // Attempt to restart service when app task is removed
        // This is called when user swipes app away or force-quits
        // Note: This may not work on all Android versions/manufacturers
        try {
            val restartIntent = Intent(this, SmsForegroundService::class.java).apply {
                action = ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restart service on task removed")
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SmsForegroundService destroyed")
        // NOTE: NOT restarting service here to avoid infinite loop
        // System will restart via START_STICKY if appropriate
        // MainActivity will check and restart on app launch
        // onTaskRemoved() handles app swipe/force-quit scenario
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
