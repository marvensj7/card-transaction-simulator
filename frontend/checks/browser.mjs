import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import v8ToIstanbul from 'v8-to-istanbul'
import coverageLibrary from 'istanbul-lib-coverage'
import reportLibrary from 'istanbul-lib-report'
import reports from 'istanbul-reports'
import { createFixtures } from './verificationFixtures.mjs'

const frontend = process.env.DEMO_FRONTEND_URL || 'http://127.0.0.1:5173'
const backend = process.env.DEMO_API_URL || 'http://127.0.0.1:8080'
const output = new URL('../../outputs/03_Verification/', import.meta.url)
await mkdir(output, {recursive: true})
const results = []
let current = ''
async function check(name, action) {
  current = name; await action(); await collectCoverage(); results.push({name, passed: true}); console.log(`PASS ${name}`)
}
const fixtures = await createFixtures()
const browser = await chromium.launch({headless: true})
const context = await browser.newContext({viewport: {width: 1440, height: 900}})
await context.addInitScript(() => {
  for (const method of ['setItem', 'getItem', 'removeItem']) Storage.prototype[method] = () => { throw new Error('Browser storage must not hold authentication.') }
})
const page = await context.newPage()
const coverage = coverageLibrary.createCoverageMap({})
await page.coverage.startJSCoverage({resetOnNavigation: false})
async function collectCoverage(restart = true) {
  for (const entry of await page.coverage.stopJSCoverage()) {
    const path = new URL(entry.url).pathname
    if (!path.startsWith('/src/') || !/\.(jsx|js)$/.test(path)) continue
    const converter = v8ToIstanbul(fileURLToPath(new URL('..' + path, import.meta.url)), 0, {source: entry.source})
    await converter.load()
    converter.applyCoverage(entry.functions)
    coverage.merge(converter.toIstanbul())
  }
  if (restart) await page.coverage.startJSCoverage({resetOnNavigation: false})
}
let token = ''
page.on('request', request => {
  const authorization = request.headers().authorization
  if (authorization?.startsWith('Bearer ')) token = authorization.slice(7)
})
async function navigation(name) {
  if (await page.getByRole('button', {name: 'Menu', exact: true}).isVisible() && !await page.locator('#main-navigation').isVisible()) await page.getByRole('button', {name: 'Menu', exact: true}).click()
  await page.locator('#main-navigation').getByRole('link', {name, exact: true}).click()
}
async function findAdminAccount(section, action) {
  await expect(section.getByRole('table')).toBeVisible()
  for (let attempts = 0; !await section.getByRole('button', {name: action, exact: true}).isVisible() && attempts < 20; attempts++) {
    const [response] = await Promise.all([page.waitForResponse(response => response.url().includes('/api/admin/accounts?')), section.getByRole('button', {name: 'Next page', exact: true}).click()])
    const result = await response.json()
    await expect(section.getByText(`Page ${result.page + 1} of ${result.totalPages}`)).toBeVisible()
  }
  await expect(section.getByRole('button', {name: action, exact: true})).toBeVisible()
}
async function signOut() {
  if (!await page.locator('#main-navigation').isVisible()) await page.getByRole('button', {name: 'Menu', exact: true}).click()
  await page.getByRole('button', {name: 'Sign out', exact: true}).click()
  await expect(page.getByRole('heading', {name: 'Sign in', exact: true})).toBeVisible()
}
async function signIn(email) {
  await page.getByLabel('Email', {exact: true}).fill(email)
  await page.getByLabel('Password', {exact: true}).fill(fixtures.password)
  await page.getByRole('button', {name: 'Sign in', exact: true}).click()
  await expect(page.getByRole('heading', {name: email === fixtures.admin.email ? 'Administration' : 'Dashboard', exact: true})).toBeVisible()
}
async function api(path, method = 'GET', body, bearer = token) {
  const response = await fetch(backend + path, {method, headers: {Authorization: `Bearer ${bearer}`, ...(body ? {'Content-Type': 'application/json'} : {})}, ...(body ? {body: JSON.stringify(body)} : {})})
  return {status: response.status, data: await response.json()}
}
async function accessible() {
  const audit = await new AxeBuilder({page}).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  assert.equal(audit.violations.length, 0, `Accessibility rule failures: ${audit.violations.map(item => item.id).join(', ')}`)
}
let original, purchaseId, customerToken, accountId, assignedCard
try {
  await check('Home teaser, desktop layout, skip link and keyboard focus', async () => {
    await page.goto(frontend)
    await expect(page.getByRole('heading', {level: 1})).toContainText('CreditCircuit.')
    await page.keyboard.press('Tab')
    await expect(page.getByRole('link', {name: 'Skip to content'})).toBeFocused()
    await page.keyboard.press('Enter')
    await expect(page.locator('#main-content')).toBeFocused()
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('home-desktop.png', output)), fullPage: true})
  })
  await check('3D card flips with pointer and keyboard, fits narrow screens, and respects reduced motion', async () => {
    const flip = page.getByRole('button', {name: 'Flip fictional card', exact: true})
    const rotator = page.locator('.credit-card-rotator')
    await expect(flip).toHaveAttribute('aria-pressed', 'false')
    await flip.click()
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await expect(page.locator('.credit-card-back')).toHaveAttribute('aria-hidden', 'false')
    await expect(rotator).toHaveCSS('transform', 'matrix3d(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1)')
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('card-back-desktop.png', output)), fullPage: true})
    await page.keyboard.press('Enter')
    await expect(flip).toHaveAttribute('aria-pressed', 'false')
    await page.keyboard.press('Space')
    await expect(flip).toHaveAttribute('aria-pressed', 'true')
    await expect(flip).toBeFocused()
    await page.emulateMedia({reducedMotion: 'reduce'})
    await expect(rotator).toHaveCSS('transform', 'none')
    await expect(rotator).toHaveCSS('transition-duration', '0s')
    await expect(page.locator('.credit-card-front')).toHaveCSS('visibility', 'hidden')
    await expect(page.locator('.credit-card-back')).toBeVisible()
    for (const width of [768, 390, 320]) {
      await page.setViewportSize({width, height: 844})
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `No overflow at ${width}px`)
      for (const side of [false, true]) {
        if ((await flip.getAttribute('aria-pressed')) !== String(side)) await flip.click()
        const face = page.locator(side ? '.credit-card-back' : '.credit-card-front')
        assert.ok(await face.evaluate(element => element.scrollHeight <= element.clientHeight && element.scrollWidth <= element.clientWidth), `Card content fits at ${width}px`)
        await accessible()
      }
    }
    await page.screenshot({path: fileURLToPath(new URL('card-back-phone.png', output)), fullPage: true})
    await flip.click()
    await page.emulateMedia({reducedMotion: 'no-preference'})
    await page.setViewportSize({width: 1440, height: 900})
  })
  await check('Registration, controlled sign-in, loading skeleton and empty history', async () => {
    await navigation('Sign in')
    await page.getByRole('button', {name: 'New here? Create account'}).click()
    await page.getByLabel('Display name', {exact: true}).fill('Verification Signup')
    await page.getByLabel('Email', {exact: true}).fill(fixtures.signupEmail)
    await page.getByLabel('Password', {exact: true}).fill(fixtures.password)
    await page.getByRole('button', {name: 'Create account', exact: true}).click()
    await expect(page.getByText('Your fictional account is ready. Sign in to continue.')).toBeVisible()
    await expect(page.getByLabel('Password', {exact: true})).toHaveValue('')
    await page.route('**/api/accounts', async route => { await new Promise(resolve => setTimeout(resolve, 1500)); await route.continue() })
    await signIn(fixtures.signupEmail)
    await expect(page.locator('.skeleton')).toBeVisible()
    await expect(page.getByText('$1,000.00').first()).toBeVisible()
    await page.unroute('**/api/accounts')
    customerToken = token
    const accounts = await api('/api/accounts')
    accountId = accounts.data[0].id
    assignedCard = (await api(`/api/accounts/${accountId}/cards`)).data[0]
    await expect(page.locator('.credit-card-front')).toContainText(assignedCard.maskedNumber)
    await expect(page.locator('.credit-card-front')).toContainText(`${String(assignedCard.expiryMonth).padStart(2, '0')}/${assignedCard.expiryYear}`)
    await page.getByRole('button', {name: 'Flip fictional card', exact: true}).click()
    await expect(page.locator('.credit-card-back')).toHaveAttribute('aria-hidden', 'false')
    await accessible()
    await page.getByRole('button', {name: 'Flip fictional card', exact: true}).click()
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('dashboard-desktop.png', output)), fullPage: true})
    await navigation('Transactions')
    await expect(page.getByText('No transactions yet. Your next fictional purchase will appear here.')).toBeVisible()
  })
  await check('Recoverable load failure and empty account/card states', async () => {
    await page.route('**/api/accounts', route => route.fulfill({status: 503, contentType: 'application/json', body: JSON.stringify({message: 'The demo service is temporarily unavailable.'})}))
    await navigation('Dashboard')
    await expect(page.getByRole('alert')).toContainText('The demo service is temporarily unavailable.')
    await page.unroute('**/api/accounts')
    await page.getByRole('button', {name: 'Try again', exact: true}).click()
    await expect(page.getByText('$1,000.00').first()).toBeVisible()
    await page.route('**/api/accounts/*/cards', route => route.fulfill({status: 200, contentType: 'application/json', body: '[]'}))
    await navigation('Purchase')
    await expect(page.getByText('No assigned account and card are available.')).toBeVisible()
    await page.unroute('**/api/accounts/*/cards')
    await page.route('**/api/accounts', route => route.fulfill({status: 200, contentType: 'application/json', body: '[]'}))
    await navigation('Dashboard')
    await expect(page.getByRole('heading', {name: 'No account', exact: true})).toBeVisible()
    await page.unroute('**/api/accounts')
  })
  await check('Customer route, backend role and account ownership protection', async () => {
    assert.equal((await api(`/api/accounts/${fixtures.other.accountId}/cards`)).status, 404)
    assert.equal((await api('/api/admin/accounts')).status, 403)
    await page.evaluate(() => { history.pushState({}, '', '/admin'); dispatchEvent(new PopStateEvent('popstate')) })
    await expect(page.getByRole('heading', {name: 'Access restricted'})).toBeVisible()
    await navigation('Purchase')
  })
  await check('Different assigned cards, full-number matching, safe displays and unchanged history on invalid input', async () => {
    const otherLogin = await fetch(backend + '/api/auth/login', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({email: fixtures.other.email, password: fixtures.password}),
    })
    assert.equal(otherLogin.status, 200)
    const otherToken = (await otherLogin.json()).accessToken
    const otherCard = (await api(`/api/accounts/${fixtures.other.accountId}/cards`, 'GET', undefined, otherToken)).data[0]
    const ownNumber = '0000' + String(accountId).padStart(12, '0')
    const otherNumber = '0000' + String(fixtures.other.accountId).padStart(12, '0')
    assert.ok(ownNumber !== otherNumber, 'Different accounts need different assigned fictional numbers.')
    await expect(page.getByText(assignedCard.numberEntryHint, {exact: true})).toBeVisible()
    await page.getByRole('button', {name: 'Flip fictional card', exact: true}).click()
    await expect(page.getByLabel('Fictional card number', {exact: true})).toBeVisible()
    await expect(page.getByLabel('Fictional security code', {exact: true})).toHaveValue('')
    await page.getByRole('button', {name: 'Flip fictional card', exact: true}).click()
    assert.ok(!JSON.stringify(assignedCard).includes(ownNumber), 'Card API displays remain masked.')
    const request = {
      cardId: assignedCard.id,
      testCardNumber: otherNumber,
      expiryMonth: assignedCard.expiryMonth,
      expiryYear: assignedCard.expiryYear,
      testSecurityCode: '9'.repeat(3),
      merchantName: 'Assigned card check',
      amount: '1.00',
      requestId: randomUUID(),
    }
    assert.equal((await api(`/api/accounts/${accountId}/purchases`, 'POST', request)).status, 400)
    assert.equal((await api(`/api/accounts/${accountId}/purchases`, 'POST', {...request, cardId: otherCard.id})).status, 404)
    assert.equal((await api(`/api/accounts/${accountId}/purchases`, 'POST', {...request, testCardNumber: ownNumber}, otherToken)).status, 404)
    assert.equal((await api(`/api/accounts/${accountId}/transactions`)).data.totalElements, 0)
    assert.equal((await api('/api/accounts')).data[0].outstandingBalance, 0)
    const otherResult = await api(`/api/accounts/${fixtures.other.accountId}/purchases`, 'POST', {
      ...request, cardId: otherCard.id, expiryMonth: otherCard.expiryMonth, expiryYear: otherCard.expiryYear,
    }, otherToken)
    assert.equal(otherResult.status, 201)
    assert.equal(otherResult.data.transaction.status, 'APPROVED')
  })
  await check('Purchase field errors, pending controls and identical retry after a lost response', async () => {
    await expect(page.getByLabel('Amount (USD)')).toBeVisible()
    await page.getByRole('button', {name: 'Submit purchase'}).click()
    await expect(page.getByRole('alert')).toContainText('Check the highlighted fields.')
    await expect(page.getByLabel('Amount (USD)')).toHaveAttribute('aria-invalid', 'true')
    await page.getByLabel('Fictional card number', {exact: true}).fill('0000' + String(accountId).padStart(12, '0'))
    await page.getByLabel('Fictional security code', {exact: true}).fill('9'.repeat(3))
    await page.getByLabel('Fictional merchant', {exact: true}).fill('Verification Bookstore')
    await page.getByLabel('Amount (USD)').fill('50.00')
    const bodies = []
    await page.route('**/api/accounts/*/purchases', async route => {
      bodies.push(route.request().postData())
      await new Promise(resolve => setTimeout(resolve, 300))
      if (bodies.length === 1) {
        original = route.request().postDataJSON()
        const saved = await route.fetch()
        purchaseId = (await saved.json()).transaction.id
        await route.abort('failed')
      } else await route.continue()
    })
    await page.getByRole('button', {name: 'Submit purchase'}).click()
    await expect(page.getByRole('button', {name: 'Submit purchase'})).toBeDisabled()
    await expect(page.getByRole('button', {name: 'Retry same purchase'})).toBeVisible()
    await expect(page.getByLabel('Amount (USD)')).toBeDisabled()
    await page.getByRole('button', {name: 'Retry same purchase'}).click()
    await expect(page.getByRole('heading', {name: 'Purchase approved', exact: true})).toBeVisible()
    assert.ok(bodies.length === 2 && bodies[0] === bodies[1], 'The retry must preserve every input and request ID.')
    assert.equal((await api(`/api/accounts/${accountId}/transactions`)).data.totalElements, 1)
    await expect(page.getByText('$950.00').first()).toBeVisible()
    await page.unroute('**/api/accounts/*/purchases')
  })
  await check('Saved insufficient-credit decline is an outcome on a phone layout', async () => {
    await page.setViewportSize({width: 390, height: 844})
    await page.getByRole('button', {name: 'Start another purchase'}).click()
    await page.getByLabel('Fictional card number', {exact: true}).fill('0000' + String(accountId).padStart(12, '0'))
    await page.getByLabel('Fictional security code', {exact: true}).fill('9'.repeat(3))
    await page.getByLabel('Fictional merchant', {exact: true}).fill('Verification Decline')
    await page.getByLabel('Amount (USD)').fill('2000')
    await page.getByRole('button', {name: 'Submit purchase'}).click()
    await expect(page.getByRole('heading', {name: 'Purchase declined', exact: true})).toBeVisible()
    await expect(page.getByText('There is not enough available credit.')).toBeVisible()
    await expect(page.getByText('$950.00').first()).toBeVisible()
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('purchase-phone.png', output)), fullPage: true})
  })
  await check('Refund modal focus, Escape and unchanged retry after a lost refund response', async () => {
    await navigation('Transactions')
    const refundButton = page.getByRole('button', {name: `Full refund #${purchaseId}`, exact: true})
    await refundButton.click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByRole('button', {name: 'Cancel', exact: true})).toBeFocused()
    await page.keyboard.press('Escape')
    await expect(refundButton).toBeFocused()
    await refundButton.click()
    const ids = []
    await page.route('**/api/transactions/*/refund?*', async route => {
      ids.push(new URL(route.request().url()).searchParams.get('requestId'))
      if (ids.length === 1) { await route.fetch(); await route.abort('failed') }
      else await route.continue()
    })
    await page.getByRole('button', {name: 'Confirm', exact: true}).click()
    await expect(page.getByRole('button', {name: 'Retry same refund'})).toBeVisible()
    await page.getByRole('button', {name: 'Retry same refund'}).click()
    await expect(page.getByText(/Full refund saved:/)).toBeVisible()
    assert.ok(ids.length === 2 && ids[0] === ids[1], 'Refund retries must use the original request ID.')
    await expect(page.getByText('APPROVED · Refunded')).toBeVisible()
    await page.unroute('**/api/transactions/*/refund?*')
  })
  await check('Real pagination and refunded status across separate history pages on tablet', async () => {
    for (let i = 0; i < 11; i++) assert.equal((await api(`/api/accounts/${accountId}/purchases`, 'POST', {...original, merchantName: 'Pagination fixture', amount: '0.01', requestId: randomUUID()})).status, 201)
    await navigation('Dashboard'); await navigation('Transactions')
    await page.setViewportSize({width: 768, height: 1024})
    await page.getByRole('button', {name: 'Next page', exact: true}).click()
    await expect(page.getByText('Page 2 of 2')).toBeVisible()
    await expect(page.getByText('APPROVED · Refunded')).toBeVisible()
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    const scrollRegion = page.getByRole('region', {name: 'Transaction activity, newest first', exact: true})
    await scrollRegion.focus(); await page.keyboard.press('ArrowRight')
    await expect.poll(() => scrollRegion.evaluate(element => element.scrollLeft)).toBeGreaterThan(0)
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('history-tablet.png', output)), fullPage: true})
  })
  await check('Admin account/activity paging and freeze', async () => {
    await signOut(); await signIn(fixtures.admin.email)
    await expect(page.getByRole('heading', {name: 'Administration', exact: true})).toBeVisible()
    const section = page.locator('.panel').filter({has: page.getByRole('heading', {name: 'Customer accounts', exact: true})})
    await findAdminAccount(section, `Freeze #${accountId}`)
    await page.getByRole('button', {name: `Freeze #${accountId}`, exact: true}).click()
    await page.getByRole('button', {name: 'Confirm', exact: true}).click()
    await expect(page.getByText(`Account #${accountId} is FROZEN.`, {exact: true})).toBeVisible()
    for (const endpoint of ['accounts', 'transactions']) await page.route(`**/api/admin/${endpoint}?*`, route => route.fulfill({status: 200, contentType: 'application/json', body: JSON.stringify({items: [], page: 0, size: 10, totalElements: 0, totalPages: 0})}))
    await page.getByRole('button', {name: 'Refresh accounts and activity'}).click()
    await expect(page.getByText('No customer accounts are available.')).toBeVisible()
    await expect(page.getByText('No transaction activity yet.')).toBeVisible()
    for (const endpoint of ['accounts', 'transactions']) await page.unroute(`**/api/admin/${endpoint}?*`)
    await page.getByRole('button', {name: 'Refresh accounts and activity'}).click()
    await expect(page.getByRole('table').first()).toBeVisible()
    await page.setViewportSize({width: 1440, height: 900})
    await accessible()
    await page.screenshot({path: fileURLToPath(new URL('admin-desktop.png', output)), fullPage: true})
  })
  await check('Frozen account decline, refund while frozen, and admin reactivation', async () => {
    await signOut(); await signIn(fixtures.signupEmail)
    assert.deepEqual((await api(`/api/accounts/${accountId}/cards`)).data[0], assignedCard)
    await navigation('Purchase')
    await expect(page.getByLabel('Amount (USD)')).toBeVisible()
    await page.getByLabel('Fictional card number', {exact: true}).fill('0000' + String(accountId).padStart(12, '0'))
    await page.getByLabel('Fictional security code', {exact: true}).fill('9'.repeat(3))
    await page.getByLabel('Fictional merchant', {exact: true}).fill('Frozen fixture')
    await page.getByLabel('Amount (USD)').fill('1')
    await page.getByRole('button', {name: 'Submit purchase'}).click()
    await expect(page.getByText('The account is frozen.')).toBeVisible()
    await navigation('Transactions')
    await page.getByRole('button', {name: /^Full refund #/}).first().click()
    await page.getByRole('button', {name: 'Confirm', exact: true}).click()
    await expect(page.getByText(/Full refund saved:/)).toBeVisible()
    await signOut(); await signIn(fixtures.admin.email)
    const section = page.locator('.panel').filter({has: page.getByRole('heading', {name: 'Customer accounts', exact: true})})
    await findAdminAccount(section, `Reactivate #${accountId}`)
    await page.getByRole('button', {name: `Reactivate #${accountId}`, exact: true}).click()
    await page.getByRole('button', {name: 'Confirm', exact: true}).click()
    await expect(page.getByText(`Account #${accountId} is ACTIVE.`, {exact: true})).toBeVisible()
  })
  await check('401 expiration feedback, reload clears access and signed-out URLs stay protected', async () => {
    await page.route('**/api/admin/accounts?*', route => route.fulfill({status: 401, contentType: 'application/json', body: JSON.stringify({message: 'Sign in to continue.'})}))
    await page.getByRole('button', {name: 'Refresh accounts and activity'}).click()
    await expect(page.getByRole('heading', {name: 'Sign in', exact: true})).toBeVisible()
    await expect(page.getByText('Your sign-in expired. Sign in again to continue.')).toBeVisible()
    await page.unroute('**/api/admin/accounts?*')
    await signIn(fixtures.signupEmail)
    await expect(page.getByRole('heading', {name: 'Dashboard', exact: true})).toBeVisible()
    await page.reload()
    await expect(page.getByRole('heading', {name: 'Sign in', exact: true})).toBeVisible()
    await signIn(fixtures.signupEmail)
    assert.deepEqual((await api(`/api/accounts/${accountId}/cards`)).data[0], assignedCard)
    await signOut()
    await page.goto(frontend + '/transactions')
    await expect(page.getByRole('heading', {name: 'Sign in', exact: true})).toBeVisible()
    await page.setViewportSize({width: 320, height: 844})
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    await accessible()
  })
} catch (failure) {
  results.push({name: current, passed: false, errorType: failure.name})
  const safeDetail = failure.message.replaceAll(fixtures.password, '[redacted]').replace(/[0-9]{16}/g, '[redacted]').replace(/eyJ[\w.-]+/g, '[redacted]')
  console.error(`FAIL ${current}: ${failure.name} ${safeDetail}`)
  process.exitCode = 1
} finally {
  token = ''; customerToken = ''; original = null
  await collectCoverage(false)
  const reportContext = reportLibrary.createContext({dir: fileURLToPath(output), coverageMap: coverage})
  reports.create('lcovonly', {projectRoot: fileURLToPath(new URL('../../', import.meta.url))}).execute(reportContext)
  await writeFile(new URL('frontend-coverage.json', output), JSON.stringify(coverage.getCoverageSummary(), null, 2) + '\n')
  await browser.close(); await fixtures.cleanup()
  await writeFile(new URL('browser-results.json', output), JSON.stringify({date: new Date().toISOString(), realBackend: true, database: 'MySQL', injectedFailures: ['lost purchase response after real save', 'lost refund response after real save', '401 expiration response', '503 dashboard load', 'empty account/card/admin lists'], viewports: ['1440x900', '768x1024', '390x844', '320x844'], results}, null, 2) + '\n')
}
