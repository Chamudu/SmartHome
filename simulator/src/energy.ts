import type { UsageReport } from './usage'

export const DEFAULT_PRICE_PER_KWH = 0.2

export type EnergyProfile = 'OUTLET' | 'MULTI_SWITCH' | 'SAFETY_OUTLET' | 'LIGHT' | 'CAMERA'

export type EnergyEstimate = {
  energyKwh: number
  cost: number
}

/**
 * Mirrors functions/src/energyEstimator.ts and the Android EnergyEstimator.
 * Converts active duration into estimated kilowatt-hours and cost using an
 * assumed per-profile wattage. Results are estimates, not meter readings.
 */
export function defaultWatts(profile: EnergyProfile, channelCount = 1): number {
  if (channelCount < 1) throw new RangeError('Channel count must be positive.')
  switch (profile) {
    case 'OUTLET':
      return 100
    case 'MULTI_SWITCH':
      return 60 * channelCount
    case 'SAFETY_OUTLET':
      return 1500
    case 'LIGHT':
      return 9
    case 'CAMERA':
      return 5
  }
}

export function energyKwh(watts: number, durationMillis: number): number {
  if (durationMillis < 0) throw new RangeError('Active duration cannot be negative.')
  return (watts * durationMillis) / 3_600_000_000
}

export function estimate(
  watts: number,
  durationMillis: number,
  pricePerKwh = DEFAULT_PRICE_PER_KWH,
): EnergyEstimate {
  const kwh = energyKwh(watts, durationMillis)
  return { energyKwh: kwh, cost: kwh * pricePerKwh }
}

export function estimateReport(
  profile: EnergyProfile,
  report: UsageReport,
  pricePerKwh = DEFAULT_PRICE_PER_KWH,
): EnergyEstimate {
  let kwh = 0
  for (const entry of report.entries) {
    const watts = entry.key === '' ? defaultWatts(profile) : defaultWatts('MULTI_SWITCH')
    kwh += energyKwh(watts, entry.usage.durationMillis)
  }
  return { energyKwh: kwh, cost: kwh * pricePerKwh }
}

export function formatEnergy(energyKwh: number): string {
  const hundredths = Math.round(energyKwh * 100)
  const whole = Math.floor(hundredths / 100)
  const fraction = hundredths % 100
  const fractionText = fraction === 0
    ? ''
    : fraction % 10 === 0
      ? String(fraction / 10)
      : String(fraction).padStart(2, '0')
  return fractionText === '' ? `${whole} kWh` : `${whole}.${fractionText} kWh`
}

export function formatCost(cost: number): string {
  const cents = Math.round(cost * 100)
  return `$${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`
}