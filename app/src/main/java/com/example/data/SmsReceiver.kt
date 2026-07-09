package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.SmsLog
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS received intent triggered")
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) {
                    pendingResult.finish()
                    return@launch
                }

                val sender = messages[0].originatingAddress ?: "Unknown"
                val fullMessage = messages.joinToString("") { it.messageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                Log.d(TAG, "Parsing SMS from: $sender, message: $fullMessage")

                val database = AppDatabase.getDatabase(context)
                val repository = SmsRepository(database.smsLogDao(), database.appSettingsDao())
                val settings = repository.getSettings()

                // Check if forwarder service is active
                if (!settings.isServiceActive) {
                    Log.d(TAG, "SMS Forwarder service is disabled in settings. Skipping forwarding.")
                    pendingResult.finish()
                    return@launch
                }

                // Apply Filters
                val shouldForward = evaluateFilters(sender, fullMessage, settings.senderFilter, settings.keywordFilter)
                if (!shouldForward) {
                    Log.d(TAG, "SMS did not match sender or keyword filters. Skipping forwarding.")
                    pendingResult.finish()
                    return@launch
                }

                // Insert Log as PENDING
                val logId = repository.insertLog(
                    SmsLog(
                        sender = sender,
                        message = fullMessage,
                        timestamp = timestamp,
                        status = "PENDING",
                        responseCode = null,
                        responseBody = null
                    )
                )

                // Forward SMS
                Log.d(TAG, "Forwarding SMS to webhook: ${settings.webhookUrl}")
                val result = SmsForwarder.forwardSms(
                    sender = sender,
                    message = fullMessage,
                    timestamp = timestamp,
                    webhookUrl = settings.webhookUrl,
                    authHeaderName = settings.authHeaderName,
                    authHeaderValue = settings.authHeaderValue
                )

                // Update Log status
                val updatedLog = SmsLog(
                    id = logId,
                    sender = sender,
                    message = fullMessage,
                    timestamp = timestamp,
                    status = if (result.isSuccessful) "SUCCESS" else "FAILED",
                    responseCode = result.responseCode,
                    responseBody = result.responseBody
                )
                repository.updateLog(updatedLog)
                Log.d(TAG, "SMS Log updated: Status = ${updatedLog.status}")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing received SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun evaluateFilters(
        sender: String,
        message: String,
        senderFilter: String,
        keywordFilter: String
    ): Boolean {
        // Evaluate sender filter
        if (senderFilter.isNotBlank()) {
            val senders = senderFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (senders.isNotEmpty()) {
                val matchesSender = senders.any { sender.lowercase().contains(it) }
                if (!matchesSender) return false
            }
        }

        // Evaluate keyword filter
        if (keywordFilter.isNotBlank()) {
            val keywords = keywordFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (keywords.isNotEmpty()) {
                val matchesKeyword = keywords.any { message.lowercase().contains(it) }
                if (!matchesKeyword) return false
            }
        }

        return true
    }
}
