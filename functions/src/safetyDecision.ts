export type SafetySnapshot = {
  profile?: string
  reportedStatus?: string
  maxOnDurationSeconds?: number
  activatedAtMillis?: number | null
  cutoffDueAtMillis?: number | null
}

export function shouldStartSafetyTimer(
  before: SafetySnapshot,
  after: SafetySnapshot,
): boolean {
  return after.profile === 'SAFETY_OUTLET' &&
    before.reportedStatus !== 'ON' &&
    after.reportedStatus === 'ON' &&
    after.activatedAtMillis == null &&
    isValidDuration(after.maxOnDurationSeconds)
}

export function shouldClearSafetyTimer(
  before: SafetySnapshot,
  after: SafetySnapshot,
): boolean {
  return after.profile === 'SAFETY_OUTLET' &&
    before.reportedStatus === 'ON' &&
    after.reportedStatus !== 'ON' &&
    (after.activatedAtMillis != null || after.cutoffDueAtMillis != null)
}

export function isSafetyCutoffDue(
  snapshot: SafetySnapshot,
  nowMillis: number,
): boolean {
  return snapshot.profile === 'SAFETY_OUTLET' &&
    snapshot.reportedStatus === 'ON' &&
    snapshot.cutoffDueAtMillis != null &&
    snapshot.cutoffDueAtMillis <= nowMillis
}

export function cutoffDueMillis(startMillis: number, durationSeconds: number): number {
  if (!isValidDuration(durationSeconds)) {
    throw new RangeError('Safety duration must be between 60 and 14,400 seconds.')
  }
  return startMillis + durationSeconds * 1000
}

function isValidDuration(value: number | undefined): value is number {
  return Number.isInteger(value) && value !== undefined && value >= 60 && value <= 14_400
}
