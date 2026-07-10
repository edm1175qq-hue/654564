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
                    isSmsActive = current.isSmsActive,
                    isNotificationActive = current.isNotificationActive
                )
                repository.saveSettings(defaultSettings)
                defaultSettings
            } else {
                current
            }
            
            val isAnyServiceActive = finalSettings.isSmsActive || finalSettings.isNotificationActive
            if (isAnyServiceActive) {
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
            
            val isAnyServiceActive = newSettings.isSmsActive || newSettings.isNotificationActive
            // Start or stop service based on the updated state
            val intent = Intent(getApplication(), SmsForwarderService::class.java).apply {
                action = if (isAnyServiceActive) {
                    SmsForwarderService.ACTION_START
                } else {
                    SmsForwarderService.ACTION_STOP
                }
            }
            
            if (isAnyServiceActive) {
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

}
