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