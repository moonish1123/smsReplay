package pe.brice.smsreplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage as AndroidSmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.brice.smsreplay.domain.model.SmsMessage
import pe.brice.smsreplay.domain.usecase.SendSmsAsEmailUseCase
import timber.log.Timber

/**
 * BroadcastReceiver for receiving SMS messages
 * Automatically processes SMS and sends email
 */
class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val sendSmsAsEmailUseCase: SendSmsAsEmailUseCase by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Check if this is SMS received action
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            try {
                // Parse SMS messages from PDU
                val messages = parseSmsMessages(intent)

                // Process each SMS
                messages.forEach { smsMessage ->
                    scope.launch {
                        try {
                            val sender = smsMessage.originatingAddress ?: "Unknown"
                            val body = smsMessage.messageBody ?: ""

                            val sms = SmsMessage(
                                sender = sender,
                                body = body,
                                timestamp = System.currentTimeMillis()
                            )

                            Timber.d("SMS received from ${sms.sender}: ${sms.body}")

                            // Send SMS as email
                            val result = sendSmsAsEmailUseCase(sms)

                            when (result) {
                                is pe.brice.smsreplay.domain.model.SendingResult.Success -> {
                                    Timber.i("Email sent successfully for SMS from ${sms.sender}")
                                }
                                is pe.brice.smsreplay.domain.model.SendingResult.Failure -> {
                                    Timber.e("Failed to send email: ${(result as pe.brice.smsreplay.domain.model.SendingResult.Failure).message}")
                                }
                                is pe.brice.smsreplay.domain.model.SendingResult.Retry -> {
                                    Timber.w("Scheduled retry for SMS from ${sms.sender}")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error processing SMS")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error receiving SMS")
            }
        }
    }

    /**
     * Parse SMS messages from Intent
     */
    private fun parseSmsMessages(intent: Intent): Array<AndroidSmsMessage> {
        val pdus = intent.extras?.get("pdus") as? Array<ByteArray> ?: return emptyArray()
        val format = intent.getStringExtra("format") ?: "3gpp"

        return pdus.map { pdu ->
            AndroidSmsMessage.createFromPdu(pdu, format)
        }.toTypedArray()
    }
}
