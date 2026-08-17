import { describe, expect, it } from 'vitest'
import { scheduledPowerState } from './lightSchedule.js'

describe('scheduledPowerState', () => {
  it('turns on inside a daytime window and off at its exclusive end', () => {
    expect(scheduledPowerState(
      new Date('2026-07-25T13:00:00Z'), '18:00', '22:00', 'Asia/Colombo',
    )).toBe('ON')
    expect(scheduledPowerState(
      new Date('2026-07-25T16:30:00Z'), '18:00', '22:00', 'Asia/Colombo',
    )).toBe('OFF')
  })

  it('supports a window crossing midnight', () => {
    expect(scheduledPowerState(
      new Date('2026-07-25T20:00:00Z'), '18:00', '06:00', 'Asia/Colombo',
    )).toBe('ON')
    expect(scheduledPowerState(
      new Date('2026-07-25T06:30:00Z'), '18:00', '06:00', 'Asia/Colombo',
    )).toBe('OFF')
  })

  it('returns null for invalid time or timezone configuration', () => {
    const now = new Date('2026-07-25T13:00:00Z')
    expect(scheduledPowerState(now, '25:00', '22:00', 'Asia/Colombo')).toBeNull()
    expect(scheduledPowerState(now, '18:00', '18:00', 'Asia/Colombo')).toBeNull()
    expect(scheduledPowerState(now, '18:00', '22:00', 'Not/AZone')).toBeNull()
  })
})
