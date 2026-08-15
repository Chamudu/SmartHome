package com.smarthome.app.domain.usage

import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.EventOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

class EnergyEstimatorTest {

    private fun on(millis: Long, channelId: String? = null) = event(DeviceStatus.ON, millis, channelId)

    private fun off(millis: Long, channelId: String? = null) = event(DeviceStatus.OFF, millis, channelId)

    private fun event(status: DeviceStatus, millis: Long, channelId: String? = null) = DeviceEvent(
        id = "event-$millis-${status.name}",
        type = "STATE_REPORTED",
        fromStatus = null,
        toStatus = status,
        origin = EventOrigin.SIMULATOR,
        actorId = null,
        requestId = null,
        reason = null,
        occurredAtMillis = millis,
        channelId = channelId,
    )

    @Test
    fun `converts wattage and duration into kilowatt-hours`() {
        assertEquals(1.0, EnergyEstimator.energyKwh(watts = 1_000.0, durationMillis = HOUR), 0.0001)
        assertEquals(0.045, EnergyEstimator.energyKwh(watts = 9.0, durationMillis = 5 * HOUR), 0.0001)
        assertEquals(0.0, EnergyEstimator.energyKwh(watts = 100.0, durationMillis = 0L), 0.0001)
    }

    @Test
    fun `multiplies energy by the tariff to estimate cost`() {
        val estimate = EnergyEstimator.estimate(watts = 1_000.0, durationMillis = HOUR)
        assertEquals(1.0, estimate.energyKwh, 0.0001)
        assertEquals(0.20, estimate.cost, 0.0001)

        val custom = EnergyEstimator.estimate(watts = 500.0, durationMillis = HOUR, pricePerKwh = 0.30)
        assertEquals(0.5, custom.energyKwh, 0.0001)
        assertEquals(0.15, custom.cost, 0.0001)
    }

    @Test
    fun `uses expected default wattage per profile`() {
        assertEquals(100.0, EnergyEstimator.defaultWatts(DeviceProfile.OUTLET), 0.0001)
        assertEquals(1500.0, EnergyEstimator.defaultWatts(DeviceProfile.SAFETY_OUTLET), 0.0001)
        assertEquals(9.0, EnergyEstimator.defaultWatts(DeviceProfile.LIGHT), 0.0001)
        assertEquals(5.0, EnergyEstimator.defaultWatts(DeviceProfile.CAMERA), 0.0001)
        assertEquals(60.0, EnergyEstimator.defaultWatts(DeviceProfile.MULTI_SWITCH), 0.0001)
        assertEquals(300.0, EnergyEstimator.defaultWatts(DeviceProfile.MULTI_SWITCH, channelCount = 5), 0.0001)
    }

    @Test
    fun `rejects a negative channel count`() {
        val thrown = try {
            EnergyEstimator.defaultWatts(DeviceProfile.MULTI_SWITCH, channelCount = 0)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `rejects a negative active duration`() {
        val thrown = try {
            EnergyEstimator.energyKwh(watts = 100.0, durationMillis = -1L)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `estimates a device report from its duration`() {
        val start = 1_000_000L
        val report = UsageCalculator.report(
            events = listOf(on(start), off(start + HOUR)),
            periodStartMillis = start,
            periodEndMillis = start + 2 * HOUR,
        )
        val estimate = EnergyEstimator.estimateReport(DeviceProfile.LIGHT, report)
        assertEquals(9.0 / 1000.0, estimate.energyKwh, 0.000_001)
        assertEquals(9.0 / 1000.0 * DEFAULT_PRICE_PER_KWH, estimate.cost, 0.000_001)
    }

    @Test
    fun `accumulates energy independently across multi-switch channels`() {
        val start = 1_000_000L
        val report = UsageCalculator.report(
            events = listOf(
                on(start, "channel-1"),
                off(start + HOUR, "channel-1"),
                on(start, "channel-2"),
                off(start + 2 * HOUR, "channel-2"),
            ),
            periodStartMillis = start,
            periodEndMillis = start + 3 * HOUR,
        )
        val estimate = EnergyEstimator.estimateReport(DeviceProfile.MULTI_SWITCH, report)
        assertEquals(3 * 60.0 / 1000.0, estimate.energyKwh, 0.000_001)
    }

    @Test
    fun `formats energy compactly`() {
        assertEquals("0 kWh", EnergyEstimator.formatEnergy(0.0))
        assertEquals("0.42 kWh", EnergyEstimator.formatEnergy(0.42))
        assertEquals("1.2 kWh", EnergyEstimator.formatEnergy(1.2))
        assertEquals("1.25 kWh", EnergyEstimator.formatEnergy(1.25))
        assertEquals("2 kWh", EnergyEstimator.formatEnergy(2.0))
    }
}