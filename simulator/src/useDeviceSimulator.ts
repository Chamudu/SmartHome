import { useCallback, useEffect, useRef, useState } from 'react'
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from 'firebase/auth'
import {
  collection,
  doc,
  onSnapshot,
  runTransaction,
  serverTimestamp,
  updateDoc,
} from 'firebase/firestore'
import { auth, db, homeId } from './firebase'
import type { DeviceStatus, DeviceTwin } from './types'

export function useDeviceSimulator() {
  const [user, setUser] = useState<User | null>(null)
  const [devices, setDevices] = useState<DeviceTwin[]>([])
  const [listenerConnected, setListenerConnected] = useState(false)
  const [busyDeviceIds, setBusyDeviceIds] = useState<ReadonlySet<string>>(new Set())
  const [busyChannelKeys, setBusyChannelKeys] = useState<ReadonlySet<string>>(new Set())
  const [authBusy, setAuthBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const acknowledgements = useRef(new Set<string>())

  useEffect(() => {
    if (!auth) return
    return onAuthStateChanged(auth, setUser)
  }, [])

  useEffect(() => {
    if (!user || !db || !homeId) {
      setDevices([])
      setListenerConnected(false)
      return
    }

    const devicesReference = collection(db, 'homes', homeId, 'devices')
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
            void updateDoc(doc(devicesReference, device.id), {
              'reported.status': device.desired.status,
              'reported.requestId': requestId,
              'reported.updatedAt': serverTimestamp(),
              'reported.errorCode': null,
              commandState: 'APPLIED',
              updatedAt: serverTimestamp(),
            }).catch((cause: unknown) => {
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
      await updateDoc(doc(db, 'homes', homeId, 'devices', deviceId), {
        'reported.status': status,
        'reported.requestId': null,
        'reported.updatedAt': serverTimestamp(),
        'reported.errorCode': status === 'ERROR' ? 'SIMULATED_ERROR' : null,
        commandState: 'IDLE',
        updatedAt: serverTimestamp(),
      })
    } catch (cause) {
      setError(toMessage(cause))
    } finally {
      setBusyDeviceIds((current) => {
        const next = new Set(current)
        next.delete(deviceId)
        return next
      })
    }
  }, [])

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
      await runTransaction(db, async (transaction) => {
        const reference = doc(db!, 'homes', homeId!, 'devices', deviceId)
        const snapshot = await transaction.get(reference)
        const device = snapshot.data() as DeviceTwin | undefined
        if (!device || device.profile !== 'MULTI_SWITCH' || !('channels' in device.config)) {
          throw new Error('Multi-switch channels are unavailable.')
        }
        const channels = device.config.channels.map((channel) =>
          channel.id === channelId ? { ...channel, reportedStatus: status } : channel)
        if (!channels.some((channel) => channel.id === channelId)) {
          throw new Error('Switch channel does not exist.')
        }
        transaction.update(reference, { 'config.channels': channels, updatedAt: serverTimestamp() })
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
    const channels = device.config.channels.map((channel) =>
      channel.id === channelId
        ? { ...channel, reportedStatus: channel.desiredStatus }
        : channel)
    transaction.update(reference, { 'config.channels': channels, updatedAt: serverTimestamp() })
  })
}

function toMessage(cause: unknown): string {
  return cause instanceof Error ? cause.message : 'An unexpected simulator error occurred.'
}
