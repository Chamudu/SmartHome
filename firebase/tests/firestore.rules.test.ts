import { readFileSync } from 'node:fs'
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from '@firebase/rules-unit-testing'
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  type Firestore,
} from 'firebase/firestore'
import {
  afterAll,
  beforeAll,
  beforeEach,
  describe,
  it,
} from 'vitest'

const PROJECT_ID = 'demo-smart-home'
const HOME_ID = 'demo-home'
const DEVICE_ID = 'main-outlet'

const OWNER_UID = 'owner-user'
const SIMULATOR_UID = 'simulator-user'
const OUTSIDER_UID = 'outsider-user'

let testEnvironment: RulesTestEnvironment

function outletReference(database: Firestore) {
  return doc(database, 'homes', HOME_ID, 'devices', DEVICE_ID)
}

beforeAll(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8080,
      rules: readFileSync(
        new URL('../firestore.rules', import.meta.url),
        'utf8',
      ),
    },
  })
})

beforeEach(async () => {
  await testEnvironment.clearFirestore()

  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore()
    const now = new Date()

    await setDoc(doc(database, 'homes', HOME_ID), {
      name: 'Primary home',
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })

    await setDoc(
      doc(database, 'homes', HOME_ID, 'members', OWNER_UID),
      {
        role: 'OWNER',
        active: true,
      },
    )

    await setDoc(
      doc(database, 'homes', HOME_ID, 'members', SIMULATOR_UID),
      {
        role: 'SIMULATOR',
        active: true,
      },
    )

    await setDoc(
      doc(database, 'homes', HOME_ID, 'floors', 'ground-floor'),
      {
        name: 'Ground floor',
        level: 0,
        gridColumns: 12,
        gridRows: 16,
        createdAt: now,
        updatedAt: now,
      },
    )

    await setDoc(
      doc(
        database,
        'homes',
        HOME_ID,
        'floors',
        'ground-floor',
        'rooms',
        'utility',
      ),
      {
        name: 'Utility',
        column: 0,
        row: 0,
        width: 4,
        height: 4,
        createdAt: now,
        updatedAt: now,
      },
    )

    await setDoc(outletReference(database), {
      name: 'Main outlet',
      profile: 'OUTLET',
      floorId: 'ground-floor',
      roomId: 'utility',
      position: {
        row: 1,
        column: 1,
      },
      desired: {
        status: 'OFF',
        requestId: null,
        requestedBy: null,
        requestedAt: null,
      },
      reported: {
        status: 'OFF',
        requestId: null,
        updatedAt: now,
        errorCode: null,
      },
      commandState: 'IDLE',
      createdAt: now,
      updatedAt: now,
    })
  })
})

afterAll(async () => {
  await testEnvironment.cleanup()
})

describe('home authorization', () => {
  it('allows an active owner to read their home', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      getDoc(doc(database, 'homes', HOME_ID)),
    )
  })

  it('denies an unauthenticated user', async () => {
    const database =
      testEnvironment.unauthenticatedContext().firestore()

    await assertFails(
      getDoc(doc(database, 'homes', HOME_ID)),
    )
  })

  it('denies an authenticated user without membership', async () => {
    const database =
      testEnvironment.authenticatedContext(OUTSIDER_UID).firestore()

    await assertFails(
      getDoc(doc(database, 'homes', HOME_ID)),
    )
  })
})

