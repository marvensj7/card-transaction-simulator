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
  assert.ok(html.includes('Access unavailable'), 'sign-in and account pages share the same access status')
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
  assert.doesNotMatch(html, /<p\b[^>]*aria-label=/, 'paragraphs use readable text instead of an unsupported ARIA name')
  assert.ok(html.includes('<span aria-hidden="true">•••• 4242</span>'), 'the mask is visual; assistive technology gets the card ending as text')
  assert.ok(html.includes('Request'))
  assert.ok(html.includes('Checks'))
  assert.ok(html.includes('Outcome'))
  const main = html.slice(html.indexOf('<main'), html.indexOf('</main>'))
  assert.doesNotMatch(main, /\b(approved|declined|balance|refund)\b|\$\s*\d/i, 'the home page must remain a teaser')
})
