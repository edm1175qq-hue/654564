package com.example.data

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.SmsLog
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "BankNotification"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Service Connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Skip if package name is our own app to avoid loops
        if (packageName == this.packageName) return

        // Skip if empty notification text
        if (text.isBlank()) return

        // Get readable app name from package manager
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        Log.d(TAG, "Notification detected from: $appName ($packageName), Title: $title, Text: $text")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = SmsRepository(database.smsLogDao(), database.appSettingsDao())
                val settings = repository.getSettings()

                // Check if notification receiver is active
                if (!settings.isNotificationActive) {
                    Log.d(TAG, "Notification Listener service is disabled in settings. Skipping notification.")
                    return@launch
                }

                // Enforce bank filtering if enabled
                if (settings.onlyForwardTrackedBanks) {
                    val trackedList = settings.trackedBanks.split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                    
                    if (!trackedList.contains(packageName.lowercase())) {
                        Log.d(TAG, "Notification skipped: Package '$packageName' is not in the tracked banks list.")
                        return@launch
                    }
                }

                // Apply Filters
                val shouldForward = evaluateFilters(appName, title, text, settings.senderFilter, settings.keywordFilter)
                if (!shouldForward) {
                    Log.d(TAG, "Notification did not match sender or keyword filters. Skipping forwarding.")
                    return@launch
                }

                val timestamp = sbn.postTime
                val logSender = "$appName ($title)"
                val logMessage = text

                // Insert Log as PENDING
                val logId = repository.insertLog(
                    SmsLog(
                        sender = logSender,
                        message = logMessage,
                        timestamp = timestamp,
                        status = "PENDING",
                        responseCode = null,
                        responseBody = null
                    )
                )

                // Forward SMS (Notification)
                Log.d(TAG, "Forwarding notification to webhook: ${settings.webhookUrl}")
                val result = SmsForwarder.forwardSms(
                    sender = logSender,
                    message = logMessage,
                    timestamp = timestamp,
                    webhookUrl = settings.webhookUrl,
                    authHeaderName = settings.authHeaderName,
                    authHeaderValue = settings.authHeaderValue
                )

                // Update Log status
                val updatedLog = SmsLog(
                    id = logId,
                    sender = logSender,
                    message = logMessage,
                    timestamp = timestamp,
                    status = if (result.isSuccessful) "SUCCESS" else "FAILED",
                    responseCode = result.responseCode,
                    responseBody = result.responseBody
                )
                repository.updateLog(updatedLog)
                Log.d(TAG, "Notification Log updated: Status = ${updatedLog.status}")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    private fun evaluateFilters(
        appName: String,
        title: String,
        text: String,
        senderFilter: String,
        keywordFilter: String
    ): Boolean {
        // Evaluate sender filter: checks if the appName or package name contains any defined sender filter
        if (senderFilter.isNotBlank()) {
            val senders = senderFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (senders.isNotEmpty()) {
                val matchesSender = senders.any {
                    appName.lowercase().contains(it) || title.lowercase().contains(it)
                }
                if (!matchesSender) return false
            }
        }

        // Evaluate keyword filter: checks if the notification title or body contains any keywords
        if (keywordFilter.isNotBlank()) {
            val keywords = keywordFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (keywords.isNotEmpty()) {
                val matchesKeyword = keywords.any {
                    title.lowercase().contains(it) || text.lowercase().contains(it)
                }
                if (!matchesKeyword) return false
            }
        }

        return true
    }
}
