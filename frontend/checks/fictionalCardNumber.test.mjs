import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fictionalCardNumber } from '../src/api/fictionalCardNumber.js'

function cardFor(accountId) {
  return {id: 9, label: 'Test card', maskedNumber: '•••• ' + String(accountId).padStart(12, '0').slice(-4), expiryMonth: 12, expiryYear: 2035, numberEntryHint: `For this simulation, enter 0000 followed by account ID ${accountId} padded to 12 digits with leading zeros.`}
}
test('assigned account details reconstruct the fictional number, including the maximum supported ID', () => {
  for (const id of [1, 42, 999999999999]) {
    const number = fictionalCardNumber(cardFor(id), id)
    assert.ok(number.length === 16 && number.startsWith('0000') && Number(number.slice(4)) === id)
  }
})
test('a card instruction for a different account cannot be used', () => {
  assert.ok(fictionalCardNumber(cardFor(42), 43) === '')
})
test('mismatched last four digits prevent reveal or prefill', () => {
  assert.ok(fictionalCardNumber({...cardFor(42), maskedNumber: '•••• 9999'}, 42) === '')
})
test('unknown or changed entry instructions fail closed', () => {
  assert.ok(fictionalCardNumber({...cardFor(42), numberEntryHint: 'Unknown profile'}, 42) === '')
})
test('unsupported account IDs do not produce fictional credentials', () => {
  for (const id of [0, -1, 1.5, NaN, 1000000000000]) assert.ok(fictionalCardNumber(cardFor(id), id) === '')
})
test('legacy cards retain their original number rule and mask check', () => {
  const legacy = {...cardFor(42), maskedNumber: '•••• 4242', numberEntryHint: 'For this legacy classroom card, enter 4242 repeated four times.'}
  assert.ok(fictionalCardNumber(legacy, 42) === '4242'.repeat(4))
  assert.ok(fictionalCardNumber({...legacy, maskedNumber: '•••• 0042'}, 42) === '')
})
