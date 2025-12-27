package pe.brice.smsreplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage as AndroidSmsMessage
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
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.i("===== SmsReceiver.onReceive CALLED =====")
        Timber.i("Intent action: ${intent?.action}")
        Timber.i("Context: ${context?.packageName}")

        if (context == null || intent == null) {
            Timber.w("Context or Intent is null!")
            return
        }

        // Check if this is SMS received action
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            Timber.i("SMS_RECEIVED action detected!")
            try {
                // Parse SMS messages from PDU
                val messages = parseSmsMessages(intent)
                Timber.i("Parsed ${messages.size} SMS messages")

                // Forward each SMS to Service for processing
                messages.forEach { smsMessage ->
                    val sender = smsMessage.originatingAddress ?: "Unknown"
                    val body = smsMessage.messageBody ?: ""
                    val timestamp = System.currentTimeMillis()

                    Timber.i("Forwarding SMS to Service - Sender: $sender, Body: $body")

                    // Forward to Service for safe background processing
                    SmsForegroundService.processSms(context, sender, body, timestamp)
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

    /**
     * Parse SMS messages from Intent
     */
    private fun parseSmsMessages(intent: Intent): Array<AndroidSmsMessage> {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return emptyArray()
        val format = intent.getStringExtra("format") ?: "3gpp"

        @Suppress("UNCHECKED_CAST")
        val pduArray = pdus as? Array<ByteArray> ?: return emptyArray()

        return pduArray.map { pdu ->
            AndroidSmsMessage.createFromPdu(pdu, format)
        }.toTypedArray()
    }
}
