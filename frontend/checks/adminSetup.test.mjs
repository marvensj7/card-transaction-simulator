import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { randomUUID } from 'node:crypto'
import bcrypt from 'bcryptjs'
import { createFixtures } from './verificationFixtures.mjs'

test('Private admin setup creates/resets only ADMIN and rejects customer escalation', async () => {
  const fixtures = await createFixtures()
  function run(email, password) {
    return spawnSync(process.execPath, [fileURLToPath(new URL('createAdmin.mjs', import.meta.url))], {env: {...process.env, CAPSTONE_ADMIN_EMAIL: email, CAPSTONE_ADMIN_PASSWORD: password}, encoding: 'utf8', windowsHide: true})
  }
  try {
    assert.equal(run(fixtures.signupEmail, fixtures.password).status, 0)
    const nextPassword = randomUUID()
    assert.equal(run(fixtures.signupEmail, nextPassword).status, 0)
    const [users] = await fixtures.connection.execute('SELECT role,password_hash FROM app_users WHERE email=?', [fixtures.signupEmail])
    assert.equal(users[0].role, 'ADMIN')
    assert.ok(await bcrypt.compare(nextPassword, users[0].password_hash))
    assert.notEqual(run(fixtures.user.email, fixtures.password).status, 0)
    const [customer] = await fixtures.connection.execute('SELECT role FROM app_users WHERE id=?', [fixtures.user.id])
    assert.equal(customer[0].role, 'USER')
    assert.notEqual(run(fixtures.signupEmail, '').status, 0)
  } finally { await fixtures.cleanup() }
})
