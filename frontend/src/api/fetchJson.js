/**
 * Call a relative /api path with the browser's same-origin session cookie.
 * @param {string} path
 * @param {'GET' | 'POST' | 'PATCH'} [method]
 * @param {Record<string, unknown>} [body]
 */
export async function fetchJson(path, method = 'GET', body) {
  /** @type {RequestInit} */
  const options = {
    method,
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  }

  if (body !== undefined) {
    options.headers = { ...options.headers, 'Content-Type': 'application/json' }
    options.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(path, options)
  } catch {
    throw new Error('Cannot reach Credit Circuit. Check your connection and try again.')
  }

  if (!response.ok) {
    throw new Error(`The request failed (HTTP ${response.status}). Please try again.`)
  }

  return response.json()
}
