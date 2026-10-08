import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { fetchJson } from '../src/api/fetchJson.js'

afterEach(() => mock.restoreAll())

test('fetch helper reads JSON with a relative path and same-origin credentials', async () => {
  const accounts = [{ id: 7, ownerName: 'Demo customer', availableCredit: 900 }]
  const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json(accounts))

  assert.deepEqual(await fetchJson('/api/accounts'), accounts)
  assert.deepEqual(fetchMock.mock.calls[0].arguments, ['/api/accounts', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  }])
})

test('fetch helper serializes a supplied JSON body only when there is one', async () => {
  const body = { merchantName: 'Demo shop', amount: '25.00' }
  const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json({ saved: true }))

  assert.deepEqual(await fetchJson('/api/accounts/7/purchases', 'POST', body), { saved: true })
  assert.deepEqual(fetchMock.mock.calls[0].arguments, ['/api/accounts/7/purchases', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }])
})
