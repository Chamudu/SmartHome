import type { Timestamp } from 'firebase/firestore'

export type DeviceStatus = 'ON' | 'OFF' | 'ERROR' | 'DISCONNECTED'
export type PowerStatus = 'ON' | 'OFF'
export type CommandState = 'IDLE' | 'PENDING' | 'APPLIED' | 'REJECTED' | 'TIMED_OUT'
export type DeviceProfile = 'OUTLET' | 'MULTI_SWITCH' | 'SAFETY_OUTLET' | 'LIGHT' | 'CAMERA'
export type EventOrigin = 'ANDROID' | 'SIMULATOR' | 'AUTOMATION' | 'SYSTEM'

export type FloorSummary = {
  id: string
  name: string
  level: number
}

export type DeviceEvent = {
  id: string
  type: string
  fromStatus: DeviceStatus | null
  toStatus: DeviceStatus | null
  origin: EventOrigin
  actorId: string | null
  requestId: string | null
  reason: string | null
  occurredAt: Timestamp | null
  metadata: Record<string, unknown>
}

export type DesiredState = {
  status: PowerStatus
  requestId: string | null
  requestedBy: string | null
  requestedAt: Timestamp | null
}

export type ReportedState = {
  status: DeviceStatus
  requestId: string | null
  updatedAt: Timestamp | null
  errorCode: string | null
}

export type SwitchChannel = {
  id: string
  name: string
  desiredStatus: PowerStatus
  reportedStatus: DeviceStatus
  requestId: string | null
}

export type DeviceConfig =
  | Record<string, never>
  | { channels: SwitchChannel[] }
  | { maxOnDurationSeconds: number; activatedAt: Timestamp | null; cutoffDueAt: Timestamp | null }
  | { schedule: { enabled: boolean; startLocalTime: string; endLocalTime: string; timezone: string } }
  | { mediaType: 'SNAPSHOT' | 'MOCK_STREAM'; mediaUri: string; capturedAt: Timestamp | null }

export type DeviceTwin = {
  id: string
  name: string
  profile: DeviceProfile
  floorId: string
  roomId: string | null
  position: { column: number; row: number }
  desired: DesiredState
  reported: ReportedState
  commandState: CommandState
  config: DeviceConfig
}

export type DeviceTwinData = Omit<DeviceTwin, 'id'>
