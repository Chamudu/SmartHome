import { initializeApp } from 'firebase/app'
import { getAuth, signInWithEmailAndPassword, signOut } from 'firebase/auth'
import {
  collection,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  limit,
  query,
  serverTimestamp,
  setLogLevel,
  setDoc,
  where,
} from 'firebase/firestore'

const requiredEnvironment = [
  'SEED_FIREBASE_API_KEY',
  'SEED_FIREBASE_AUTH_DOMAIN',
  'SEED_FIREBASE_PROJECT_ID',
  'SEED_FIREBASE_APP_ID',
  'SEED_OWNER_EMAIL',
  'SEED_OWNER_PASSWORD',
  'SEED_SIMULATOR_EMAIL',
  'SEED_SIMULATOR_PASSWORD',
]

const missing = requiredEnvironment.filter((name) => !process.env[name])
if (missing.length > 0) {
  throw new Error(`Missing environment variables: ${missing.join(', ')}`)
}

const homeId = process.env.SEED_HOME_ID ?? 'demo-home'
const app = initializeApp({
  apiKey: process.env.SEED_FIREBASE_API_KEY,
  authDomain: process.env.SEED_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.SEED_FIREBASE_PROJECT_ID,
  storageBucket: process.env.SEED_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.SEED_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.SEED_FIREBASE_APP_ID,
})
const auth = getAuth(app)
const database = getFirestore(app)
setLogLevel('silent')

const simulatorCredential = await signInWithEmailAndPassword(
  auth,
  process.env.SEED_SIMULATOR_EMAIL,
  process.env.SEED_SIMULATOR_PASSWORD,
)
const simulatorUid = simulatorCredential.user.uid
await signOut(auth)

const ownerCredential = await signInWithEmailAndPassword(
  auth,
  process.env.SEED_OWNER_EMAIL,
  process.env.SEED_OWNER_PASSWORD,
)
const ownerUid = ownerCredential.user.uid

try {
  const homeReference = doc(database, 'homes', homeId)
  const ownerMembershipReference = doc(
    database,
    'homes',
    homeId,
    'members',
    ownerUid,
  )
  const ownerMembership = {
    role: 'OWNER',
    active: true,
    createdAt: serverTimestamp(),
  }

  if (!(await readableDocumentExists(ownerMembershipReference))) {
    try {
      await setDoc(ownerMembershipReference, ownerMembership)
    } catch (error) {
      if (!isPermissionDenied(error)) throw error

      await setDoc(homeReference, {
        name: 'Primary home',
        timezone: 'Asia/Colombo',
        createdBy: ownerUid,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
      process.stdout.write(`create home ${homeId}\n`)
      await setDoc(ownerMembershipReference, ownerMembership)
    }
    process.stdout.write(`create owner membership ${ownerUid}\n`)
  } else {
    process.stdout.write(`skip existing owner membership ${ownerUid}\n`)
  }

  await createWhenMissing(
    doc(database, 'homes', homeId, 'members', simulatorUid),
    { role: 'SIMULATOR', active: true, createdAt: serverTimestamp() },
    `simulator membership ${simulatorUid}`,
  )

  const groundFloorId = await ensureFloor(0, 'demo-ground-floor', 'Ground floor', 12, 16)
  const firstFloorId = await ensureFloor(1, 'demo-first-floor', 'First floor', 12, 12)

  await ensureRoom(groundFloorId, 'living-room', 'Living room', 0, 0, 6, 6)
  await ensureRoom(groundFloorId, 'utility-room', 'Utility room', 6, 0, 6, 6)
  await ensureRoom(firstFloorId, 'bedroom', 'Bedroom', 0, 0, 6, 8)
  await ensureRoom(firstFloorId, 'office', 'Office', 6, 0, 6, 8)

  await ensureDevice('demo-outlet', baseDevice('Main outlet', 'OUTLET', groundFloorId, 'living-room', 1, 1, {}))
  await ensureDevice('demo-switch', baseDevice('Hall switch', 'MULTI_SWITCH', groundFloorId, 'living-room', 3, 2, {
    channels: [1, 2, 3].map((number) => ({
      id: `channel-${number}`,
      name: `Switch ${number}`,
      desiredStatus: 'OFF',
      reportedStatus: 'OFF',
      requestId: null,
    })),
  }))
  await ensureDevice('demo-iron', baseDevice('Utility iron', 'SAFETY_OUTLET', groundFloorId, 'utility-room', 8, 2, {
    maxOnDurationSeconds: 900,
    activatedAt: null,
    cutoffDueAt: null,
  }))
  await ensureDevice('demo-light', baseDevice('Bedroom light', 'LIGHT', firstFloorId, 'bedroom', 2, 2, {
    schedule: {
      enabled: false,
      startLocalTime: '18:00',
      endLocalTime: '22:00',
      timezone: 'Asia/Colombo',
      lastEvaluatedAt: null,
    },
  }))
  await ensureDevice('demo-camera', baseDevice('Office camera', 'CAMERA', firstFloorId, 'office', 8, 2, {
    mediaType: 'SNAPSHOT',
    mediaUri: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=640&q=80',
    capturedAt: serverTimestamp(),
  }))

  process.stdout.write(`Demo data ready in homes/${homeId}.\n`)
} finally {
  await signOut(auth)
}

async function ensureFloor(level, fallbackId, name, gridColumns, gridRows) {
  const floors = collection(database, 'homes', homeId, 'floors')
  const existing = await getDocs(query(floors, where('level', '==', level), limit(1)))
  if (!existing.empty) {
    const floor = existing.docs[0]
    const data = floor.data()
    if (data.gridColumns < gridColumns || data.gridRows < gridRows) {
      throw new Error(
        `Existing level ${level} floor ${floor.id} must be at least ${gridColumns}x${gridRows} for demo rooms.`,
      )
    }
    const id = floor.id
    process.stdout.write(`reuse floor ${id} at level ${level}\n`)
    return id
  }

  await setDoc(doc(floors, fallbackId), {
    name,
    level,
    gridColumns,
    gridRows,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  })
  process.stdout.write(`create floor ${fallbackId}\n`)
  return fallbackId
}

async function ensureRoom(floorId, roomId, name, column, row, width, height) {
  await createWhenMissing(
    doc(database, 'homes', homeId, 'floors', floorId, 'rooms', roomId),
    { name, column, row, width, height, createdAt: serverTimestamp(), updatedAt: serverTimestamp() },
    `room ${floorId}/${roomId}`,
  )
}

async function ensureDevice(deviceId, data) {
  await createWhenMissing(
    doc(database, 'homes', homeId, 'devices', deviceId),
    data,
    `device ${deviceId}`,
  )
}

async function createWhenMissing(reference, data, label) {
  if ((await getDoc(reference)).exists()) {
    process.stdout.write(`skip existing ${label}\n`)
    return
  }
  await setDoc(reference, data)
  process.stdout.write(`create ${label}\n`)
}

async function readableDocumentExists(reference) {
  try {
    return (await getDoc(reference)).exists()
  } catch (error) {
    if (isPermissionDenied(error)) return false
    throw error
  }
}

function isPermissionDenied(error) {
  return typeof error === 'object'
    && error !== null
    && 'code' in error
    && error.code === 'permission-denied'
}

function baseDevice(name, profile, floorId, roomId, column, row, config) {
  return {
    name,
    profile,
    floorId,
    roomId,
    position: { column, row },
    desired: { status: 'OFF', requestId: null, requestedBy: null, requestedAt: null },
    reported: { status: 'OFF', requestId: null, updatedAt: serverTimestamp(), errorCode: null },
    commandState: 'IDLE',
    config,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }
}
