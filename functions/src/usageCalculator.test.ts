import { describe, expect, it } from 'vitest'
import {
  calculateSeriesUsage,
  calculateUsageReport,
  type UsageEvent,
} from './usageCalculator.js'

const DAY = 24 * 60 * 60 * 1_000
const HOUR = 60 * 60 * 1_000
const MINUTE = 60 * 1_000

function on(millis: number, channelId?: string): UsageEvent {
  return { occurredAtMillis: millis, toStatus: 'ON', channelId }
}

function off(millis: number, channelId?: string): UsageEvent {
  return { occurredAtMillis: millis, toStatus: 'OFF', channelId }
}

describe('series usage pairing', () => {
  it('pairs ON/OFF events into activation counts and durations', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [on(start), off(start + 30 * MINUTE), on(start + 2 * HOUR), off(start + 3 * HOUR)],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(usage.activationCount).toBe(2)
    expect(usage.durationMillis).toBe(90 * MINUTE)
    expect(usage.ongoing).toBe(false)
    expect(usage.unpairedOffCount).toBe(0)
    expect(usage.startedBeforePeriod).toBe(false)
  })

  it('handles an interval that is still open at the period end', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [on(start + HOUR)],
      { periodStartMillis: start, periodEndMillis: start + 5 * HOUR },
    )
    expect(usage.activationCount).toBe(1)
    expect(usage.durationMillis).toBe(4 * HOUR)
    expect(usage.ongoing).toBe(true)
  })

  it('counts an interval that began before the period from the period start', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [on(start - HOUR), off(start + 2 * HOUR)],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(usage.activationCount).toBe(1)
    expect(usage.durationMillis).toBe(2 * HOUR)
    expect(usage.startedBeforePeriod).toBe(true)
    expect(usage.ongoing).toBe(false)
  })

  it('does not report negative durations for missing opening pairs', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [off(start + HOUR), off(start + 2 * HOUR)],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(usage.activationCount).toBe(0)
    expect(usage.durationMillis).toBe(0)
    expect(usage.unpairedOffCount).toBe(2)
  })

  it('treats ERROR and DISCONNECTED as closing an open interval', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [on(start), { occurredAtMillis: start + HOUR, toStatus: 'ERROR' }],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(usage.activationCount).toBe(1)
    expect(usage.durationMillis).toBe(HOUR)
    expect(usage.ongoing).toBe(false)
  })

  it('ignores events outside the requested period', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage(
      [on(start - DAY), off(start - DAY + HOUR), on(start + HOUR), off(start + 2 * HOUR)],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(usage.activationCount).toBe(1)
    expect(usage.durationMillis).toBe(HOUR)
  })

  it('uses the optional initial status as a fallback opening state', () => {
    const start = 1_000_000
    const usage = calculateSeriesUsage([], {
      periodStartMillis: start,
      periodEndMillis: start + DAY,
      initialStatus: 'ON',
    })
    expect(usage.activationCount).toBe(1)
    expect(usage.durationMillis).toBe(DAY)
    expect(usage.ongoing).toBe(true)
  })

  it('rejects a period whose end precedes its start', () => {
    expect(() => calculateSeriesUsage([], {
      periodStartMillis: 1_000,
      periodEndMillis: 999,
    })).toThrow(RangeError)
  })
})

describe('usage report grouping', () => {
  it('groups channel events and combines totals', () => {
    const start = 1_000_000
    const report = calculateUsageReport(
      [
        on(start, 'channel-1'), off(start + HOUR, 'channel-1'),
        on(start + HOUR), off(start + 2 * HOUR),
        on(start, 'channel-2'),
      ],
      { periodStartMillis: start, periodEndMillis: start + DAY },
    )
    expect(report.entries).toHaveLength(3)
    const byKey = new Map(report.entries.map((entry) => [entry.key, entry.usage]))
    expect(byKey.get('')?.activationCount).toBe(1)
    expect(byKey.get('channel-1')?.activationCount).toBe(1)
    expect(byKey.get('channel-2')?.activationCount).toBe(1)
    expect(byKey.get('channel-2')?.ongoing).toBe(true)
    expect(report.totalActivations).toBe(3)
    expect(report.totalDurationMillis).toBe(2 * HOUR + DAY)
  })
})