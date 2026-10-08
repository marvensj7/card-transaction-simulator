import assert from 'node:assert/strict'
import { before, test } from 'node:test'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router'
import { createServer } from 'vite'

let App

before(async () => {
  const server = await createServer({
    server: { middlewareMode: true, hmr: false },
    appType: 'custom',
    logLevel: 'error',
  })

  try {
    // Vite reads the same JSX files used by the browser.
    App = (await server.ssrLoadModule('/src/App.jsx')).default
  } finally {
    await server.close()
  }
})

function renderRoute(path) {
  return renderToStaticMarkup(
    createElement(MemoryRouter, { initialEntries: [path] }, createElement(App)),
  )
}

const routes = [
  ['/', 'Credit<br/><span>Circuit.</span>'],
  ['/login', 'Sign in'],
  ['/dashboard', 'Dashboard'],
  ['/purchase', 'Purchase'],
  ['/transactions', 'Transactions'],
  ['/admin', 'Administration'],
]

for (const [path, heading] of routes) {
  test(`${path} renders its page and marks one navigation link current`, () => {
    const html = renderRoute(path)
    assert.ok(html.includes(`<h1>${heading}</h1>`))
    assert.equal((html.match(/<main\b/g) || []).length, 1)
    const currentLinks = html.match(/<a\b[^>]*aria-current="page"[^>]*>/g) || []
    assert.equal(currentLinks.length, 1)
    assert.ok(currentLinks[0].includes(`href="${path}"`))
    assert.ok(html.includes('href="#main-content"'))
    assert.ok(html.includes('id="main-content"'))
    assert.ok(html.includes('Not signed in'))
  })
}

for (const path of ['/dashboard', '/purchase', '/transactions', '/admin']) {
  test(`${path} stays unavailable without banking data or controls`, () => {
    const html = renderRoute(path)
    const main = html.slice(html.indexOf('<main'), html.indexOf('</main>'))
    assert.ok(html.includes('Secure sign-in comes first.'))
    assert.ok(html.includes('About sign-in'))
    assert.doesNotMatch(main, /<(form|input|button|select|table)\b/)
    assert.doesNotMatch(html, /\$\s*\d/)
  })
}

test('sign-in explains its status without collecting credentials', () => {
  const html = renderRoute('/login')
  const main = html.slice(html.indexOf('<main'), html.indexOf('</main>'))
  assert.ok(html.includes('Sign-in is still being built.'))
  assert.doesNotMatch(main, /<(form|input|button)\b/)
})

test('an unknown nested address offers a link home without a current nav item', () => {
  const html = renderRoute('/dashboard/missing')
  assert.ok(html.includes('<h1>Page not found</h1>'))
  assert.ok(html.includes('Return home'))
  assert.doesNotMatch(html, /aria-current="page"/)
})

test('home keeps the fictional display card and purchase-path teaser', () => {
  const html = renderRoute('/')
  assert.ok(html.includes('Fictional Credit Circuit display card'))
  assert.ok(html.includes('Card ending in 4242'))
  assert.ok(html.includes('Request'))
  assert.ok(html.includes('Checks'))
  assert.ok(html.includes('Outcome'))
})