describe('outlet authorization', () => {
  it('allows an owner to request desired power state', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      updateDoc(outletReference(database), {
        'desired.status': 'ON',
        'desired.requestId': 'request-1',
        'desired.requestedBy': OWNER_UID,
        'desired.requestedAt': new Date(),
        commandState: 'PENDING',
        updatedAt: new Date(),
      }),
    )
  })

  it('denies a simulator changing desired power state', async () => {
    const database =
      testEnvironment.authenticatedContext(SIMULATOR_UID).firestore()

    await assertFails(
      updateDoc(outletReference(database), {
        'desired.status': 'ON',
        'desired.requestId': 'malicious-request',
        'desired.requestedBy': SIMULATOR_UID,
        'desired.requestedAt': new Date(),
        commandState: 'PENDING',
        updatedAt: new Date(),
      }),
    )
  })

  it('allows a simulator to report applied state', async () => {
    const database =
      testEnvironment.authenticatedContext(SIMULATOR_UID).firestore()

    await assertSucceeds(
      updateDoc(outletReference(database), {
        'reported.status': 'ON',
        'reported.requestId': 'request-1',
        'reported.updatedAt': new Date(),
        'reported.errorCode': null,
        commandState: 'APPLIED',
        updatedAt: new Date(),
      }),
    )
  })

  it('denies an owner forging reported hardware state', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertFails(
      updateDoc(outletReference(database), {
        'reported.status': 'ON',
        'reported.requestId': 'request-1',
        'reported.updatedAt': new Date(),
        'reported.errorCode': null,
        commandState: 'APPLIED',
        updatedAt: new Date(),
      }),
    )
  })
})

describe('floor layout authorization and validation', () => {
  it('allows an owner to create a valid floor', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      setDoc(doc(database, 'homes', HOME_ID, 'floors', 'first-floor'), {
        name: 'First floor',
        level: 1,
        gridColumns: 10,
        gridRows: 12,
        createdAt: new Date(),
        updatedAt: new Date(),
      }),
    )
  })

  it('denies a floor with grid dimensions outside supported limits', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertFails(
      setDoc(doc(database, 'homes', HOME_ID, 'floors', 'invalid-floor'), {
        name: 'Invalid floor',
        level: 2,
        gridColumns: 3,
        gridRows: 41,
        createdAt: new Date(),
        updatedAt: new Date(),
      }),
    )
  })

  it('allows an owner to create a room inside the floor grid', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      setDoc(
        doc(database, 'homes', HOME_ID, 'floors', 'ground-floor', 'rooms', 'kitchen'),
        {
          name: 'Kitchen',
          column: 0,
          row: 0,
          width: 5,
          height: 6,
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      ),
    )
  })

  it('denies a room extending beyond the floor grid', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertFails(
      setDoc(
        doc(database, 'homes', HOME_ID, 'floors', 'ground-floor', 'rooms', 'invalid-room'),
        {
          name: 'Invalid room',
          column: 10,
          row: 14,
          width: 4,
          height: 4,
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      ),
    )
  })

  it('denies the simulator creating floor data', async () => {
    const database =
      testEnvironment.authenticatedContext(SIMULATOR_UID).firestore()

    await assertFails(
      setDoc(doc(database, 'homes', HOME_ID, 'floors', 'simulator-floor'), {
        name: 'Simulator floor',
        level: 3,
        gridColumns: 10,
        gridRows: 10,
        createdAt: new Date(),
        updatedAt: new Date(),
      }),
    )
  })
})

describe('device placement validation', () => {
  it('allows an owner to place a device inside a floor grid', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      updateDoc(outletReference(database), {
        floorId: 'ground-floor',
        roomId: null,
        position: { column: 3, row: 4 },
        updatedAt: new Date(),
      }),
    )
  })

  it('allows an owner to place a device inside its assigned room', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertSucceeds(
      updateDoc(outletReference(database), {
        floorId: 'ground-floor',
        roomId: 'utility',
        position: { column: 2, row: 3 },
        updatedAt: new Date(),
      }),
    )
  })

  it('denies a device position outside its assigned room', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertFails(
      updateDoc(outletReference(database), {
        floorId: 'ground-floor',
        roomId: 'utility',
        position: { column: 5, row: 5 },
        updatedAt: new Date(),
      }),
    )
  })

  it('denies a device position outside the floor grid', async () => {
    const database =
      testEnvironment.authenticatedContext(OWNER_UID).firestore()

    await assertFails(
      updateDoc(outletReference(database), {
        floorId: 'ground-floor',
        roomId: null,
        position: { column: 12, row: 16 },
        updatedAt: new Date(),
      }),
    )
  })
})

