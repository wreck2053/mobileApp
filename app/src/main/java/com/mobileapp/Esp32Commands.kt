package com.mobileapp

enum class FanSpeed(
    val wireValue: String,
    val label: String,
) {
    LOW("low", "Low"),
    MEDIUM("med", "Medium"),
    HIGH("high", "High");

    fun next(): FanSpeed =
        when (this) {
            LOW -> MEDIUM
            MEDIUM -> HIGH
            HIGH -> LOW
        }
}

object Esp32Commands {
    const val BASE_URL = "http://192.168.0.108/"

    const val TOGGLE_LIGHT = "/toggle-light"
    const val NEXT_COLOR = "/next-color"
    const val TOGGLE_FAN = "/toggle-fan"
    const val TOGGLE_NIGHT_LAMP = "/toggle-nl"
    const val POWER_OFF = "/power/off"
    const val MODE_COOL = "/mode/cool"
    const val STATE_TURBO = "/state/turbo"
    const val STATE_LED = "/state/led"
    const val STATE_SWING = "/state/swing"

    fun temperature(value: Int): String {
        require(value in 17..30) { "Temperature must be between 17 and 30." }
        return "/temp/set/$value"
    }

    fun fan(speed: FanSpeed): String = "/fan/${speed.wireValue}"

    fun urlFor(path: String, baseUrl: String = BASE_URL): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
