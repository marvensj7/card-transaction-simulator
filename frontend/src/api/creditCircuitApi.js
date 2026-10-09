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

// Identity is attached by fetchJson from React memory.
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
export function getTransactions(accountId, page = 0) {
  return fetchJson(`/api/accounts/${accountId}/transactions?page=${page}&size=10`)
}

/**
 * Full refunds need only the original purchase ID and a caller-supplied request ID.
 * @param {number} purchaseId
 * @param {string} requestId
 */
export function refundPurchase(purchaseId, requestId) {
  return fetchJson(`/api/transactions/${purchaseId}/refund?requestId=${encodeURIComponent(requestId)}`, 'POST')
}

export function getAdminAccounts(page = 0) {
  return fetchJson(`/api/admin/accounts?page=${page}&size=10`)
}

export function getAdminTransactions(page = 0) {
  return fetchJson(`/api/admin/transactions?page=${page}&size=10`)
}

/**
 * @param {number} accountId
 * @param {'ACTIVE' | 'FROZEN'} status
 */
export function updateAccountStatus(accountId, status) {
  return fetchJson(`/api/admin/accounts/${accountId}/status?status=${encodeURIComponent(status)}`, 'PATCH')
}

/** @param {{email: string, password: string}} credentials */
export function login(credentials) {
  return fetchJson('/api/auth/login', 'POST', credentials)
}

/** @param {{displayName: string, email: string, password: string}} registration */
export function register(registration) {
  return fetchJson('/api/auth/register', 'POST', registration)
}

export function getCurrentUser() {
  return fetchJson('/api/auth/me')
}
