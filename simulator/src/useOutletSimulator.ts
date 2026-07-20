import { useCallback, useEffect, useRef, useState } from 'react'
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from 'firebase/auth'
import {
  doc,
  onSnapshot,
  serverTimestamp,
  updateDoc,
} from 'firebase/firestore'
import { auth, db, homeId, outletId } from './firebase'
import type { DeviceStatus, OutletTwin } from './types'

export function useOutletSimulator() {
  const [user, setUser] = useState<User | null>(null)
  const [device, setDevice] = useState<OutletTwin | null>(null)
  const [listenerConnected, setListenerConnected] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const acknowledgements = useRef(new Set<string>())

  useEffect(() => {
    if (!auth) return
    return onAuthStateChanged(auth, setUser)
  }, [])

  useEffect(() => {
    if (!user || !db || !homeId || !outletId) {
      setDevice(null)
      setListenerConnected(false)
      return
    }

    const deviceReference = doc(db, 'homes', homeId, 'devices', outletId)
    const unsubscribe = onSnapshot(
      deviceReference,
      (snapshot) => {
        setListenerConnected(true)
        if (!snapshot.exists()) {
          setDevice(null)
          return
        }

        const nextDevice = snapshot.data() as OutletTwin
        setDevice(nextDevice)
        setError(null)

        const requestId = nextDevice.desired.requestId
        const requiresAcknowledgement =
          requestId !== null &&
          requestId !== nextDevice.reported.requestId &&
          !acknowledgements.current.has(requestId)

        if (requiresAcknowledgement) {
          acknowledgements.current.add(requestId)
          void updateDoc(deviceReference, {
            'reported.status': nextDevice.desired.status,
            'reported.requestId': requestId,
            'reported.updatedAt': serverTimestamp(),
            'reported.errorCode': null,
            commandState: 'APPLIED',
            updatedAt: serverTimestamp(),
          }).catch((cause: unknown) => {
            acknowledgements.current.delete(requestId)
            setError(toMessage(cause))
          })
        }
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
    setBusy(true)
    setError(null)
    try {
      await signInWithEmailAndPassword(auth, email, password)
    } catch (cause) {
      setError(toMessage(cause))
    } finally {
      setBusy(false)
    }
  }, [])

  const signOut = useCallback(async () => {
    if (!auth) return
    setError(null)
    await firebaseSignOut(auth)
  }, [])

  const reportStatus = useCallback(async (status: DeviceStatus) => {
    if (!db || !homeId || !outletId) return
    setBusy(true)
    setError(null)
    try {
      await updateDoc(doc(db, 'homes', homeId, 'devices', outletId), {
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
      setBusy(false)
    }
  }, [])

  return {
    user,
    device,
    listenerConnected,
    busy,
    error,
    signIn,
    signOut,
    reportStatus,
  }
}

function toMessage(cause: unknown): string {
  return cause instanceof Error ? cause.message : 'An unexpected simulator error occurred.'
}
