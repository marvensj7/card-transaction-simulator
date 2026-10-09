import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { createFixtures } from './verificationFixtures.mjs'

// Start and restart only this test's server; sensitive request fields stay in memory.
const jarPath = process.env.DEMO_RESTART_JAR
if (!jarPath) {
  throw new Error('Set DEMO_RESTART_JAR to the verified local backend JAR.')
}
const backend = 'http://127.0.0.1:8083'
// Fail before creating fixtures if this test's port already belongs to another server.
let portAlreadyInUse = false
try {
  await fetch(backend + '/v3/api-docs')
  portAlreadyInUse = true
} catch {
  // No HTTP server answered on the port reserved for this check.
}
if (portAlreadyInUse) {
  throw new Error('Port 8083 is already in use. This check needs its own backend process.')
}
const fixtures = await createFixtures()
let backendProcess
let passed = false

async function startBackend() {
  let startupFailure
  backendProcess = spawn(process.env.DEMO_JAVA || 'java', [
    '-jar', jarPath,
    '--spring.profiles.active=local',
    '--spring.config.additional-location=file:./backend/src/main/resources/',
    '--server.port=8083',
  ], {
    cwd: fileURLToPath(new URL('../../', import.meta.url)),
    stdio: 'ignore',
    windowsHide: true,
  })
  backendProcess.on('error', failure => {
    startupFailure = failure
  })
  for (let attempt = 0; attempt < 60; attempt++) {
    if (startupFailure || backendProcess.exitCode !== null) {
      throw new Error('Task-owned backend exited during startup.')
    }
    try {
      const response = await fetch(backend + '/v3/api-docs')
      if (response.ok) {
        return
      }
    } catch {
      // The local process is still starting; no application request has been sent yet.
    }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  throw new Error('Task-owned backend did not become ready.')
}

async function stopBackend() {
  if (!backendProcess || backendProcess.exitCode !== null || backendProcess.pid === undefined) {
    return
  }
  const stopped = new Promise(resolve => backendProcess.once('exit', resolve))
  backendProcess.kill()
  await stopped
  backendProcess = null
}

async function request(path, method = 'GET', body, token = '') {
  const response = await fetch(backend + path, {
    method,
    headers: {'Content-Type': 'application/json', ...(token ? {Authorization: `Bearer ${token}`} : {})},
    ...(body ? {body: JSON.stringify(body)} : {}),
  })
  return {status: response.status, data: await response.json()}
}

async function signIn() {
  const login = await request('/api/auth/login', 'POST', {
    email: fixtures.user.email,
    password: fixtures.password,
  })
  assert.equal(login.status, 200)
  return login.data.accessToken
}

try {
  await startBackend()
  const firstToken = await signIn()
  const cardPath = `/api/accounts/${fixtures.user.accountId}/cards`
  const firstCard = (await request(cardPath, 'GET', undefined, firstToken)).data[0]
  const purchase = {
    cardId: firstCard.id,
    testCardNumber: '0000' + String(fixtures.user.accountId).padStart(12, '0'),
    expiryMonth: firstCard.expiryMonth,
    expiryYear: firstCard.expiryYear,
    testSecurityCode: '9'.repeat(3),
    merchantName: 'Restart verification',
    amount: '1.00',
    requestId: randomUUID(),
  }
  const purchasePath = `/api/accounts/${fixtures.user.accountId}/purchases`
  const firstResult = await request(purchasePath, 'POST', purchase, firstToken)
  assert.equal(firstResult.status, 201)
  assert.equal(firstResult.data.transaction.status, 'APPROVED')
  await stopBackend()
  console.log('First purchase saved; restarting the task-owned backend.')
  await startBackend()

  const secondToken = await signIn()
  const secondCard = (await request(cardPath, 'GET', undefined, secondToken)).data[0]
  assert.deepEqual(secondCard, firstCard)
  const replay = await request(purchasePath, 'POST', purchase, secondToken)
  assert.equal(replay.status, 200)
  assert.equal(replay.data.transaction.id, firstResult.data.transaction.id)
  assert.equal(replay.data.account.outstandingBalance, 1)
  const history = await request(`/api/accounts/${fixtures.user.accountId}/transactions`, 'GET', undefined, secondToken)
  assert.equal(history.data.totalElements, 1)
  passed = true
  console.log('PASS actual backend restart preserves the assigned card and one saved purchase.')
} catch (failure) {
  console.error(`FAIL backend restart verification: ${failure.name}`)
  process.exitCode = 1
} finally {
  await stopBackend()
  await fixtures.cleanup()
  const restartResults = {
    date: new Date().toISOString(),
    realBackend: true,
    database: 'MySQL',
    actualBackendRestart: true,
    passed,
    credentialsExported: false,
  }
  await writeFile(
    new URL('../../outputs/03_Verification/restart-results.json', import.meta.url),
    JSON.stringify(restartResults, null, 2) + '\n',
  )
}
