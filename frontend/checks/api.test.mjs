import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { fetchJson } from '../src/api/fetchJson.js'
import {
  getAccounts,
  getCards,
  submitPurchase,
  getTransactions,
  refundPurchase,
  getAdminAccounts,
  getAdminTransactions,
  updateAccountStatus,
} from '../src/api/creditCircuitApi.js'

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

const requestId = 'A573E641-9391-4B32-9EC2-42F66F2CC42B'
const account = {
  id: 7,
  ownerName: 'Demo customer',
  creditLimit: 1000,
  outstandingBalance: 100,
  availableCredit: 900,
  status: 'ACTIVE',
}
const card = {
  id: 3,
  label: 'Demo card',
  maskedNumber: '•••• 4242',
  expiryMonth: 12,
  expiryYear: 2030,
}
const transaction = {
  id: 11,
  accountId: 7,
  cardId: 3,
  type: 'PURCHASE',
  status: 'APPROVED',
  amount: 25,
  outstandingAfter: 100,
  merchantName: 'Demo shop',
  reasonCode: null,
  createdAt: '2026-10-08T12:00:00Z',
  originalPurchaseId: null,
}

// Fictional request data only. Assertions avoid printing the purchase body.
const purchase = Object.freeze({
  cardId: 3,
  testCardNumber: '4242424242424242',
  expiryMonth: 12,
  expiryYear: 2030,
  testSecurityCode: '123',
  merchantName: 'Demo shop',
  amount: '25.00',
  requestId,
})

const lists = [
  ['accounts', getAccounts, '/api/accounts', [account]],
  ['cards', () => getCards(7), '/api/accounts/7/cards', [card]],
  ['history', () => getTransactions(7), '/api/accounts/7/transactions', [transaction]],
  ['admin accounts', getAdminAccounts, '/api/admin/accounts', [account]],
  ['admin history', getAdminTransactions, '/api/admin/transactions', [transaction]],
]

for (const [name, call, path, rows] of lists) {
  test(`${name} uses its GET path and returns a plain array`, async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json(rows))
    const result = await call()

    assert.ok(Array.isArray(result))
    assert.deepEqual(result, rows)
    assert.deepEqual(fetchMock.mock.calls[0].arguments, [path, {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    }])
  })
}

test('empty history stays an empty array', async () => {
  mock.method(globalThis, 'fetch', async () => Response.json([]))
  assert.deepEqual(await getTransactions(7), [])
})

for (const status of ['APPROVED', 'DECLINED']) {
  test(`purchase sends the documented JSON and returns a ${status} result with HTTP 200`, async () => {
    const result = {
      transaction: { ...transaction, status, reasonCode: status === 'DECLINED' ? 'INSUFFICIENT_CREDIT' : null },
      account,
    }
    const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json(result))
    const input = status === 'APPROVED' ? purchase : { ...purchase, amount: 25 }

    assert.deepEqual(await submitPurchase(7, input), result)
    const [path, options] = fetchMock.mock.calls[0].arguments
    assert.equal(path, '/api/accounts/7/purchases')
    assert.equal(options.method, 'POST')
    assert.equal(options.credentials, 'same-origin')
    assert.deepEqual(options.headers, { Accept: 'application/json', 'Content-Type': 'application/json' })
    assert.ok(options.body === JSON.stringify(input), 'purchase JSON preserves all eight fields and the amount type')
  })
}

test('retrying a purchase sends the same caller-supplied request ID and body', async () => {
  const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json({ transaction, account }))
  await submitPurchase(7, purchase)
  await submitPurchase(7, purchase)

  assert.equal(fetchMock.mock.callCount(), 2)
  const firstBody = fetchMock.mock.calls[0].arguments[1].body
  const retryBody = fetchMock.mock.calls[1].arguments[1].body
  assert.ok(firstBody === retryBody, 'retry JSON must remain identical')
  assert.equal(JSON.parse(retryBody).requestId, requestId)
})

test('full refund uses a POST query parameter without a JSON body or amount', async () => {
  const result = {
    transaction: { ...transaction, id: 12, type: 'REFUND', originalPurchaseId: 11 },
    account,
  }
  const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json(result))

  assert.deepEqual(await refundPurchase(11, requestId), result)
  assert.deepEqual(fetchMock.mock.calls[0].arguments, [
    `/api/transactions/11/refund?requestId=${requestId}`,
    { method: 'POST', credentials: 'same-origin', headers: { Accept: 'application/json' } },
  ])
})

for (const status of ['ACTIVE', 'FROZEN']) {
  test(`admin status sends ${status} in a PATCH query without a JSON body`, async () => {
    const updated = { ...account, status }
    const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json(updated))

    assert.deepEqual(await updateAccountStatus(7, status), updated)
    assert.deepEqual(fetchMock.mock.calls[0].arguments, [
      `/api/admin/accounts/7/status?status=${status}`,
      { method: 'PATCH', credentials: 'same-origin', headers: { Accept: 'application/json' } },
    ])
  })
}

test('query values are encoded so they cannot add extra parameters', async () => {
  const fetchMock = mock.method(globalThis, 'fetch', async () => Response.json({}))
  await refundPurchase(11, 'bad id&amount=1?')
  await updateAccountStatus(7, 'FROZEN&extra=1')

  assert.equal(fetchMock.mock.calls[0].arguments[0], '/api/transactions/11/refund?requestId=bad%20id%26amount%3D1%3F')
  assert.equal(fetchMock.mock.calls[1].arguments[0], '/api/admin/accounts/7/status?status=FROZEN%26extra%3D1')
})
