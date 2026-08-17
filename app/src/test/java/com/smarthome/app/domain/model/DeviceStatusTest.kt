package com.smarthome.app.domain.model

import org.junit.Assert.assertEquals
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

    @Test
    fun `operational states map to online camera connectivity`() {
        assertEquals(CameraConnectivity.ONLINE, DeviceStatus.ON.toCameraConnectivity())
        assertEquals(CameraConnectivity.ONLINE, DeviceStatus.OFF.toCameraConnectivity())
    }

    @Test
    fun `error and disconnected states map to error and offline connectivity`() {
        assertEquals(CameraConnectivity.ERROR, DeviceStatus.ERROR.toCameraConnectivity())
        assertEquals(CameraConnectivity.OFFLINE, DeviceStatus.DISCONNECTED.toCameraConnectivity())
    }
}
