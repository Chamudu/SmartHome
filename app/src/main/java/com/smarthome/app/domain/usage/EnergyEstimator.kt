package com.smarthome.app.domain.usage

import com.smarthome.app.domain.model.DeviceProfile

/**
 * Fixed tariff used for estimated cost when no per-home price is configured.
 * Energy figures are estimates derived from active duration and assumed
 * wattage; they are not meter readings.
 */
const val DEFAULT_PRICE_PER_KWH = 0.20

/** Estimated energy and cost for one device or series over a selected period. */
data class EnergyEstimate(
    val energyKwh: Double,
    val cost: Double,
)

/**
 * Mirrors `functions/src/energyEstimator.ts`. Converts active duration into
 * estimated kilowatt-hours and cost using an assumed per-profile wattage.
 *
 * The wattage is a typical placeholder, not a measured value, so results are
 * deliberately presented as an estimate. Multi-switch units assume a fixed
 * wattage per independently controlled channel.
 */
object EnergyEstimator {

    /** Typical wattage assumed for a single unit of the given profile. */
    fun defaultWatts(profile: DeviceProfile, channelCount: Int = 1): Double {
        require(channelCount >= 1) { "Channel count must be positive." }
        return when (profile) {
            DeviceProfile.OUTLET -> 100.0
            DeviceProfile.MULTI_SWITCH -> 60.0 * channelCount
            DeviceProfile.SAFETY_OUTLET -> 1500.0
            DeviceProfile.LIGHT -> 9.0
            DeviceProfile.CAMERA -> 5.0
        }
    }

    /** Kilowatt-hours consumed by an active interval of [durationMillis] at [watts]. */
    fun energyKwh(watts: Double, durationMillis: Long): Double {
        require(durationMillis >= 0L) { "Active duration cannot be negative." }
        return watts * durationMillis / 3_600_000_000.0
    }

    fun estimate(
        watts: Double,
        durationMillis: Long,
        pricePerKwh: Double = DEFAULT_PRICE_PER_KWH,
    ): EnergyEstimate {
        val kwh = energyKwh(watts, durationMillis)
        return EnergyEstimate(energyKwh = kwh, cost = kwh * pricePerKwh)
    }

    /**
     * Combines every entry in a usage report into one estimate. The device-level
     * series uses the profile wattage; multi-switch channel entries use the
     * per-channel wattage so parallel channels accumulate independently.
     */
    fun estimateReport(
        profile: DeviceProfile,
        report: UsageReport,
        pricePerKwh: Double = DEFAULT_PRICE_PER_KWH,
    ): EnergyEstimate {
        var kwh = 0.0
        report.entries.forEach { entry ->
            val watts = if (entry.key.isEmpty()) {
                defaultWatts(profile)
            } else {
                defaultWatts(DeviceProfile.MULTI_SWITCH)
            }
            kwh += energyKwh(watts, entry.usage.durationMillis)
        }
        return EnergyEstimate(energyKwh = kwh, cost = kwh * pricePerKwh)
    }

    /** Compact human-readable energy such as `0.42 kWh` or `1.25 kWh`. */
    fun formatEnergy(energyKwh: Double): String {
        val hundredths = Math.round(energyKwh * 100).toLong()
        val whole = hundredths / 100
        val fraction = hundredths % 100
        val fractionText = when {
            fraction == 0L -> ""
            fraction % 10 == 0L -> (fraction / 10).toString()
            else -> fraction.toString().padStart(2, '0')
        }
        return if (fractionText.isEmpty()) "$whole kWh" else "$whole.$fractionText kWh"
    }
}