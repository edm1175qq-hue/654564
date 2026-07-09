package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ForwardResult
import com.example.data.SmsForwarder
import com.example.data.SmsForwarderService
import com.example.data.database.AppDatabase
import com.example.data.entity.AppSettings
import com.example.data.entity.SmsLog
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SmsRepository
    
    val allLogs: StateFlow<List<SmsLog>>
    val settings: StateFlow<AppSettings>

    private val _isAutoSimulating = MutableStateFlow(false)
    val isAutoSimulating = _isAutoSimulating.asStateFlow()

    private var autoSimulateJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SmsRepository(database.smsLogDao(), database.appSettingsDao())
        
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val finalSettings = if (current.webhookUrl.isBlank() || 
                current.webhookUrl.contains("replit.app") || 
                !current.webhookUrl.contains("khaki-lapwing-104409.hostingersite.com")
            ) {
                val defaultSettings = AppSettings(
                    webhookUrl = "https://khaki-lapwing-104409.hostingersite.com/api/v1/sms/callback",
                    authHeaderName = "X-SMS-Token",
                    authHeaderValue = "fd49e732c5f5ed78fe5fe38b5f8ac8c2",
                    isServiceActive = current.isServiceActive
                )
                repository.saveSettings(defaultSettings)
                defaultSettings
            } else {
                current
            }
            
            if (finalSettings.isServiceActive) {
                val intent = Intent(getApplication(), SmsForwarderService::class.java).apply {
                    action = SmsForwarderService.ACTION_START
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            }
        }
        
        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        settings = repository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(newSettings)
            
            // Start or stop service based on the updated state
            val intent = Intent(getApplication(), SmsForwarderService::class.java).apply {
                action = if (newSettings.isServiceActive) {
                    SmsForwarderService.ACTION_START
                } else {
                    SmsForwarderService.ACTION_STOP
                }
            }
            
            if (newSettings.isServiceActive) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            } else {
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    fun resendSms(log: SmsLog, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            // Set temporary pending status
            val pendingLog = log.copy(status = "PENDING", responseCode = null, responseBody = null)
            repository.updateLog(pendingLog)

            val currentSettings = repository.getSettings()
            val result = SmsForwarder.forwardSms(
                sender = log.sender,
                message = log.message,
                timestamp = System.currentTimeMillis(),
                webhookUrl = currentSettings.webhookUrl,
                authHeaderName = currentSettings.authHeaderName,
                authHeaderValue = currentSettings.authHeaderValue
            )

            val finalLog = log.copy(
                status = if (result.isSuccessful) "SUCCESS" else "FAILED",
                responseCode = result.responseCode,
                responseBody = result.responseBody,
                timestamp = System.currentTimeMillis() // Update time of forward
            )
            repository.updateLog(finalLog)
            
            launch(Dispatchers.Main) {
                onResult(result.isSuccessful, "Code ${result.responseCode}: ${result.responseBody}")
            }
        }
    }

    fun simulateSmsReceived(sender: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val currentSettings = repository.getSettings()

            // If not active, we still log but don't forward
            if (!currentSettings.isServiceActive) {
                repository.insertLog(
                    SmsLog(
                        sender = sender,
                        message = message,
                        timestamp = timestamp,
                        status = "SERVICE_INACTIVE",
                        responseCode = null,
                        responseBody = "Service is inactive. Forwarding skipped."
                    )
                )
                return@launch
            }

            // Apply Filters
            val shouldForward = evaluateFilters(sender, message, currentSettings.senderFilter, currentSettings.keywordFilter)
            if (!shouldForward) {
                repository.insertLog(
                    SmsLog(
                        sender = sender,
                        message = message,
                        timestamp = timestamp,
                        status = "FILTERED",
                        responseCode = null,
                        responseBody = "Filtered out. Did not match sender/keyword filters."
                    )
                )
                return@launch
            }

            val logId = repository.insertLog(
                SmsLog(
                    sender = sender,
                    message = message,
                    timestamp = timestamp,
                    status = "PENDING",
                    responseCode = null,
                    responseBody = null
                )
            )

            val result = SmsForwarder.forwardSms(
                sender = sender,
                message = message,
                timestamp = timestamp,
                webhookUrl = currentSettings.webhookUrl,
                authHeaderName = currentSettings.authHeaderName,
                authHeaderValue = currentSettings.authHeaderValue
            )

            repository.updateLog(
                SmsLog(
                    id = logId,
                    sender = sender,
                    message = message,
                    timestamp = timestamp,
                    status = if (result.isSuccessful) "SUCCESS" else "FAILED",
                    responseCode = result.responseCode,
                    responseBody = result.responseBody
                )
            )
        }
    }

    fun toggleAutoSimulation(active: Boolean) {
        _isAutoSimulating.value = active
        if (active) {
            autoSimulateJob?.cancel()
            autoSimulateJob = viewModelScope.launch(Dispatchers.IO) {
                val templates = listOf(
                    Pair("K-Bank", "คุณได้รับโอนเงินจาก นายวิทยา จำนวน 1,500.00 บาท วันที่ 09/07/2026 13:40"),
                    Pair("SCB", "เงินเข้า บัญชี x1234 ยอด 2,350.00 บาท จาก SCB EASY App"),
                    Pair("TrueMoney", "คุณได้รับเงิน 500.00 บาท จาก บจก. เทสการค้า"),
                    Pair("Krungthai", "เงินเข้าบัญชี Krungthai NEXT จำนวน 10,000.00 บาท"),
                    Pair("K PLUS", "ได้รับโอนเงินจำนวน 4,200.00 บาท จาก น.ส. รัตนา"),
                    Pair("SCB", "ได้รับโอนเงินจำนวน 8,500.00 บาท จาก SCB EASY 09/07 13:45"),
                    Pair("KBank", "ยอดเงินเข้า 350.00 บาท จาก นายประดิษฐ์"),
                    Pair("Krungsri", "เงินเข้าบัญชี x9876 จำนวน 1,200.00 บาท"),
                    Pair("TrueMoney", "เติมเงินสำเร็จ 150.00 บาท ผ่านโมบายแบงก์กิ้ง"),
                    Pair("SCB", "เงินเข้าบัญชี x5678 จำนวน 600.00 บาท จาก บัญชีต่างธนาคาร")
                )
                var index = 0
                while (_isAutoSimulating.value) {
                    val item = templates[index % templates.size]
                    simulateSmsReceived(item.first, item.second)
                    index++
                    delay(15000) // every 15 seconds
                }
            }
        } else {
            autoSimulateJob?.cancel()
            autoSimulateJob = null
        }
    }

    private fun evaluateFilters(
        sender: String,
        message: String,
        senderFilter: String,
        keywordFilter: String
    ): Boolean {
        if (senderFilter.isNotBlank()) {
            val senders = senderFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (senders.isNotEmpty()) {
                val matchesSender = senders.any { sender.lowercase().contains(it) }
                if (!matchesSender) return false
            }
        }

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
