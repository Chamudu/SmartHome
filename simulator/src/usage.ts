import type { DeviceEvent, DeviceStatus } from './types'

export type UsageStatus = DeviceStatus

export type UsageEvent = {
  occurredAtMillis: number
  toStatus: UsageStatus
  channelId?: string | null
}

export type SeriesUsage = {
  activationCount: number
  durationMillis: number
  ongoing: boolean
  startedBeforePeriod: boolean
  unpairedOffCount: number
}

export type UsageReportEntry = {
  key: string
  usage: SeriesUsage
}

export type UsageReport = {
  periodStartMillis: number
  periodEndMillis: number
  entries: UsageReportEntry[]
  totalActivations: number
  totalDurationMillis: number
}

const KNOWN_STATUSES: ReadonlySet<string> = new Set(['ON', 'OFF', 'ERROR', 'DISCONNECTED'])

/**
 * Mirrors functions/src/usageCalculator.ts. Pairs reported state transitions
 * into ON/OFF intervals and derives activation counts and accumulated active
 * duration for one series (a device or channel).
 */
export function calculateSeriesUsage(
  events: readonly UsageEvent[],
  options: { periodStartMillis: number; periodEndMillis: number; initialStatus?: UsageStatus },
): SeriesUsage {
  const { periodStartMillis, periodEndMillis } = options
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

export function deviceEventToUsageEvent(event: DeviceEvent): UsageEvent | null {
  if (event.occurredAt == null || event.toStatus == null) return null
  if (!KNOWN_STATUSES.has(event.toStatus)) return null
  const channelId = typeof event.metadata?.channelId === 'string' ? event.metadata.channelId : null
  return {
    occurredAtMillis: event.occurredAt.toMillis(),
    toStatus: event.toStatus,
    channelId,
  }
}

export function formatDuration(durationMillis: number): string {
  const totalMinutes = Math.floor(durationMillis / 60_000)
  if (totalMinutes < 60) return `${totalMinutes} min`
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (minutes === 0) return `${hours} h`
  return `${hours} h ${minutes.toString().padStart(2, '0')} min`
}
