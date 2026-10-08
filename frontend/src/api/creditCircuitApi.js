import { fetchJson } from './fetchJson.js'

/**
 * Fictional card fields belong only in this request, never in logs or storage.
 * @typedef {object} PurchaseRequest
 * @property {number} cardId
 * @property {string} testCardNumber
 * @property {number} expiryMonth
 * @property {number} expiryYear
 * @property {string} testSecurityCode
 * @property {string} merchantName
 * @property {string | number} amount
 * @property {string} requestId
 */

// Pages will call these only after session sign-in and CSRF protection are ready.
export function getAccounts() {
  return fetchJson('/api/accounts')
}

/** @param {number} accountId */
export function getCards(accountId) {
  return fetchJson(`/api/accounts/${accountId}/cards`)
}

/**
 * The page creates requestId once and keeps the same purchase for an uncertain retry.
 * @param {number} accountId
 * @param {PurchaseRequest} purchase
 */
export function submitPurchase(accountId, purchase) {
  return fetchJson(`/api/accounts/${accountId}/purchases`, 'POST', purchase)
}

/** @param {number} accountId */
export function getTransactions(accountId) {
  return fetchJson(`/api/accounts/${accountId}/transactions`)
}

/**
 * Full refunds need only the original purchase ID and a caller-supplied request ID.
 * @param {number} purchaseId
 * @param {string} requestId
 */
export function refundPurchase(purchaseId, requestId) {
  return fetchJson(`/api/transactions/${purchaseId}/refund?requestId=${encodeURIComponent(requestId)}`, 'POST')
}

export function getAdminAccounts() {
  return fetchJson('/api/admin/accounts')
}

export function getAdminTransactions() {
  return fetchJson('/api/admin/transactions')
}

/**
 * @param {number} accountId
 * @param {'ACTIVE' | 'FROZEN'} status
 */
export function updateAccountStatus(accountId, status) {
  return fetchJson(`/api/admin/accounts/${accountId}/status?status=${encodeURIComponent(status)}`, 'PATCH')
}
