package com.smarthome.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceStatusTest {
    @Test
    fun `connected operational states accept power commands`() {
        assertTrue(DeviceStatus.ON.acceptsPowerCommands)
        assertTrue(DeviceStatus.OFF.acceptsPowerCommands)
    }

    @Test
    fun `error and disconnected states reject power commands`() {
        assertFalse(DeviceStatus.ERROR.acceptsPowerCommands)
        assertFalse(DeviceStatus.DISCONNECTED.acceptsPowerCommands)
    }
}
