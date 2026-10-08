import assert from 'node:assert/strict'
import { afterEach, before, beforeEach, test } from 'node:test'
import { act, createElement, StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'

let App
let UserUiProvider
let useUserUi
let SiteNavigation
let AdminPage
let dom
let root
let router
let phoneLayout
let mediaListeners

before(async () => {
  const server = await createServer({
    server: { middlewareMode: true, hmr: false },
    appType: 'custom',
    logLevel: 'error',
  })

  try {
    App = (await server.ssrLoadModule('/src/App.jsx')).default
    const userUi = await server.ssrLoadModule('/src/auth/UserUiContext.jsx')
    UserUiProvider = userUi.UserUiProvider
    useUserUi = userUi.useUserUi
    SiteNavigation = (await server.ssrLoadModule('/src/components/SiteNavigation.jsx')).default
    AdminPage = (await server.ssrLoadModule('/src/pages/AdminPage.jsx')).default
  } finally {
    await server.close()
  }
})

beforeEach(() => {
  dom = new JSDOM('<div id="root"></div>', { url: 'http://localhost/' })
  globalThis.window = dom.window
  globalThis.document = dom.window.document
  globalThis.IS_REACT_ACT_ENVIRONMENT = true
  dom.window.scrollTo = () => {}

  // JSDOM has no layout engine. Browser checks cover the actual CSS breakpoint.
  mediaListeners = new Set()
  phoneLayout = {
    matches: true,
    addEventListener: (type, listener) => mediaListeners.add(listener),
    removeEventListener: (type, listener) => mediaListeners.delete(listener),
  }
  dom.window.matchMedia = () => phoneLayout

  // Rendering/navigation must work even when browser storage is unavailable.
  for (const method of ['getItem', 'setItem', 'removeItem']) {
    dom.window.Storage.prototype[method] = () => {
      throw new Error('UI state must stay in memory.')
    }
  }
})

afterEach(async () => {
  await act(async () => root?.unmount())
  router?.dispose()
  root = null
  router = null
  assert.equal(mediaListeners.size, 0, 'unmount removes the resize listener')
  dom.window.close()
  delete globalThis.window
  delete globalThis.document
  delete globalThis.IS_REACT_ACT_ENVIRONMENT
})

async function mount(path = '/', element = createElement(App)) {
  router = createMemoryRouter([{ path: '*', element }], {
    initialEntries: [path],
  })
  root = createRoot(document.getElementById('root'))
  await act(async () => {
    root.render(createElement(StrictMode, null, createElement(RouterProvider, { router })))
  })
}

function menuButton() {
  return document.querySelector('.menu-toggle')
}

function assertMenu(open) {
  assert.equal(menuButton().getAttribute('aria-expanded'), String(open))
  const menuId = menuButton().getAttribute('aria-controls')
  assert.equal(document.getElementById(menuId).getAttribute('data-open'), String(open))
}

async function click(element) {
  await act(async () => element.click())
}

test('starts anonymous and stays unavailable across links, history, and a remount', async () => {
  await mount()
  for (const path of ['/dashboard', '/purchase', '/transactions', '/admin', '/login', '/']) {
    await click(document.querySelector(`nav a[href="${path}"]`))
    assert.equal(router.state.location.pathname, path)
    assert.equal(document.querySelector('.navigation-status').textContent, 'Not signed in')
    assert.equal(document.querySelector('nav a[aria-current="page"]').getAttribute('href'), path)
    assert.equal(document.activeElement.id, 'main-content')
    assert.equal(document.querySelectorAll('main form, main input, main button, main table').length, 0)
    if (path !== '/' && path !== '/login') {
      assert.equal(document.querySelector('#access-status').textContent, 'Secure sign-in comes first.')
    }
  }
  await act(async () => router.navigate(-1))
  assert.equal(document.querySelector('h1').textContent, 'Sign in')
  await act(async () => router.navigate(1))
  assert.equal(router.state.location.pathname, '/')
  await act(async () => root.unmount())
  router.dispose()
  await mount('/admin')
  assert.equal(document.querySelector('.navigation-status').textContent, 'Not signed in')
  assert.equal(document.querySelector('#access-status').textContent, 'Secure sign-in comes first.')
})

test('shared user UI details cannot unlock tools and are discarded on remount', async () => {
  let setUser
  function StateProbe() {
    setUser = useUserUi().setUser
    return null
  }
  const page = createElement(UserUiProvider, null,
    createElement(StateProbe), createElement(SiteNavigation), createElement(AdminPage))
  await mount('/admin', page)

  // Test-only UI details: no server session is created or treated as verified.
  await act(async () => setUser({ id: 1, displayName: 'UI test only', role: 'ADMIN' }))
  assert.equal(document.querySelector('.navigation-status').textContent, 'Signed in')
  assert.equal(document.querySelector('.notice-label').textContent, 'Access unavailable')
  assert.equal(document.querySelector('#access-status').textContent, 'Account tools are still being built.')
  assert.equal(document.querySelectorAll('main form, main input, main button, main table').length, 0)
  await act(async () => setUser(null))
  assert.equal(document.querySelector('.navigation-status').textContent, 'Not signed in')
  assert.equal(document.querySelector('#access-status').textContent, 'Secure sign-in comes first.')
  await act(async () => setUser({ id: 1, displayName: 'UI test only', role: 'ADMIN' }))
  await act(async () => root.unmount())
  router.dispose()
  await mount('/admin', page)
  assert.equal(document.querySelector('.navigation-status').textContent, 'Not signed in')
  assert.equal(document.querySelector('#access-status').textContent, 'Secure sign-in comes first.')
})

test('menu toggles; Escape closes it and returns focus, then removes its key listener', async () => {
  await mount()
  assertMenu(false)
  await click(menuButton())
  assertMenu(true)
  document.querySelector('nav a').focus()
  await act(async () => document.dispatchEvent(new dom.window.KeyboardEvent('keydown', { key: 'Escape' })))
  assertMenu(false)
  assert.equal(document.activeElement, menuButton())
  assert.equal(mediaListeners.size, 0)
  document.getElementById('main-content').focus()
  await act(async () => document.dispatchEvent(new dom.window.KeyboardEvent('keydown', { key: 'Escape' })))
  assert.equal(document.activeElement.id, 'main-content')
  await click(menuButton())
  await click(menuButton())
  assertMenu(false)
})

test('a menu destination closes the menu and focuses the new page', async () => {
  await mount()
  await click(menuButton())
  await click(document.querySelector('nav a[href="/purchase"]'))
  assertMenu(false)
  assert.equal(document.activeElement.id, 'main-content')
  assert.equal(document.title, 'Purchase | Credit Circuit')
  assert.equal(document.querySelector('#access-status').textContent, 'Secure sign-in comes first.')
})

test('choosing the current page closes the menu and keeps focus on its button', async () => {
  await mount()
  await click(menuButton())
  await click(document.querySelector('nav a[href="/"]'))
  assertMenu(false)
  assert.equal(document.activeElement, menuButton())
})

test('external navigation, query changes, and back/forward close the menu', async () => {
  await mount('/login')
  for (const destination of ['/dashboard', '/dashboard?view=summary', -1, 1]) {
    await click(menuButton())
    assertMenu(true)
    await act(async () => router.navigate(destination))
    assertMenu(false)
  }
  await click(menuButton())
  await click(document.querySelector('main a[href="/login"]'))
  assertMenu(false)
  assert.equal(document.querySelector('h1').textContent, 'Sign in')
})

test('switching to desktop closes the menu; an open menu cleans up on unmount', async () => {
  await mount()
  await click(menuButton())
  assert.equal(mediaListeners.size, 1)
  phoneLayout.matches = false
  await act(async () => {
    for (const listener of mediaListeners) listener()
  })
  assertMenu(false)
  assert.equal(mediaListeners.size, 0)
  phoneLayout.matches = true
  await click(menuButton())
  assertMenu(true)
  // afterEach unmounts while open and checks cleanup, including StrictMode setup.
})
