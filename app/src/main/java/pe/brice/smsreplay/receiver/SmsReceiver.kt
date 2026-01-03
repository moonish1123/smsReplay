package pe.brice.smsreplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat.startForegroundService
import pe.brice.smsreplay.service.SmsForegroundService
import timber.log.Timber

/**
 * BroadcastReceiver for receiving SMS messages
 * Forwards SMS events to SmsForegroundService for processing
 *
 * Architecture: Receiver → Service → UseCase
 * - Receiver: Lightweight, only forwards events
 * - Service: Handles long-running email sending in foreground
 * - UseCase: Business logic for email sending
 *
 * Improvements:
 * - Uses standard API `Telephony.Sms.Intents.getMessagesFromIntent` for better compatibility
 * - Handles multipart SMS (LMS) by combining message parts from the same sender
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.i("===== SmsReceiver.onReceive CALLED =====")
        Timber.i("Intent action: ${intent?.action}")

        if (context == null || intent == null) {
            Timber.w("Context or Intent is null!")
            return
        }

        // Check if this is SMS received action
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            SmsForegroundService.startService(context)

            Timber.i("SMS_RECEIVED action detected!")
            try {
                // 1. Use Android Standard API to parse messages
                // This handles PDU parsing logic internally for better compatibility
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                
                if (messages.isNullOrEmpty()) {
                    Timber.w("No messages found in intent")
                    return
                }
                
                Timber.i("Parsed ${messages.size} message parts")

                // 2. Group by Sender to handle Multipart SMS (LMS)
                // Long messages are split into multiple parts, we need to combine them
                val messagesBySender = messages.groupBy { it.originatingAddress }

                messagesBySender.forEach { (sender, parts) ->
                    val safeSender = sender ?: "Unknown"
                    
                    // Combine body parts
                    val fullBody = StringBuilder()
                    parts.forEach { part ->
                        fullBody.append(part.messageBody ?: "")
                    }
                    val finalBody = fullBody.toString()
                    val timestamp = System.currentTimeMillis() // Use current time for receipt time

                    Timber.i("Combined SMS from $safeSender (Parts: ${parts.size})")
                    Timber.d("Full Body length: ${finalBody.length}")

                    if (finalBody.isNotEmpty()) {
                         Timber.i("Forwarding combined SMS to Service - Sender: $safeSender")
                        // Forward to Service for safe background processing
                        SmsForegroundService.processSms(context, safeSender, finalBody, timestamp)
                    } else {
                        Timber.w("Empty message body from $safeSender, skipping")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error receiving SMS")
                e.printStackTrace()
            }
        } else {
            Timber.w("Received unexpected intent action: ${intent.action}")
        }
        Timber.i("===== SmsReceiver.onReceive FINISHED =====")
    }
}
