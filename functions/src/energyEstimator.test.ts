import { describe, expect, it } from 'vitest'
import {
  DEFAULT_PRICE_PER_KWH,
  defaultWatts,
  energyKwh,
  estimate,
  estimateReport,
  formatEnergy,
} from './energyEstimator.js'
import { calculateUsageReport, type UsageEvent } from './usageCalculator.js'

const HOUR = 60 * 60 * 1_000

function on(millis: number, channelId?: string): UsageEvent {
  return { occurredAtMillis: millis, toStatus: 'ON', channelId }
}

function off(millis: number, channelId?: string): UsageEvent {
  return { occurredAtMillis: millis, toStatus: 'OFF', channelId }
}

describe('energy estimation', () => {
  it('converts wattage and duration into kilowatt-hours', () => {
    expect(energyKwh(1_000, HOUR)).toBeCloseTo(1.0, 5)
    expect(energyKwh(9, 5 * HOUR)).toBeCloseTo(0.045, 5)
    expect(energyKwh(100, 0)).toBe(0)
  })

  it('multiplies energy by the tariff to estimate cost', () => {
    const result = estimate(1_000, HOUR)
    expect(result.energyKwh).toBeCloseTo(1.0, 5)
    expect(result.cost).toBeCloseTo(DEFAULT_PRICE_PER_KWH, 5)

    const custom = estimate(500, HOUR, 0.3)
    expect(custom.energyKwh).toBeCloseTo(0.5, 5)
    expect(custom.cost).toBeCloseTo(0.15, 5)
  })

  it('uses expected default wattage per profile', () => {
    expect(defaultWatts('OUTLET')).toBe(100)
    expect(defaultWatts('SAFETY_OUTLET')).toBe(1500)
    expect(defaultWatts('LIGHT')).toBe(9)
    expect(defaultWatts('CAMERA')).toBe(5)
    expect(defaultWatts('MULTI_SWITCH')).toBe(60)
    expect(defaultWatts('MULTI_SWITCH', 5)).toBe(300)
  })

  it('rejects a negative channel count or active duration', () => {
    expect(() => defaultWatts('MULTI_SWITCH', 0)).toThrow(RangeError)
    expect(() => energyKwh(100, -1)).toThrow(RangeError)
  })

  it('estimates a report from its active duration', () => {
    const start = 1_000_000
    const report = calculateUsageReport(
      [on(start), off(start + HOUR)],
      { periodStartMillis: start, periodEndMillis: start + 2 * HOUR },
    )
    const result = estimateReport('LIGHT', report)
    expect(result.energyKwh).toBeCloseTo(9 / 1000, 6)
    expect(result.cost).toBeCloseTo((9 / 1000) * DEFAULT_PRICE_PER_KWH, 6)
  })

  it('accumulates energy independently across multi-switch channels', () => {
    const start = 1_000_000
    const report = calculateUsageReport(
      [
        on(start, 'channel-1'), off(start + HOUR, 'channel-1'),
        on(start, 'channel-2'), off(start + 2 * HOUR, 'channel-2'),
      ],
      { periodStartMillis: start, periodEndMillis: start + 3 * HOUR },
    )
    const result = estimateReport('MULTI_SWITCH', report)
    expect(result.energyKwh).toBeCloseTo((3 * 60) / 1000, 6)
  })

  it('formats energy compactly', () => {
    expect(formatEnergy(0)).toBe('0 kWh')
    expect(formatEnergy(0.42)).toBe('0.42 kWh')
    expect(formatEnergy(1.2)).toBe('1.2 kWh')
    expect(formatEnergy(1.25)).toBe('1.25 kWh')
    expect(formatEnergy(2)).toBe('2 kWh')
  })
})