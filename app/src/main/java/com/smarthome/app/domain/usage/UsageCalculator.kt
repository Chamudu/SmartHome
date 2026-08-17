package com.smarthome.app.domain.usage

import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.DeviceStatus

/** Usage totals for one ON/OFF series (a device or one multi-switch channel). */
data class SeriesUsage(
    val activationCount: Int,
    val durationMillis: Long,
    val ongoing: Boolean,
    val startedBeforePeriod: Boolean,
    val unpairedOffCount: Int,
)

/** One entry within a usage report, keyed by channel ID (empty string = device). */
data class UsageReportEntry(
    val key: String,
    val usage: SeriesUsage,
)

/** Combined usage across a device's series for a selected period. */
data class UsageReport(
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val entries: List<UsageReportEntry>,
    val totalActivations: Int,
    val totalDurationMillis: Long,
)

/**
 * Mirrors `functions/src/usageCalculator.ts`. Pairs reported state transitions
 * into ON/OFF intervals and derives activation counts and accumulated active
 * duration.
 *
 * Only [DeviceStatus.ON] is treated as active; `OFF`, `ERROR`, and
 * `DISCONNECTED` all close an open interval. Missing pairs are handled
 * explicitly rather than producing negative or misleading totals:
 * - An ON interval still open at the period end is truncated at the period end
 *   and flagged as [SeriesUsage.ongoing].
 * - An OFF transition with no matching open interval is counted in
 *   [SeriesUsage.unpairedOffCount] and contributes nothing to duration.
 * - An interval that began before the period is measured from the period start
 *   and flagged with [SeriesUsage.startedBeforePeriod].
 */
object UsageCalculator {

    fun seriesUsage(
        events: List<DeviceEvent>,
        periodStartMillis: Long,
        periodEndMillis: Long,
        initialStatus: DeviceStatus = DeviceStatus.OFF,
    ): SeriesUsage {
        require(periodEndMillis >= periodStartMillis) {
            "Usage period end must not precede its start."
        }

        val ordered = events.sortedBy { it.occurredAtMillis }

        var state = initialStatus
        var activeAtStart = state == DeviceStatus.ON
        var activatedBeforeStart = false
        for (event in ordered) {
            val status = event.toStatus ?: continue
            if (event.occurredAtMillis > periodStartMillis) break
            state = status
            val turnsOn = status == DeviceStatus.ON
            activeAtStart = turnsOn
            activatedBeforeStart = turnsOn && event.occurredAtMillis < periodStartMillis
        }

        var activationCount = if (activeAtStart) 1 else 0
        var durationMillis = 0L
        var unpairedOffCount = 0
        var openStartMillis: Long? = if (activeAtStart) periodStartMillis else null

        for (event in ordered) {
            val status = event.toStatus ?: continue
            val timestamp = event.occurredAtMillis
            if (timestamp <= periodStartMillis) continue
            if (timestamp > periodEndMillis) break

            if (status == DeviceStatus.ON) {
                if (openStartMillis == null) {
                    openStartMillis = timestamp
                    activationCount += 1
                }
            } else if (openStartMillis != null) {
                durationMillis += timestamp - maxOf(openStartMillis, periodStartMillis)
                openStartMillis = null
            } else if (status == DeviceStatus.OFF) {
                unpairedOffCount += 1
            }
        }

        var ongoing = false
        if (openStartMillis != null) {
            durationMillis += periodEndMillis - maxOf(openStartMillis, periodStartMillis)
            ongoing = true
        }

        return SeriesUsage(
            activationCount = activationCount,
            durationMillis = durationMillis,
            ongoing = ongoing,
            startedBeforePeriod = activeAtStart && activatedBeforeStart,
            unpairedOffCount = unpairedOffCount,
        )
    }

    /**
     * Groups events by optional channel ID and returns one entry per series
     * together with combined totals. Events without a channel represent the
     * device-level series keyed by the empty string.
     */
    fun report(
        events: List<DeviceEvent>,
        periodStartMillis: Long,
        periodEndMillis: Long,
        initialStatus: DeviceStatus = DeviceStatus.OFF,
    ): UsageReport {
        val keys = events.map { it.channelId.orEmpty() }.distinct().sorted()
        val entries = keys.map { key ->
            UsageReportEntry(
                key = key,
                usage = seriesUsage(
                    events = events.filter { it.channelId.orEmpty() == key },
                    periodStartMillis = periodStartMillis,
                    periodEndMillis = periodEndMillis,
                    initialStatus = initialStatus,
                ),
            )
        }
        return UsageReport(
            periodStartMillis = periodStartMillis,
            periodEndMillis = periodEndMillis,
            entries = entries,
            totalActivations = entries.sumOf { it.usage.activationCount },
            totalDurationMillis = entries.sumOf { it.usage.durationMillis },
        )
    }

    /** Compact human-readable duration such as `42 min` or `1 h 05 min`. */
    fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / 60_000
        if (totalMinutes < 60) return "$totalMinutes min"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0L) {
            "${hours} h"
        } else {
            "${hours} h ${minutes.toString().padStart(2, '0')} min"
        }
    }
}
