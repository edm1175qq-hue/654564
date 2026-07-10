package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val webhookUrl: String = "https://khaki-lapwing-104409.hostingersite.com/api/v1/sms/callback",
    val authHeaderName: String = "X-SMS-Token",
    val authHeaderValue: String = "fd49e732c5f5ed78fe5fe38b5f8ac8c2",
    val senderFilter: String = "", // Comma-separated sender list (e.g. SCB,KBank)
    val keywordFilter: String = "", // Comma-separated keyword list (e.g. OTP,โอนเงิน)
    val isSmsActive: Boolean = true,
    val isNotificationActive: Boolean = true,
    val trackedBanks: String = "com.kasikorn.kplus,com.scb.phone,th.co.krungthaibank.next,com.bualuang.mbanking,kr.co.krungsri.kma,com.ttbbank.oneapp,th.or.gsb.mymo,th.co.truemoney.wallet",
    val onlyForwardTrackedBanks: Boolean = true
)

