export type ScheduledPowerState = 'ON' | 'OFF'

const LOCAL_TIME = /^(?:[01]\d|2[0-3]):[0-5]\d$/

export function scheduledPowerState(
  now: Date,
  startLocalTime: string,
  endLocalTime: string,
  timezone: string,
): ScheduledPowerState | null {
  if (!LOCAL_TIME.test(startLocalTime) || !LOCAL_TIME.test(endLocalTime) ||
    startLocalTime === endLocalTime) return null

  const currentMinutes = localMinutesAt(now, timezone)
  if (currentMinutes == null) return null
  const start = toMinutes(startLocalTime)
  const end = toMinutes(endLocalTime)
  const active = start < end
    ? currentMinutes >= start && currentMinutes < end
    : currentMinutes >= start || currentMinutes < end
  return active ? 'ON' : 'OFF'
}

function localMinutesAt(now: Date, timezone: string): number | null {
  try {
    const parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: timezone,
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(now)
    const hour = Number(parts.find((part) => part.type === 'hour')?.value)
    const minute = Number(parts.find((part) => part.type === 'minute')?.value)
    return Number.isInteger(hour) && Number.isInteger(minute) ? hour * 60 + minute : null
  } catch {
    return null
  }
}

function toMinutes(value: string): number {
  const parts = value.split(':')
  return Number(parts[0]) * 60 + Number(parts[1])
}
