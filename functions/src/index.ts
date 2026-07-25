import { initializeApp } from 'firebase-admin/app'
import { FieldValue, Timestamp, getFirestore } from 'firebase-admin/firestore'
import { logger } from 'firebase-functions'
import { onDocumentUpdated } from 'firebase-functions/v2/firestore'
import { onSchedule } from 'firebase-functions/v2/scheduler'
import {
  cutoffDueMillis,
  isSafetyCutoffDue,
  shouldClearSafetyTimer,
  shouldStartSafetyTimer,
  type SafetySnapshot,
} from './safetyDecision.js'
import { scheduledPowerState } from './lightSchedule.js'

initializeApp()
const database = getFirestore()
const REGION = 'asia-south1'
const DEVICE_PATH = 'homes/{homeId}/devices/{deviceId}'

export const trackSafetyOutlet = onDocumentUpdated(
  { document: DEVICE_PATH, region: REGION },
  async (event) => {
    const beforeDocument = event.data?.before
    const afterDocument = event.data?.after
    if (!beforeDocument?.exists || !afterDocument?.exists) return

    const before = toSafetySnapshot(beforeDocument.data())
    const after = toSafetySnapshot(afterDocument.data())
    if (!shouldStartSafetyTimer(before, after) && !shouldClearSafetyTimer(before, after)) return

    await database.runTransaction(async (transaction) => {
      const currentDocument = await transaction.get(afterDocument.ref)
      if (!currentDocument.exists) return
      const currentData = currentDocument.data()
      if (currentData == null) return
      const current = toSafetySnapshot(currentData)

      if (shouldStartSafetyTimer(before, current)) {
        const activatedAt = Timestamp.now()
        const cutoffDueAt = Timestamp.fromMillis(
          cutoffDueMillis(activatedAt.toMillis(), current.maxOnDurationSeconds!),
        )
        transaction.update(afterDocument.ref, {
          'config.activatedAt': activatedAt,
          'config.cutoffDueAt': cutoffDueAt,
          updatedAt: FieldValue.serverTimestamp(),
        })
      } else if (current.reportedStatus !== 'ON' &&
        (current.activatedAtMillis != null || current.cutoffDueAtMillis != null)) {
        transaction.update(afterDocument.ref, {
          'config.activatedAt': null,
          'config.cutoffDueAt': null,
          updatedAt: FieldValue.serverTimestamp(),
        })
      }
    })
  },
)

export const enforceSafetyCutoffs = onSchedule(
  { schedule: 'every 1 minutes', region: REGION, timeZone: 'UTC' },
  async () => {
    const now = Timestamp.now()
    const dueDevices = await database.collectionGroup('devices')
      .where('profile', '==', 'SAFETY_OUTLET')
      .where('reported.status', '==', 'ON')
      .where('config.cutoffDueAt', '<=', now)
      .get()

    await Promise.all(dueDevices.docs.map((device) => enforceCutoff(device.ref, now)))
    logger.info('Safety cutoff scan complete.', { candidates: dueDevices.size })
  },
)

export const enforceLightSchedules = onSchedule(
  { schedule: 'every 1 minutes', region: REGION, timeZone: 'UTC' },
  async () => {
    const now = Timestamp.now()
    const lights = await database.collectionGroup('devices')
      .where('profile', '==', 'LIGHT')
      .where('config.schedule.enabled', '==', true)
      .get()

    await Promise.all(lights.docs.map(async (light) => {
      const data = light.data()
      const target = scheduledPowerState(
        now.toDate(),
        data.config?.schedule?.startLocalTime as string,
        data.config?.schedule?.endLocalTime as string,
        data.config?.schedule?.timezone as string,
      )
      if (target == null || data.desired?.status === target) return

      const requestId = `light-schedule-${Math.floor(now.toMillis() / 60_000)}-${target}`
      await light.ref.update({
        'desired.status': target,
        'desired.requestId': requestId,
        'desired.requestedBy': 'AUTOMATION',
        'desired.requestedAt': now,
        commandState: 'PENDING',
        'config.schedule.lastEvaluatedAt': now,
        updatedAt: now,
      })
    }))
    logger.info('Light schedule scan complete.', { candidates: lights.size })
  },
)

async function enforceCutoff(
  deviceReference: FirebaseFirestore.DocumentReference,
  now: Timestamp,
): Promise<void> {
  await database.runTransaction(async (transaction) => {
    const currentDocument = await transaction.get(deviceReference)
    if (!currentDocument.exists) return
    const currentData = currentDocument.data()
    if (currentData == null) return
    const current = toSafetySnapshot(currentData)
    if (!isSafetyCutoffDue(current, now.toMillis())) return

    const dueMillis = current.cutoffDueAtMillis!
    const requestId = `safety-cutoff-${dueMillis}`
    const homeReference = deviceReference.parent.parent
    if (homeReference == null) return
    const eventReference = deviceReference.collection('events').doc(requestId)
    const alertReference = homeReference.collection('alerts').doc(`${deviceReference.id}-${requestId}`)

    transaction.update(deviceReference, {
      'desired.status': 'OFF',
      'desired.requestId': requestId,
      'desired.requestedBy': 'AUTOMATION',
      'desired.requestedAt': now,
      'reported.status': 'OFF',
      'reported.requestId': requestId,
      'reported.updatedAt': now,
      'reported.errorCode': null,
      commandState: 'APPLIED',
      'config.activatedAt': null,
      'config.cutoffDueAt': null,
      updatedAt: now,
    })
    transaction.set(eventReference, {
      type: 'SAFETY_CUTOFF',
      fromStatus: 'ON',
      toStatus: 'OFF',
      origin: 'AUTOMATION',
      actorId: null,
      requestId,
      reason: 'MAX_ON_DURATION_EXCEEDED',
      occurredAt: now,
      metadata: {
        maxOnDurationSeconds: current.maxOnDurationSeconds,
        activatedAt: current.activatedAtMillis == null
          ? null
          : Timestamp.fromMillis(current.activatedAtMillis),
        cutoffDueAt: Timestamp.fromMillis(dueMillis),
      },
    })
    transaction.set(alertReference, {
      deviceId: deviceReference.id,
      eventId: eventReference.id,
      severity: 'CRITICAL',
      type: 'SAFETY_CUTOFF',
      message: 'Device was switched off after reaching its maximum active duration.',
      createdAt: now,
      readBy: {},
    })
  })
}

function toSafetySnapshot(data: FirebaseFirestore.DocumentData): SafetySnapshot {
  const activatedAt = data.config?.activatedAt as Timestamp | null | undefined
  const cutoffDueAt = data.config?.cutoffDueAt as Timestamp | null | undefined
  return {
    profile: data.profile as string | undefined,
    reportedStatus: data.reported?.status as string | undefined,
    maxOnDurationSeconds: data.config?.maxOnDurationSeconds as number | undefined,
    activatedAtMillis: activatedAt?.toMillis() ?? null,
    cutoffDueAtMillis: cutoffDueAt?.toMillis() ?? null,
  }
}
