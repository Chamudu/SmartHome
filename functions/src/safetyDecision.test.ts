import { describe, expect, it } from 'vitest'
import {
  cutoffDueMillis,
  isSafetyCutoffDue,
  shouldClearSafetyTimer,
  shouldStartSafetyTimer,
} from './safetyDecision.js'

describe('safety timer decisions', () => {
  it('starts only when a safety outlet reports an ON transition', () => {
    expect(shouldStartSafetyTimer(
      { profile: 'SAFETY_OUTLET', reportedStatus: 'OFF' },
      { profile: 'SAFETY_OUTLET', reportedStatus: 'ON', maxOnDurationSeconds: 900 },
    )).toBe(true)
    expect(shouldStartSafetyTimer(
      { profile: 'OUTLET', reportedStatus: 'OFF' },
      { profile: 'OUTLET', reportedStatus: 'ON', maxOnDurationSeconds: 900 },
    )).toBe(false)
  })

  it('does not restart a timer already initialized by a retry', () => {
    expect(shouldStartSafetyTimer(
      { profile: 'SAFETY_OUTLET', reportedStatus: 'OFF' },
      {
        profile: 'SAFETY_OUTLET',
        reportedStatus: 'ON',
        maxOnDurationSeconds: 900,
        activatedAtMillis: 1_000,
      },
    )).toBe(false)
  })

  it('clears timer metadata when reported state leaves ON', () => {
    expect(shouldClearSafetyTimer(
      { profile: 'SAFETY_OUTLET', reportedStatus: 'ON' },
      { profile: 'SAFETY_OUTLET', reportedStatus: 'OFF', cutoffDueAtMillis: 20_000 },
    )).toBe(true)
  })

  it('uses an inclusive trusted deadline comparison', () => {
    const snapshot = {
      profile: 'SAFETY_OUTLET',
      reportedStatus: 'ON',
      cutoffDueAtMillis: 61_000,
    }
    expect(isSafetyCutoffDue(snapshot, 60_999)).toBe(false)
    expect(isSafetyCutoffDue(snapshot, 61_000)).toBe(true)
  })

  it('calculates deadline in milliseconds and rejects unsafe bounds', () => {
    expect(cutoffDueMillis(1_000, 60)).toBe(61_000)
    expect(() => cutoffDueMillis(1_000, 59)).toThrow(RangeError)
    expect(() => cutoffDueMillis(1_000, 14_401)).toThrow(RangeError)
  })
})
