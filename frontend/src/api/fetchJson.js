/** @type {() => string} */
let readAccessToken = () => ''
/** @type {() => void} */
let expireAuthentication = () => {}

/** @param {() => string} tokenReader @param {() => void} expirationHandler */
export function configureAuthentication(tokenReader, expirationHandler) {
  readAccessToken = tokenReader
  expireAuthentication = expirationHandler
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
export async function fetchJson(path, method = 'GET', body = undefined) {
  const isPublicAuthenticationRequest = path === '/api/auth/login' || path === '/api/auth/register'
  let accessToken = ''
  if (!isPublicAuthenticationRequest) {
    accessToken = readAccessToken()
  }

  /** @type {Record<string, string>} */
  const headers = { Accept: 'application/json' }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  /** @type {RequestInit} */
  const requestOptions = { method, credentials: 'omit', headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    requestOptions.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(path, requestOptions)
  } catch {
    throw new ApiError('Cannot reach Credit Circuit. Check your connection and try again.', 0)
  }

  let responseData
  try {
    responseData = await response.json()
  } catch {
    if (response.ok) {
      // A purchase may already be saved. Status 0 keeps its original request available for retry.
      throw new ApiError('Credit Circuit returned an unreadable response. Please try again.', 0)
    }
  }

  if (!response.ok) {
    if (response.status === 401 && !isPublicAuthenticationRequest) {
      expireAuthentication()
    }
    throw createResponseError(response.status, responseData, body, accessToken)
  }
  return responseData
}

/** @param {unknown} message @param {string} fallbackMessage @param {unknown[]} sensitiveValues */
function safeMessage(message, fallbackMessage, sensitiveValues) {
  if (typeof message !== 'string' || !message.trim() || message.length > 300) {
    return fallbackMessage
  }
  for (const sensitiveValue of sensitiveValues) {
    if (typeof sensitiveValue === 'string' && sensitiveValue && message.includes(sensitiveValue)) {
      return fallbackMessage
    }
  }
  return message.trim()
}

/** @param {number} status @param {any} responseData @param {Record<string, unknown> | undefined} requestBody @param {string} accessToken */
function createResponseError(status, responseData, requestBody, accessToken) {
  const fallbackMessage = `The request failed (HTTP ${status}). Please try again.`
  const sensitiveValues = [requestBody?.password, requestBody?.testCardNumber, requestBody?.testSecurityCode, accessToken]
  /** @type {Record<string, string>} */
  const fieldErrors = {}
  if (responseData?.fields && typeof responseData.fields === 'object') {
    for (const [fieldName, fieldMessage] of Object.entries(responseData.fields)) {
      fieldErrors[fieldName] = safeMessage(fieldMessage, fallbackMessage, sensitiveValues)
    }
  }
  return new ApiError(safeMessage(responseData?.message, fallbackMessage, sensitiveValues), status, fieldErrors)
}
