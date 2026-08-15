import { useCallback, useEffect, useRef, useState } from 'react'
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from 'firebase/auth'
import {
  CollectionReference,
  collection,
  doc,
  limit,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  writeBatch,
} from 'firebase/firestore'
import { auth, db, homeId } from './firebase'
import type { DeviceEvent, DeviceStatus, DeviceTwin } from './types'

export function useDeviceSimulator() {
  const [user, setUser] = useState<User | null>(null)
  const [devices, setDevices] = useState<DeviceTwin[]>([])
  const [eventsByDevice, setEventsByDevice] = useState<Record<string, DeviceEvent[]>>({})
  const [listenerConnected, setListenerConnected] = useState(false)
  const [busyDeviceIds, setBusyDeviceIds] = useState<ReadonlySet<string>>(new Set())
  const [busyChannelKeys, setBusyChannelKeys] = useState<ReadonlySet<string>>(new Set())
  const [authBusy, setAuthBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const acknowledgements = useRef(new Set<string>())
  const eventUnsubscribers = useRef(new Map<string, () => void>())

  useEffect(() => {
    if (!auth) return
    return onAuthStateChanged(auth, setUser)
  }, [])

  useEffect(() => {
    if (!user || !db || !homeId) {
      setDevices([])
      setEventsByDevice({})
      eventUnsubscribers.current.forEach((unsubscribe) => unsubscribe())
      eventUnsubscribers.current.clear()
      setListenerConnected(false)
      return
    }

    const database = db
    const devicesReference = collection(database, 'homes', homeId, 'devices')
    const unsubscribe = onSnapshot(
      devicesReference,
      (snapshot) => {
        setListenerConnected(true)
        const nextDevices = snapshot.docs
          .map((deviceSnapshot) => ({
            id: deviceSnapshot.id,
            ...deviceSnapshot.data(),
          }) as DeviceTwin)
          .sort((left, right) => left.name.localeCompare(right.name))
        setDevices(nextDevices)
        setError(null)

        nextDevices.forEach((device) => {
          const requestId = device.desired.requestId
          const acknowledgementKey = `${device.id}:${requestId}`
          const requiresAcknowledgement =
            requestId !== null &&
            requestId !== device.reported.requestId &&
            !acknowledgements.current.has(acknowledgementKey)

          if (requiresAcknowledgement) {
            acknowledgements.current.add(acknowledgementKey)
const batch = writeBatch(database)
            batch.update(doc(devicesReference, device.id), {
              'reported.status': device.desired.status,
              'reported.requestId': requestId,
              'reported.updatedAt': serverTimestamp(),
              'reported.errorCode': null,
              commandState: 'APPLIED',
              updatedAt: serverTimestamp(),
            })
            if (device.reported.status !== device.desired.status) {
              const origin = ackEventOrigin(device)
              batch.set(
                eventDocument(devicesReference, device.id, stateEventId('device', requestId)),
                {
                  type: 'STATE_REPORTED',
                  fromStatus: device.reported.status,
                  toStatus: device.desired.status,
                  origin: origin.origin,
                  actorId: origin.actorId,
                  requestId,
                  reason: null,
                  occurredAt: serverTimestamp(),
                  metadata: {},
                },
              )
            }
            void batch.commit().catch((cause: unknown) => {
              acknowledgements.current.delete(acknowledgementKey)
              setError(toMessage(cause))
            })
          }


          const channels = device.profile === 'MULTI_SWITCH' && 'channels' in device.config
            ? device.config.channels
            : []
          channels.forEach((channel) => {
            if (channel.requestId === null || channel.desiredStatus === channel.reportedStatus) return
            const channelKey = `${device.id}:${channel.id}:${channel.requestId}`
            if (acknowledgements.current.has(channelKey)) return
            acknowledgements.current.add(channelKey)
            void acknowledgeSwitchChannel(device.id, channel.id, channel.requestId).catch((cause: unknown) => {
              acknowledgements.current.delete(channelKey)
              setError(toMessage(cause))
            })
          })
        })
      },
      (cause) => {
        setListenerConnected(false)
        setError(toMessage(cause))
      },
    )

    return unsubscribe
  }, [user])

  useEffect(() => {
    if (!db || !homeId) return

    const wantedDeviceIds = new Set(devices.map((device) => device.id))
    eventUnsubscribers.current.forEach((unsubscribe, deviceId) => {
      if (!wantedDeviceIds.has(deviceId)) {
        unsubscribe()
        eventUnsubscribers.current.delete(deviceId)
      }
    })

    wantedDeviceIds.forEach((deviceId) => {
      if (eventUnsubscribers.current.has(deviceId)) return
      const eventsReference = collection(db!, 'homes', homeId!, 'devices', deviceId, 'events')
      const unsubscribe = onSnapshot(
        query(eventsReference, orderBy('occurredAt', 'desc'), limit(200)),
        (snapshot) => {
          setEventsByDevice((current) => ({
            ...current,
            [deviceId]: snapshot.docs.map((document) => ({
              id: document.id,
              ...document.data(),
            }) as DeviceEvent),
          }))
        },
        () => {
          setEventsByDevice((current) => {
            if (!(deviceId in current)) return current
            const next = { ...current }
            delete next[deviceId]
            return next
          })
        },
      )
      eventUnsubscribers.current.set(deviceId, unsubscribe)
    })
  }, [devices])

  const signIn = useCallback(async (email: string, password: string) => {
    if (!auth) return
    setAuthBusy(true)
    setError(null)
    try {
      await signInWithEmailAndPassword(auth, email, password)
    } catch (cause) {
      setError(toMessage(cause))
    } finally {
      setAuthBusy(false)
    }
  }, [])

  const signOut = useCallback(async () => {
    if (!auth) return
    setError(null)
    await firebaseSignOut(auth)
  }, [])

  const reportStatus = useCallback(async (deviceId: string, status: DeviceStatus) => {
    if (!db || !homeId) return
    setBusyDeviceIds((current) => new Set(current).add(deviceId))
    setError(null)
    try {
      const batch = writeBatch(db)
      batch.update(doc(db, 'homes', homeId, 'devices', deviceId), {
        'reported.status': status,
        'reported.requestId': null,
        'reported.updatedAt': serverTimestamp(),
        'reported.errorCode': status === 'ERROR' ? 'SIMULATED_ERROR' : null,
        commandState: 'IDLE',
        updatedAt: serverTimestamp(),
      })
      const current = devices.find((device) => device.id === deviceId)
      if (current != null && current.reported.status !== status) {
        batch.set(
          eventDocument(
            collection(db, 'homes', homeId, 'devices'),
            deviceId,
            manualEventId('device'),
          ),
          {
            type: 'STATE_REPORTED',
            fromStatus: current.reported.status,
            toStatus: status,
            origin: 'SIMULATOR',
            actorId: null,
            requestId: null,
            reason: null,
            occurredAt: serverTimestamp(),
            metadata: {},
          },
        )
      }
      await batch.commit()
    } catch (cause) {
      setError(toMessage(cause))
    } finally {
      setBusyDeviceIds((current) => {
        const next = new Set(current)
        next.delete(deviceId)
        return next
      })
    }
  }, [devices])

  const reportChannelStatus = useCallback(async (
    deviceId: string,
    channelId: string,
    status: DeviceStatus,
  ) => {
    if (!db || !homeId) return
    const key = `${deviceId}:${channelId}`
    setBusyChannelKeys((current) => new Set(current).add(key))
    setError(null)
    try {
      const eventId = manualEventId(channelId)
      await runTransaction(db, async (transaction) => {
        const reference = doc(db!, 'homes', homeId!, 'devices', deviceId)
        const snapshot = await transaction.get(reference)
        const device = snapshot.data() as DeviceTwin | undefined
        if (!device || device.profile !== 'MULTI_SWITCH' || !('channels' in device.config)) {
          throw new Error('Multi-switch channels are unavailable.')
        }
        const before = device.config.channels.find((channel) => channel.id === channelId)
        const channels = device.config.channels.map((channel) =>
          channel.id === channelId ? { ...channel, reportedStatus: status } : channel)
        if (!channels.some((channel) => channel.id === channelId)) {
          throw new Error('Switch channel does not exist.')
        }
        transaction.update(reference, { 'config.channels': channels, updatedAt: serverTimestamp() })
        if (before != null && before.reportedStatus !== status) {
          transaction.set(
            eventDocument(
              collection(db!, 'homes', homeId!, 'devices'),
              deviceId,
              eventId,
            ),
            {
              type: 'STATE_REPORTED',
              fromStatus: before.reportedStatus,
              toStatus: status,
              origin: 'SIMULATOR',
              actorId: null,
              requestId: null,
              reason: null,
              occurredAt: serverTimestamp(),
              metadata: { channelId },
            },
          )
        }
      })
    } catch (cause) {
      setError(toMessage(cause))
    } finally {
      setBusyChannelKeys((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }, [])

  return {
    user,
    devices,
    eventsByDevice,
    listenerConnected,
    busyDeviceIds,
    busyChannelKeys,
    authBusy,
    error,
    signIn,
    signOut,
    reportStatus,
    reportChannelStatus,
  }
}

async function acknowledgeSwitchChannel(
  deviceId: string,
  channelId: string,
  requestId: string,
): Promise<void> {
  if (!db || !homeId) return
  await runTransaction(db, async (transaction) => {
    const reference = doc(db!, 'homes', homeId!, 'devices', deviceId)
    const snapshot = await transaction.get(reference)
    const device = snapshot.data() as DeviceTwin | undefined
    if (!device || device.profile !== 'MULTI_SWITCH' || !('channels' in device.config)) return
    const current = device.config.channels.find((channel) => channel.id === channelId)
    if (!current || current.requestId !== requestId) return
    const beforeStatus = current.reportedStatus
    const afterStatus = current.desiredStatus
    const channels = device.config.channels.map((channel) =>
      channel.id === channelId
        ? { ...channel, reportedStatus: afterStatus }
        : channel)
    transaction.update(reference, { 'config.channels': channels, updatedAt: serverTimestamp() })
    if (beforeStatus !== afterStatus) {
      transaction.set(
        eventDocument(
          collection(db!, 'homes', homeId!, 'devices'),
          deviceId,
          stateEventId(channelId, requestId),
        ),
        {
          type: 'STATE_REPORTED',
          fromStatus: beforeStatus,
          toStatus: afterStatus,
          origin: 'ANDROID',
          actorId: null,
          requestId,
          reason: null,
          occurredAt: serverTimestamp(),
          metadata: { channelId },
        },
      )
    }
  })
}

function toMessage(cause: unknown): string {
  return cause instanceof Error ? cause.message : 'An unexpected simulator error occurred.'
}

function eventDocument(
  devicesReference: CollectionReference,
  deviceId: string,
  eventId: string,
): ReturnType<typeof doc> {
  return doc(devicesReference, deviceId, 'events', eventId)
}

function stateEventId(scope: string, requestId: string): string {
  return `state-${scope}-${requestId}`
}

function manualEventId(scope: string): string {
  return `state-${scope}-manual-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function ackEventOrigin(
  device: DeviceTwin,
): { origin: 'ANDROID' | 'SIMULATOR' | 'AUTOMATION'; actorId: string | null } {
  const requestedBy = device.desired.requestedBy
  if (requestedBy === 'AUTOMATION') return { origin: 'AUTOMATION', actorId: null }
  if (requestedBy != null) return { origin: 'ANDROID', actorId: requestedBy }
  return { origin: 'SIMULATOR', actorId: null }
}
