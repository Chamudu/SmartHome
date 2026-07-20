import type { Timestamp } from 'firebase/firestore'

export type DeviceStatus = 'ON' | 'OFF' | 'ERROR' | 'DISCONNECTED'
export type PowerStatus = 'ON' | 'OFF'
export type CommandState = 'IDLE' | 'PENDING' | 'APPLIED' | 'REJECTED' | 'TIMED_OUT'

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

export type OutletTwin = {
  name: string
  profile: 'OUTLET'
  floorId: string
  roomId: string | null
  desired: DesiredState
  reported: ReportedState
  commandState: CommandState
}
