package com.mobileapp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Esp32CommandClient(
    private val baseUrl: String = Esp32Commands.BASE_URL,
) {
    suspend fun send(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(Esp32Commands.urlFor(path, baseUrl))
                    .openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 2_500
                    connection.readTimeout = 3_500
                    connection.useCaches = false

                    val statusCode = connection.responseCode
                    if (statusCode !in 200..299) {
                        throw IOException("ESP32 responded with HTTP $statusCode")
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
}
