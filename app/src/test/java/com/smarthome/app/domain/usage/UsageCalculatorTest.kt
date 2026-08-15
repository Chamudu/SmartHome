package com.smarthome.app.domain.usage

import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.EventOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

class UsageCalculatorTest {

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
    fun `pairs ON OFF events into activation counts and durations`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(
                on(start),
                off(start + 30 * MINUTE),
                on(start + 2 * HOUR),
                off(start + 3 * HOUR),
            ),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(2, usage.activationCount)
        assertEquals(90 * MINUTE, usage.durationMillis)
        assertFalse(usage.ongoing)
        assertEquals(0, usage.unpairedOffCount)
        assertFalse(usage.startedBeforePeriod)
    }

    @Test
    fun `handles an interval still open at the period end`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(on(start + HOUR)),
            periodStartMillis = start,
            periodEndMillis = start + 5 * HOUR,
        )
        assertEquals(1, usage.activationCount)
        assertEquals(4 * HOUR, usage.durationMillis)
        assertTrue(usage.ongoing)
    }

    @Test
    fun `counts an interval that began before the period from the period start`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(on(start - HOUR), off(start + 2 * HOUR)),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(1, usage.activationCount)
        assertEquals(2 * HOUR, usage.durationMillis)
        assertTrue(usage.startedBeforePeriod)
        assertFalse(usage.ongoing)
    }

    @Test
    fun `does not report negative durations for missing opening pairs`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(off(start + HOUR), off(start + 2 * HOUR)),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(0, usage.activationCount)
        assertEquals(0L, usage.durationMillis)
        assertEquals(2, usage.unpairedOffCount)
    }

    @Test
    fun `treats ERROR as closing an open interval`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(
                on(start),
                event(DeviceStatus.ERROR, start + HOUR),
            ),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(1, usage.activationCount)
        assertEquals(HOUR, usage.durationMillis)
        assertFalse(usage.ongoing)
    }

    @Test
    fun `ignores events outside the requested period`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = listOf(
                on(start - DAY),
                off(start - DAY + HOUR),
                on(start + HOUR),
                off(start + 2 * HOUR),
            ),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(1, usage.activationCount)
        assertEquals(HOUR, usage.durationMillis)
    }

    @Test
    fun `clamping the period end to the latest event counts events recorded after the device clock`() {
        val deviceClockNow = 1_000_000L
        val onAt = deviceClockNow + 10 * MINUTE
        val offAt = onAt + 5 * MINUTE
        val periodStart = deviceClockNow - 7 * DAY
        val events = listOf(on(onAt), off(offAt))

        val unclamped = UsageCalculator.seriesUsage(
            events = events,
            periodStartMillis = periodStart,
            periodEndMillis = deviceClockNow,
        )
        assertEquals(0, unclamped.activationCount)

        val clamped = UsageCalculator.seriesUsage(
            events = events,
            periodStartMillis = periodStart,
            periodEndMillis = maxOf(deviceClockNow, offAt),
        )
        assertEquals(1, clamped.activationCount)
        assertEquals(5 * MINUTE, clamped.durationMillis)
        assertFalse(clamped.ongoing)
    }

    @Test
    fun `uses the optional initial status as a fallback opening state`() {
        val start = 1_000_000L
        val usage = UsageCalculator.seriesUsage(
            events = emptyList(),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
            initialStatus = DeviceStatus.ON,
        )
        assertEquals(1, usage.activationCount)
        assertEquals(DAY, usage.durationMillis)
        assertTrue(usage.ongoing)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a period whose end precedes its start`() {
        UsageCalculator.seriesUsage(
            events = emptyList(),
            periodStartMillis = 1_000L,
            periodEndMillis = 999L,
        )
    }

    @Test
    fun `groups channel events and combines totals`() {
        val start = 1_000_000L
        val report = UsageCalculator.report(
            events = listOf(
                on(start, "channel-1"),
                off(start + HOUR, "channel-1"),
                on(start + HOUR),
                off(start + 2 * HOUR),
                on(start, "channel-2"),
            ),
            periodStartMillis = start,
            periodEndMillis = start + DAY,
        )
        assertEquals(3, report.entries.size)
        val byKey = report.entries.associateBy { it.key }
        assertEquals(1, byKey.getValue("").usage.activationCount)
        assertEquals(1, byKey.getValue("channel-1").usage.activationCount)
        assertEquals(1, byKey.getValue("channel-2").usage.activationCount)
        assertTrue(byKey.getValue("channel-2").usage.ongoing)
        assertEquals(3, report.totalActivations)
        assertEquals(2 * HOUR + DAY, report.totalDurationMillis)
    }

    @Test
    fun `formats durations compactly`() {
        assertEquals("42 min", UsageCalculator.formatDuration(42 * MINUTE))
        assertEquals("1 h", UsageCalculator.formatDuration(60 * MINUTE))
        assertEquals("1 h 05 min", UsageCalculator.formatDuration(65 * MINUTE))
        assertEquals("0 min", UsageCalculator.formatDuration(0L))
    }
}
