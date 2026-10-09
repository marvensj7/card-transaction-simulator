import { spawn } from 'node:child_process'
import { createRequire } from 'node:module'
import { readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { createFixtures } from './verificationFixtures.mjs'

// Supply temporary private values in memory. Never export a run environment or raw responses.
const require = createRequire(import.meta.url)
const binary = require.resolve('@postman/pm-bin-windows-x64/bin/postman.exe')
const collection = new URL('../../outputs/03_Verification/Credit_Circuit.postman_collection.json', import.meta.url)
const fixtures = await createFixtures()
let output = ''
let exitCode = 1
try {
  const variables = {
    baseUrl: process.env.DEMO_API_URL || 'http://127.0.0.1:8080',
    customerEmail: fixtures.postmanEmail,
    otherEmail: fixtures.other.email,
    adminEmail: fixtures.admin.email,
    password: fixtures.password,
    testSecurityCode: '9'.repeat(3),
  }
  const args = ['collection', 'run', fileURLToPath(collection), '--no-report-events']
  for (const [key, value] of Object.entries(variables)) {
    args.push('--env-var', `${key}=${value}`)
  }
  exitCode = await new Promise((resolve, reject) => {
    const child = spawn(binary, args, {windowsHide: true})
    child.stdout.on('data', data => output += data.toString())
    child.stderr.on('data', data => output += data.toString())
    child.on('error', reject)
    child.on('close', resolve)
  })
  const safe = output.replaceAll(fixtures.password, '[redacted]').replace(/[0-9]{16}/g, '[redacted]').replace(/eyJ[\w.-]+/g, '[redacted]').replace(/\u001b\[[0-9;]*m/g, '')
  const specification = JSON.parse(await readFile(collection, 'utf8'))
  const requestCounts = safe.match(/\|\s*requests\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/)
  const assertionCounts = safe.match(/\|\s*assertions\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|/)
  await writeFile(new URL('../../outputs/03_Verification/postman-results.txt', import.meta.url), safe)
  await writeFile(new URL('../../outputs/03_Verification/postman-results.json', import.meta.url), JSON.stringify({
    date: new Date().toISOString(),
    tool: 'Postman CLI 1.71.0',
    realBackend: true,
    database: 'MySQL',
    exportedRequests: specification.item.length,
    httpRequestsExecuted: requestCounts ? Number(requestCounts[1]) : null,
    assertionsExecuted: assertionCounts ? Number(assertionCounts[1]) : null,
    assertionsFailed: assertionCounts ? Number(assertionCounts[2]) : null,
    exitCode,
    rawResponsesExported: false,
  }, null, 2) + '\n')
  console.log(safe)
  process.exitCode = exitCode
} finally {
  output = ''
  await fixtures.cleanup()
}