describe('device creation validation', () => {
  function channels(count: number) {
    return Array.from({ length: count }, (_, index) => ({
      id: `channel-${index + 1}`,
      name: `Switch ${index + 1}`,
      desiredStatus: 'OFF',
      reportedStatus: 'OFF',
      requestId: null,
    }))
  }

  function newDevice(profile: string, config: Record<string, unknown>) {
    const now = new Date()
    return {
      name: 'New device',
      profile,
      floorId: 'ground-floor',
      roomId: 'utility',
      position: { column: 2, row: 2 },
      desired: {
        status: 'OFF', requestId: null, requestedBy: null, requestedAt: null,
      },
      reported: {
        status: 'OFF', requestId: null, updatedAt: now, errorCode: null,
      },
      commandState: 'IDLE',
      config,
      createdAt: now,
      updatedAt: now,
    }
  }

  it('allows an owner to create a valid five-channel switch', async () => {
    const database = testEnvironment.authenticatedContext(OWNER_UID).firestore()
    await assertSucceeds(
      setDoc(
        doc(database, 'homes', HOME_ID, 'devices', 'hall-switch'),
        newDevice('MULTI_SWITCH', { channels: channels(5) }),
      ),
    )
  })

  it('allows complete outlet, safety, light, and camera profiles', async () => {
    const database = testEnvironment.authenticatedContext(OWNER_UID).firestore()
    await assertSucceeds(setDoc(
      doc(database, 'homes', HOME_ID, 'devices', 'new-outlet'),
      newDevice('OUTLET', {}),
    ))
    await assertSucceeds(setDoc(
      doc(database, 'homes', HOME_ID, 'devices', 'new-safety'),
      newDevice('SAFETY_OUTLET', {
        maxOnDurationSeconds: 900, activatedAt: null, cutoffDueAt: null,
      }),
    ))
    await assertSucceeds(setDoc(
      doc(database, 'homes', HOME_ID, 'devices', 'new-light'),
      newDevice('LIGHT', {
        schedule: {
          enabled: false,
          startLocalTime: '18:00',
          endLocalTime: '22:00',
          timezone: 'Asia/Colombo',
          lastEvaluatedAt: null,
        },
      }),
    ))
    await assertSucceeds(setDoc(
      doc(database, 'homes', HOME_ID, 'devices', 'new-camera'),
      newDevice('CAMERA', {
        mediaType: 'SNAPSHOT',
        mediaUri: 'https://placehold.co/640x360',
        capturedAt: new Date(),
      }),
    ))
  })

  it('denies an unsupported switch channel count', async () => {
    const database = testEnvironment.authenticatedContext(OWNER_UID).firestore()
    await assertFails(
      setDoc(
        doc(database, 'homes', HOME_ID, 'devices', 'bad-switch'),
        newDevice('MULTI_SWITCH', { channels: channels(4) }),
      ),
    )
  })

  it('denies a camera with a non-HTTPS media URI', async () => {
    const database = testEnvironment.authenticatedContext(OWNER_UID).firestore()
    await assertFails(
      setDoc(
        doc(database, 'homes', HOME_ID, 'devices', 'bad-camera'),
        newDevice('CAMERA', {
          mediaType: 'SNAPSHOT',
          mediaUri: 'javascript:alert(1)',
          capturedAt: new Date(),
        }),
      ),
    )
  })

  it('denies simulator-created devices', async () => {
    const database = testEnvironment.authenticatedContext(SIMULATOR_UID).firestore()
    await assertFails(
      setDoc(
        doc(database, 'homes', HOME_ID, 'devices', 'forged-device'),
        newDevice('OUTLET', {}),
      ),
    )
  })
})
