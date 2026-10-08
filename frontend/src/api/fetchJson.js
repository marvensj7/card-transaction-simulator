/** @type {() => string} */
let readToken = () => ''
/** @type {() => void} */
let onUnauthorized = () => {}

/** @param {() => string} getToken @param {() => void} expire */
export function configureAuthentication(getToken, expire) {
  readToken = getToken
  onUnauthorized = expire
}

export class ApiError extends Error {
  /** @param {string} message @param {number} status @param {Record<string, string>} [fields] */
  constructor(message, status, fields = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

/** @param {string} path @param {'GET' | 'POST' | 'PATCH'} [method] @param {Record<string, unknown>} [body] */
export async function fetchJson(path, method = 'GET', body) {
  const publicAuth = path === '/api/auth/login' || path === '/api/auth/register'
  const token = publicAuth ? '' : readToken()
  /** @type {Record<string, string>} */
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  /** @type {RequestInit} */
  const options = { method, credentials: 'omit', headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }
  let response
  try { response = await fetch(path, options) }
  catch { throw new ApiError('Cannot reach Credit Circuit. Check your connection and try again.', 0) }
  let data
  try { data = await response.json() }
  catch {
    if (response.ok) throw new ApiError('Credit Circuit returned an unreadable response. Please try again.', 0)
  }
  if (!response.ok) {
    if (response.status === 401 && !publicAuth) onUnauthorized()
    const fallback = `The request failed (HTTP ${response.status}). Please try again.`
    /** @param {unknown} value */
    function safeMessage(value) {
      if (typeof value !== 'string' || !value.trim() || value.length > 300) return fallback
      for (const secret of [body?.password, body?.testCardNumber, body?.testSecurityCode, token]) {
        if (typeof secret === 'string' && secret && value.includes(secret)) return fallback
      }
      return value.trim()
    }
    /** @type {Record<string, string>} */
    const fields = {}
    if (data?.fields && typeof data.fields === 'object') {
      for (const [key, value] of Object.entries(data.fields)) fields[key] = safeMessage(value)
    }
    throw new ApiError(safeMessage(data?.message), response.status, fields)
  }
  return data
}
