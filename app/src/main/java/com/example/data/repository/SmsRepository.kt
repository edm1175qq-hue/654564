package com.example.data.repository

import com.example.data.dao.AppSettingsDao
import com.example.data.dao.SmsLogDao
import com.example.data.entity.AppSettings
import com.example.data.entity.SmsLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SmsRepository(
    private val smsLogDao: SmsLogDao,
    private val appSettingsDao: AppSettingsDao
) {
    val allLogs: Flow<List<SmsLog>> = smsLogDao.getAllLogs()
    
    val settingsFlow: Flow<AppSettings> = appSettingsDao.getSettingsFlow().map { 
        it ?: AppSettings() 
    }

    suspend fun getSettings(): AppSettings {
        return appSettingsDao.getSettings() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        appSettingsDao.saveSettings(settings)
    }

    suspend fun insertLog(log: SmsLog): Long {
        return smsLogDao.insertLog(log)
    }

    suspend fun updateLog(log: SmsLog) {
        smsLogDao.updateLog(log)
    }

    suspend fun clearLogs() {
        smsLogDao.clearLogs()
    }
}
