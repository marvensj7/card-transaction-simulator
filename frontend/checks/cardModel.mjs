import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

// Rendering checks use public home and explicitly mocked account display data.
// No database, purchase, password, or real token is exported by this runner.
const frontend = process.env.DEMO_FRONTEND_URL || 'http://127.0.0.1:5173'
const output = new URL('../../outputs/03_Verification/', import.meta.url)
await mkdir(output, {recursive: true})
const results = []
const browser = await chromium.launch({args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader']})
const context = await browser.newContext({viewport: {width: 1440, height: 900}})
const page = await context.newPage()
const errors = []
page.on('pageerror', () => errors.push('page error'))
page.on('console', message => { if (message.type() === 'error') errors.push('rendering error') })
const flip = page.getByRole('button', {name: 'Flip fictional card', exact: true})
const canvas = page.locator('.credit-card-viewport canvas')
let currentCheck = ''
async function check(name, action) {
  currentCheck = name
  await action()
  results.push({name, passed: true})
  console.log(`PASS ${name}`)
}
async function accessible(target = page) {
  const audit = await new AxeBuilder({page: target}).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  assert.equal(audit.violations.length, 0, `Accessibility rule failures: ${audit.violations.map(item => item.id).join(', ')}`)
}
async function snapshot(name) {
  await canvas.screenshot({path: fileURLToPath(new URL(name, output))})
}

try {
  await check('Real WebGL model renders, shimmers while idle, and changes its gradient on hover', async () => {
    await page.goto(frontend)
    await expect(flip).toHaveAttribute('data-renderer', 'three')
    await expect(canvas).toBeVisible()
    await page.mouse.move(0, 0)
    const idle = await canvas.screenshot()
    await page.waitForTimeout(1000)
    const later = await canvas.screenshot()
    assert.ok(!idle.equals(later), 'Idle rendering must visibly change.')
    await snapshot('card-model-front.png')
    await flip.hover({position: {x: 340, y: 70}})
    await page.waitForTimeout(700)
    const hover = await canvas.screenshot()
    assert.ok(!hover.equals(later), 'Hover must visibly change the rendered surface.')
    await snapshot('card-model-hover.png')
    assert.equal(errors.length, 0, 'No page or shader compilation errors.')
    await accessible()
  })
  await check('Pointer and keyboard flip both faces while keeping focus', async () => {
    await flip.click()
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await page.waitForTimeout(1000)
    const back = await canvas.screenshot()
    await snapshot('card-model-back.png')
    await page.keyboard.press('Enter')
    await expect(flip).toHaveAttribute('aria-pressed', 'false')
    await page.waitForTimeout(1000)
    assert.ok(!back.equals(await canvas.screenshot()), 'Front and back must render differently.')
    await page.keyboard.press('Space')
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await expect(flip).toBeFocused()
  })
  await check('Reduced motion stops idle rendering and switches faces without animation', async () => {
    await page.emulateMedia({reducedMotion: 'reduce'})
    await page.mouse.move(0, 0)
    await page.waitForTimeout(200)
    const still = await canvas.screenshot()
    await page.waitForTimeout(500)
    assert.ok(still.equals(await canvas.screenshot()), 'Reduced motion must leave the rendered model still.')
    await page.keyboard.press('Enter')
    await expect(flip).toHaveAttribute('aria-pressed', 'false')
    assert.ok(!still.equals(await canvas.screenshot()), 'The reduced-motion front remains reachable.')
  })
  await check('Model fits tablet and phone layouts without page overflow', async () => {
    for (const width of [768, 390, 320]) {
      await page.setViewportSize({width, height: 844})
      await expect(canvas).toBeVisible()
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `No overflow at ${width}px.`)
      await accessible()
    }
    await snapshot('card-model-phone.png')
  })
  await check('Touchscreen tap flips the real model', async () => {
    const touchContext = await browser.newContext({viewport: {width: 390, height: 844}, hasTouch: true, isMobile: true})
    const touchPage = await touchContext.newPage()
    try {
      await touchPage.goto(frontend)
      const touchFlip = touchPage.getByRole('button', {name: 'Flip fictional card'})
      await expect(touchFlip).toHaveAttribute('data-renderer', 'three')
      await touchFlip.tap()
      await expect(touchFlip).toHaveAttribute('aria-pressed', 'true')
      await accessible(touchPage)
    } finally { await touchContext.close() }
  })
  await check('Unavailable WebGL keeps the ordinary accessible card usable', async () => {
    const fallbackContext = await browser.newContext()
    const fallbackPage = await fallbackContext.newPage()
    try {
      await fallbackPage.addInitScript(() => { Object.defineProperty(window, 'WebGL2RenderingContext', {value: undefined}) })
      await fallbackPage.goto(frontend)
      const fallbackFlip = fallbackPage.getByRole('button', {name: 'Flip fictional card'})
      await expect(fallbackFlip).toHaveAttribute('data-renderer', 'css')
      await expect(fallbackPage.locator('.credit-card-front')).toBeVisible()
      await fallbackFlip.click()
      await expect(fallbackFlip).toHaveAttribute('aria-pressed', 'true')
      await expect(fallbackPage.locator('.credit-card-back')).toHaveAttribute('aria-hidden', 'false')
      await accessible(fallbackPage)
    } finally { await fallbackContext.close() }
  })
  await check('A failed graphics-module download preserves the usable fallback', async () => {
    const downloadContext = await browser.newContext()
    const downloadPage = await downloadContext.newPage()
    let blocked = false
    try {
      await downloadPage.route('**/*cardScene*', route => { blocked = true; return route.abort() })
      await downloadPage.goto(frontend)
      await expect.poll(() => blocked).toBe(true)
      const fallbackFlip = downloadPage.getByRole('button', {name: 'Flip fictional card'})
      await expect(fallbackFlip).toHaveAttribute('data-renderer', 'css')
      await fallbackFlip.click()
      await expect(fallbackFlip).toHaveAttribute('aria-pressed', 'true')
    } finally { await downloadContext.close() }
  })
  await check('Lost graphics context restores the HTML card and its flip controls', async () => {
    await canvas.evaluate(element => element.getContext('webgl2').getExtension('WEBGL_lose_context').loseContext())
    await expect(flip).toHaveAttribute('data-renderer', 'css')
    await expect(canvas).toHaveCount(0)
    await flip.click()
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await accessible()
  })
  await check('Dashboard and purchase models use safe assigned details and leave form inputs separate (mock API)', async () => {
    await page.setViewportSize({width: 1440, height: 900})
    const account = {id: 42, ownerName: 'Fictional customer', creditLimit: 1000, outstandingBalance: 0, availableCredit: 1000, status: 'ACTIVE'}
    const card = {id: 42, label: 'Fictional assigned card', maskedNumber: '•••• 0042', expiryMonth: 12, expiryYear: 2035, numberEntryHint: 'Use your fictional assigned test details.'}
    await page.route(frontend + '/api/**', route => {
      const path = new URL(route.request().url()).pathname
      let body
      if (path === '/api/auth/login') {
        body = {user: {id: 42, displayName: 'Fictional customer', email: 'card-check@example.test', role: 'USER'}, accessToken: randomUUID(), expiresAt: new Date(Date.now() + 60000).toISOString()}
      } else if (path === '/api/accounts') {
        body = [account]
      } else if (path === '/api/accounts/42/cards') {
        body = [card]
      } else { throw new Error('Unexpected API call in card display check.') }
      return route.fulfill({status: 200, contentType: 'application/json', body: JSON.stringify(body)})
    })
    await page.goto(frontend + '/login')
    await page.getByLabel('Email', {exact: true}).fill('card-check@example.test')
    await page.getByLabel('Password', {exact: true}).fill(randomUUID())
    await page.getByRole('button', {name: 'Sign in', exact: true}).click()
    await expect(flip).toHaveAttribute('data-renderer', 'three')
    await expect(page.getByText('Fictional assigned card. Card ending in 0042. Expiry 12/2035.', {exact: true})).toHaveCount(1)
    await page.locator('#main-navigation').getByRole('link', {name: 'Purchase', exact: true}).click()
    await expect(flip).toHaveAttribute('data-renderer', 'three')
    await flip.click()
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByLabel('Fictional card number', {exact: true})).toHaveValue('')
    await expect(page.getByLabel('Fictional security code', {exact: true})).toHaveValue('')
    await expect(page.getByLabel('Expiry month', {exact: true})).toHaveValue('12')
    await page.setViewportSize({width: 320, height: 844})
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    await accessible()
    assert.equal(errors.length, 0, 'No rendering errors through navigation and cleanup.')
  })
} catch (error_) {
  results.push({name: currentCheck, passed: false, errorType: error_.name})
  console.error(`FAIL ${currentCheck}: ${error_.name}`)
  process.exitCode = 1
} finally {
  await browser.close()
  await writeFile(new URL('card-model-results.json', output), JSON.stringify({date: new Date().toISOString(), renderer: 'Three.js WebGL2', browser: 'Chromium with SwiftShader software graphics', realBackend: false, mockedData: 'Login and safe account/card responses for display checks only', results}, null, 2) + '\n')
}
