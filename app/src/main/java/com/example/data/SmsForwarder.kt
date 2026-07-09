package com.example.data

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SmsForwarder {
    private const val TAG = "SmsForwarder"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun forwardSms(
        sender: String,
        message: String,
        timestamp: Long,
        webhookUrl: String,
        authHeaderName: String,
        authHeaderValue: String
    ): ForwardResult = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext ForwardResult(false, 0, "Webhook URL is empty")
        }

        try {
            // Build JSON body
            val json = JSONObject().apply {
                put("sender", sender)
                put("message", message)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toString().toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)

            if (authHeaderName.isNotBlank() && authHeaderValue.isNotBlank()) {
                requestBuilder.addHeader(authHeaderName.trim(), authHeaderValue.trim())
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                val isSuccessful = response.isSuccessful
                
                Log.d(TAG, "Forward response code: $code, success: $isSuccessful, body: $responseBody")
                ForwardResult(isSuccessful, code, responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding SMS", e)
            ForwardResult(false, -1, e.message ?: "Unknown error")
        }
    }
}

data class ForwardResult(
    val isSuccessful: Boolean,
    val responseCode: Int,
    val responseBody: String
)
