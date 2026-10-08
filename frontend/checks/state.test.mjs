import assert from 'node:assert/strict'
import { afterEach, before, beforeEach, mock, test } from 'node:test'
import { act, createElement, StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'
let App, UserUiProvider, useUserUi, ProtectedRoute, api, dom, root, router, state
let phoneLayout, mediaListeners
const user = {id: 1, displayName: 'UI fixture', email: 'fixture@example.test', role: 'USER'}

before(async () => {
  const server = await createServer({server: {middlewareMode: true, hmr: false}, appType: 'custom', logLevel: 'error'})
  try {
    App = (await server.ssrLoadModule('/src/App.jsx')).default
    const context = await server.ssrLoadModule('/src/auth/UserUiContext.jsx')
    UserUiProvider = context.UserUiProvider; useUserUi = context.useUserUi
    ProtectedRoute = (await server.ssrLoadModule('/src/auth/ProtectedRoute.jsx')).default
    api = await server.ssrLoadModule('/src/api/creditCircuitApi.js')
  } finally { await server.close() }
})
beforeEach(() => {
  mock.method(globalThis, 'fetch', async () => Response.json([]))
  dom = new JSDOM('<div id="root"></div>', {url: 'http://localhost/'})
  globalThis.window = dom.window; globalThis.document = dom.window.document
  globalThis.IS_REACT_ACT_ENVIRONMENT = true
  dom.window.scrollTo = () => {}
  mediaListeners = new Set()
  phoneLayout = {matches: true, addEventListener: (type, listener) => mediaListeners.add(listener), removeEventListener: (type, listener) => mediaListeners.delete(listener)}
  dom.window.matchMedia = () => phoneLayout
  for (const method of ['getItem', 'setItem', 'removeItem']) dom.window.Storage.prototype[method] = () => { throw new Error('Authentication must remain in memory.') }
})
afterEach(async () => {
  await act(async () => root?.unmount()); router?.dispose()
  root = null; router = null
  assert.equal(mediaListeners.size, 0)
  dom.window.close(); delete globalThis.window; delete globalThis.document; delete globalThis.IS_REACT_ACT_ENVIRONMENT
  mock.restoreAll()
})
async function mount(path = '/', element = createElement(App)) {
  router = createMemoryRouter([{path: '*', element}], {initialEntries: [path]})
  root = createRoot(document.getElementById('root'))
  await act(async () => root.render(createElement(StrictMode, null, createElement(RouterProvider, {router}))))
}
function Probe() { state = useUserUi(); return createElement('p', null, state.user?.role || 'anonymous', state.notice) }
function authTree(children = createElement(Probe)) { return createElement(UserUiProvider, null, createElement(Probe), children) }
function fixture(role = 'USER', milliseconds = 60000) { return {user: {...user, role}, accessToken: 'test-only-token', expiresAt: new Date(Date.now() + milliseconds).toISOString()} }
async function click(element) { await act(async () => element.click()) }

test('anonymous protected URLs redirect to a real sign-in form without fetching', async () => {
  for (const path of ['/dashboard', '/purchase', '/transactions', '/admin']) {
    await mount(path)
    assert.equal(router.state.location.pathname, '/login')
    assert.equal(document.querySelector('h1').textContent, 'Sign in')
    assert.equal(fetch.mock.callCount(), 0)
    await act(async () => root.unmount()); router.dispose(); root = null; router = null
  }
})
test('a verified login attaches a memory token; sign-out and remount discard it', async () => {
  await mount('/', authTree())
  await act(async () => state.signIn(fixture()))
  await api.getCurrentUser()
  assert.equal(fetch.mock.calls[0].arguments[1].headers.Authorization, 'Bearer test-only-token')
  await act(async () => state.signOut())
  await api.getCurrentUser()
  assert.equal(fetch.mock.calls[1].arguments[1].headers.Authorization, undefined)
  assert.equal(state.user, null)
  await act(async () => state.signIn(fixture()))
  await act(async () => root.unmount()); router.dispose()
  await mount('/', authTree())
  assert.equal(state.user, null)
})
test('expiration clears the credential and explains the next sign-in', async () => {
  await mount('/', authTree())
  await act(async () => state.signIn(fixture('USER', 20)))
  await act(async () => new Promise(resolve => setTimeout(resolve, 50)))
  assert.equal(state.user, null)
  assert.match(state.notice, /expired/)
  await api.getCurrentUser()
  assert.equal(fetch.mock.calls[0].arguments[1].headers.Authorization, undefined)
})
test('an API 401 clears UI authentication', async () => {
  await mount('/', authTree())
  await act(async () => state.signIn(fixture()))
  fetch.mock.mockImplementation(async () => Response.json({message: 'Sign in to continue.'}, {status: 401}))
  await act(async () => assert.rejects(api.getCurrentUser()))
  assert.equal(state.user, null)
  assert.match(state.notice, /expired/)
})
test('customer and admin route checks reject the wrong role', async () => {
  await mount('/', authTree(createElement(ProtectedRoute, {role: 'ADMIN'}, createElement('p', null, 'protected admin data'))))
  await act(async () => state.signIn(fixture()))
  assert.ok(document.body.textContent.includes('Access restricted'))
  assert.ok(!document.body.textContent.includes('protected admin data'))
  await act(async () => state.signIn(fixture('ADMIN')))
  assert.ok(document.body.textContent.includes('protected admin data'))
})
test('menu Escape closes it and returns keyboard focus', async () => {
  await mount()
  const menu = document.querySelector('.menu-toggle')
  await click(menu)
  assert.equal(menu.getAttribute('aria-expanded'), 'true')
  document.querySelector('nav a').focus()
  await act(async () => document.dispatchEvent(new dom.window.KeyboardEvent('keydown', {key: 'Escape'})))
  assert.equal(menu.getAttribute('aria-expanded'), 'false')
  assert.equal(document.activeElement, menu)
})
test('navigation focuses the new page and resize preserves visible focus', async () => {
  phoneLayout.matches = false
  await mount()
  const first = document.querySelector('nav a')
  first.focus(); phoneLayout.matches = true
  await act(async () => { for (const listener of mediaListeners) listener() })
  assert.equal(document.activeElement, document.querySelector('.menu-toggle'))
  phoneLayout.matches = false
  await act(async () => { for (const listener of mediaListeners) listener() })
  assert.equal(document.activeElement, first)
  await click(document.querySelector('nav a[href="/purchase"]'))
  assert.equal(router.state.location.pathname, '/login')
  assert.equal(document.activeElement.id, 'main-content')
})
