export type UsageStatus = 'ON' | 'OFF' | 'ERROR' | 'DISCONNECTED'

export interface UsageEvent {
  occurredAtMillis: number
  toStatus: UsageStatus
  channelId?: string | null
}

export interface SeriesUsage {
  activationCount: number
  durationMillis: number
  ongoing: boolean
  startedBeforePeriod: boolean
  unpairedOffCount: number
}

export interface UsageReportEntry {
  key: string
  usage: SeriesUsage
}

export interface UsageReport {
  periodStartMillis: number
  periodEndMillis: number
  entries: UsageReportEntry[]
  totalActivations: number
  totalDurationMillis: number
}

const KNOWN_STATUSES: ReadonlySet<string> = new Set(['ON', 'OFF', 'ERROR', 'DISCONNECTED'])

/**
 * Pairs reported state transitions into ON/OFF intervals and derives activation
 * counts and accumulated active duration for one series (a device or channel).
 *
 * Only `ON` is treated as active; `OFF`, `ERROR`, and `DISCONNECTED` all close
 * an open interval so a failed or unreachable device never appears indefinitely
 * active. Missing pairs are surfaced explicitly rather than producing negative
 * or misleading totals:
 *
 * - An ON interval still open at the period end is truncated at the period end
 *   and flagged as `ongoing`.
 * - An OFF transition with no matching open interval is counted in
 *   `unpairedOffCount` and contributes nothing to duration.
 * - An interval that began before the period is measured from the period start
 *   and flagged with `startedBeforePeriod`.
 */
export function calculateSeriesUsage(
  events: readonly UsageEvent[],
  options: { periodStartMillis: number; periodEndMillis: number; initialStatus?: UsageStatus },
): SeriesUsage {
  const periodStartMillis = options.periodStartMillis
  const periodEndMillis = options.periodEndMillis
  if (periodEndMillis < periodStartMillis) {
    throw new RangeError('Usage period end must not precede its start.')
  }
  const initialStatus = options.initialStatus ?? 'OFF'

  const ordered = events
    .filter((event): event is UsageEvent => KNOWN_STATUSES.has(event.toStatus))
    .slice()
    .sort((left, right) => left.occurredAtMillis - right.occurredAtMillis)

  let state = initialStatus
  let activeAtStart = state === 'ON'
  let activatedBeforeStart = false
  for (const event of ordered) {
    if (event.occurredAtMillis > periodStartMillis) break
    state = event.toStatus
    const turnsOn = event.toStatus === 'ON'
    activeAtStart = turnsOn
    activatedBeforeStart = turnsOn && event.occurredAtMillis < periodStartMillis
  }

  let activationCount = activeAtStart ? 1 : 0
  let durationMillis = 0
  let unpairedOffCount = 0
  let openStartMillis: number | null = activeAtStart ? periodStartMillis : null

  for (const event of ordered) {
    const timestamp = event.occurredAtMillis
    if (timestamp <= periodStartMillis) continue
    if (timestamp > periodEndMillis) break

    if (event.toStatus === 'ON') {
      if (openStartMillis === null) {
        openStartMillis = timestamp
        activationCount += 1
      }
    } else if (openStartMillis !== null) {
      durationMillis += timestamp - Math.max(openStartMillis, periodStartMillis)
      openStartMillis = null
    } else if (event.toStatus === 'OFF') {
      unpairedOffCount += 1
    }
  }

  let ongoing = false
  if (openStartMillis !== null) {
    durationMillis += periodEndMillis - Math.max(openStartMillis, periodStartMillis)
    ongoing = true
  }

  return {
    activationCount,
    durationMillis,
    ongoing,
    startedBeforePeriod: activeAtStart && activatedBeforeStart,
    unpairedOffCount,
  }
}

/**
 * Groups events by optional `channelId` and returns one usage entry per series
 * together with combined totals. Events without a channel represent the
 * device-level series keyed by the empty string.
 */
export function calculateUsageReport(
  events: readonly UsageEvent[],
  options: { periodStartMillis: number; periodEndMillis: number; initialStatus?: UsageStatus },
): UsageReport {
  const seriesKeys = [...new Set(events.map((event) => event.channelId ?? ''))].sort()
  const entries = seriesKeys.map((key) => ({
    key,
    usage: calculateSeriesUsage(
      events.filter((event) => (event.channelId ?? '') === key),
      options,
    ),
  }))

  return {
    periodStartMillis: options.periodStartMillis,
    periodEndMillis: options.periodEndMillis,
    entries,
    totalActivations: entries.reduce((sum, entry) => sum + entry.usage.activationCount, 0),
    totalDurationMillis: entries.reduce((sum, entry) => sum + entry.usage.durationMillis, 0),
  }
}