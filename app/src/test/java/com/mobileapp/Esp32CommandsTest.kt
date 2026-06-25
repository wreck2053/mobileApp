package com.mobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Esp32CommandsTest {
    @Test
    fun buildsCommandUrlsWithoutDoubleSlashes() {
        assertEquals(
            "http://192.168.0.108/toggle-light",
            Esp32Commands.urlFor(Esp32Commands.TOGGLE_LIGHT),
        )
        assertEquals(
            "http://192.168.0.108/next-color",
            Esp32Commands.urlFor(Esp32Commands.NEXT_COLOR),
        )
    }

    @Test
    fun validatesTemperatureBounds() {
        assertEquals("/temp/set/17", Esp32Commands.temperature(17))
        assertEquals("/temp/set/30", Esp32Commands.temperature(30))
        assertThrows(IllegalArgumentException::class.java) {
            Esp32Commands.temperature(16)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Esp32Commands.temperature(31)
        }
    }

    @Test
    fun cyclesFanSpeedInExpectedOrder() {
        assertEquals(FanSpeed.MEDIUM, FanSpeed.LOW.next())
        assertEquals(FanSpeed.HIGH, FanSpeed.MEDIUM.next())
        assertEquals(FanSpeed.LOW, FanSpeed.HIGH.next())
        assertEquals("/fan/high", Esp32Commands.fan(FanSpeed.HIGH))
    }
}
