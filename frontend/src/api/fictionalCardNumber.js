// Reconstruct only the two fictional rules already supplied by the backend.
// The result is used in React memory, never in URLs, storage, or logs.
/** @param {import('./types.js').DemoCard} card @param {number} accountId */
export function fictionalCardNumber(card, accountId) {
  if (!Number.isSafeInteger(accountId) || accountId < 1 || accountId > 999999999999) return ''
  let number = ''
  if (card.numberEntryHint === `For this simulation, enter 0000 followed by account ID ${accountId} padded to 12 digits with leading zeros.`) {
    number = '0000' + String(accountId).padStart(12, '0')
  } else if (card.numberEntryHint === 'For this legacy classroom card, enter 4242 repeated four times.') {
    number = '4242'.repeat(4)
  }
  if (number && card.maskedNumber === '•••• ' + number.slice(-4)) return number
  return ''
}
