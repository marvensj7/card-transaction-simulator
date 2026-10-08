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

  let data
  try {
    data = await response.json()
  } catch {
    if (response.ok) {
      throw new Error('Credit Circuit returned an unreadable response. Please try again.')
    }
    // An error page or malformed JSON must not become a displayed error message.
  }

  if (!response.ok) {
    const fallback = `The request failed (HTTP ${response.status}). Please try again.`
    let message = fallback

    if (typeof data?.message === 'string' && data.message.trim() !== '') {
      message = data.message.trim()
      // The API uses fixed messages. Also suppress an accidental echo of submitted card values.
      for (const value of [body?.testCardNumber, body?.testSecurityCode]) {
        if (typeof value === 'string' && value !== '' && message.includes(value)) {
          message = fallback
          break
        }
      }
    }

    throw new Error(message)
  }

  return data
}
