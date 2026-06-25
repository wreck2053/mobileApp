package com.mobileapp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Esp32State(
    val light: Boolean,
    val fan: Boolean,
    val connected: Boolean,
    val ac: AcStatus,
)

data class AcStatus(
    val power: Boolean,
    val mode: String,
    val temperature: Int,
    val fanLevel: Int,
    val swing: Boolean,
    val led: Boolean,
    val turbo: Boolean,
)

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

    suspend fun loadState(): Result<Esp32State> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(Esp32Commands.urlFor(Esp32Commands.API_STATE, baseUrl))
                    .openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 2_500
                    connection.readTimeout = 3_500
                    connection.useCaches = false

                    val statusCode = connection.responseCode
                    if (statusCode !in 200..299) {
                        throw IOException("ESP32 state responded with HTTP $statusCode")
                    }

                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    parseState(json)
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun parseState(json: String): Esp32State {
        val root = JSONObject(json)
        val ac = root.getJSONObject("ac")
        return Esp32State(
            light = root.optBoolean("light", false),
            fan = root.optBoolean("fan", false),
            connected = root.optBoolean("connected", false),
            ac = AcStatus(
                power = ac.optBoolean("power", false),
                mode = ac.optString("mode", "cool"),
                temperature = ac.optInt("temperature", 24).coerceIn(17, 30),
                fanLevel = ac.optInt("fanLevel", 0).coerceIn(0, 3),
                swing = ac.optBoolean("swing", false),
                led = ac.optBoolean("led", false),
                turbo = ac.optBoolean("turbo", false),
            ),
        )
    }
}
