package com.smarthome.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchChannelTest {
    @Test
    fun `different connected desired and reported states are pending`() {
        val channel = SwitchChannel(
            id = "channel-1",
            name = "Lamp",
            desiredStatus = PowerState.ON,
            reportedStatus = DeviceStatus.OFF,
            requestId = "request-1",
        )

        assertTrue(channel.isPending)
    }

    @Test
    fun `error is an operational state rather than pending command`() {
        val channel = SwitchChannel(
            id = "channel-1",
            name = "Lamp",
            desiredStatus = PowerState.ON,
            reportedStatus = DeviceStatus.ERROR,
            requestId = "request-1",
        )

        assertFalse(channel.isPending)
    }
}
