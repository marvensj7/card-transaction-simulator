import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { createFixtures } from './verificationFixtures.mjs'

const frontend = process.env.DEMO_FRONTEND_URL || 'http://127.0.0.1:5173'
const backend = process.env.DEMO_API_URL || 'http://127.0.0.1:8080'
const output = new URL('../../outputs/03_Verification/', import.meta.url)
await mkdir(output, {recursive: true})
const fixtures = await createFixtures()
const browser = await chromium.launch({args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader']})
const context = await browser.newContext({viewport: {width: 1440, height: 900}, reducedMotion: 'reduce'})
await context.addInitScript(() => {
  for (const method of ['getItem', 'setItem', 'removeItem']) Storage.prototype[method] = () => { throw new Error('Card details must stay in memory.') }
})
const page = await context.newPage()
const results = []
let current = '', sampleCode = '', adminToken = ''
const expectedNumber = '0000' + String(fixtures.user.accountId).padStart(12, '0')
const flip = page.getByRole('button', {name: 'Flip card', exact: true})
const show = page.getByRole('button', {name: 'Show details', exact: true})
const hide = page.getByRole('button', {name: 'Hide details', exact: true})
const details = page.locator('.card-revealed-details')
const canvas = page.locator('.credit-card-viewport canvas')
const errors = []
page.on('pageerror', () => errors.push('page error'))
page.on('console', message => { if (message.type() === 'error') errors.push('console error') })
async function check(name, action) {
  current = name
  await action()
  results.push({name, passed: true})
  console.log(`PASS ${name}`)
}
async function accessible() {
  const audit = await new AxeBuilder({page}).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  assert.ok(audit.violations.length === 0)
}
async function navigation(name) {
  if (!await page.locator('#main-navigation').isVisible()) await page.getByRole('button', {name: 'Menu', exact: true}).click()
  await page.locator('#main-navigation').getByRole('link', {name, exact: true}).click()
}
async function masked() {
  await expect(show).toBeVisible()
  await expect(details).toBeHidden()
  assert.ok(!(await page.locator('body').textContent()).includes(expectedNumber))
}
async function steadyPixels() {
  await page.mouse.move(0, 0)
  await page.evaluate(() => document.activeElement?.blur())
  await page.waitForTimeout(100)
  return canvas.screenshot()
}
async function artworkDifference(first, second) {
  // Compare decoded pixels, not PNG metadata; images remain in process memory.
  return page.evaluate(async ([firstImage, secondImage]) => {
    const pixels = []
    for (const source of [firstImage, secondImage]) {
      const bitmap = await createImageBitmap(await (await fetch('data:image/png;base64,' + source)).blob())
      const canvas = new OffscreenCanvas(200, 126)
      const context = canvas.getContext('2d')
      context.drawImage(bitmap, 0, 0, 200, 126)
      pixels.push(context.getImageData(0, 0, 200, 126).data)
      bitmap.close()
    }
    let difference = 0
    for (let index = 0; index < pixels[0].length; index++) difference += Math.abs(pixels[0][index] - pixels[1][index])
    return difference / pixels[0].length
  }, [first.toString('base64'), second.toString('base64')])
}

try {
  await check('Public home stays masked and has no reveal control', async () => {
    await page.goto(frontend)
    await expect(flip).toHaveAttribute('data-renderer', 'three')
    await expect(show).toHaveCount(0)
  })
  await check('Owned dashboard card starts masked and reflects real ACTIVE status', async () => {
    await navigation('Sign in')
    await page.getByLabel('Email', {exact: true}).fill(fixtures.user.email)
    await page.getByLabel('Password', {exact: true}).fill(fixtures.password)
    await page.getByRole('button', {name: 'Sign in', exact: true}).click()
    await expect(flip).toHaveAttribute('data-renderer', 'three')
    await expect(page.getByText('Active · Ready to use', {exact: true})).toBeVisible()
    await masked()
    await accessible()
  })
  await check('Reveal updates the model and front/back text; hiding clears both faces', async () => {
    const maskedPixels = await steadyPixels()
    await flip.click()
    const maskedBack = await steadyPixels()
    await flip.click()
    await show.click()
    await expect(hide).toBeVisible()
    const numberText = await details.locator('dd').textContent()
    assert.ok(numberText.replaceAll(' ', '') === expectedNumber)
    assert.ok(!maskedPixels.equals(await steadyPixels()), 'The model artwork must update on reveal.')
    await flip.click()
    await expect(details.locator('dt')).toHaveText('Demo security code')
    sampleCode = await details.locator('dd').textContent()
    assert.ok(/^\d{3}$/.test(sampleCode))
    const revealedBack = await steadyPixels()
    const revealedBackDifference = await artworkDifference(maskedBack, revealedBack)
    assert.ok(revealedBackDifference > 0, 'The back artwork must reveal the sample code.')
    await accessible()
    await hide.click()
    await masked()
    assert.ok(!await page.locator('.credit-card-signature').textContent().then(text => text.includes(sampleCode)))
    // Reveal screenshots stay in process memory. Only masked views are exported.
    const hiddenBackDifference = await artworkDifference(maskedBack, await steadyPixels())
    assert.ok(hiddenBackDifference < revealedBackDifference * .25, 'Hiding must restore the original masked back artwork.')
    await flip.click()
    assert.ok(await artworkDifference(maskedPixels, await steadyPixels()) < .5, 'Hiding must also restore the original masked front artwork.')
  })
  await check('Twenty-second timeout hides details (browser clock advanced)', async () => {
    await page.clock.install()
    await show.click()
    await expect(hide).toBeVisible()
    await page.clock.fastForward(20001)
    await masked()
  })
  await check('Window blur and hidden-tab events hide details immediately (events injected)', async () => {
    await show.click()
    await page.evaluate(() => window.dispatchEvent(new Event('blur')))
    await masked()
    await show.click()
    await page.evaluate(() => {
      Object.defineProperty(document, 'hidden', {configurable: true, value: true})
      document.dispatchEvent(new Event('visibilitychange'))
      delete document.hidden
    })
    await masked()
  })
  await check('Use this card prefills number and expiry in memory; a real purchase succeeds', async () => {
    await page.getByRole('link', {name: 'Use this card', exact: true}).click()
    await expect(page.getByRole('heading', {name: 'Purchase', exact: true})).toBeVisible()
    await expect(page.getByLabel('Expiry month', {exact: true})).toHaveValue('12')
    assert.ok(await page.getByLabel('Card number', {exact: true}).inputValue() === expectedNumber)
    await expect(page.getByLabel('Security code', {exact: true})).toHaveValue('')
    assert.ok(!page.url().includes(expectedNumber))
    assert.ok(!JSON.stringify(await page.evaluate(() => history.state)).includes(expectedNumber))
    await page.getByLabel('Security code', {exact: true}).fill(sampleCode)
    await page.getByLabel('Merchant', {exact: true}).fill('Wallet verification')
    await page.getByLabel('Amount (USD)', {exact: true}).fill('7.25')
    await page.getByRole('button', {name: 'Submit purchase', exact: true}).click()
    await expect(page.getByRole('heading', {name: 'Purchase approved', exact: true})).toBeVisible()
    await page.getByRole('button', {name: 'Start another purchase', exact: true}).click()
    await expect(page.getByLabel('Card number', {exact: true})).toHaveValue('')
    await expect(page.getByLabel('Security code', {exact: true})).toHaveValue('')
    await navigation('Dashboard')
    await masked()
    await page.screenshot({path: fileURLToPath(new URL('card-wallet-masked.png', output)), fullPage: true})
  })
  await check('Status badge and muted model reflect a real admin freeze', async () => {
    const login = await fetch(backend + '/api/auth/login', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email: fixtures.admin.email, password: fixtures.password})})
    assert.ok(login.status === 200)
    adminToken = (await login.json()).accessToken
    const freeze = await fetch(`${backend}/api/admin/accounts/${fixtures.user.accountId}/status?status=FROZEN`, {method: 'PATCH', headers: {Authorization: `Bearer ${adminToken}`}})
    assert.ok(freeze.status === 200)
    await navigation('Purchase')
    await expect(flip).toHaveAttribute('data-account-status', 'FROZEN')
    await navigation('Dashboard')
    await expect(page.getByText('Frozen · Purchases paused', {exact: true})).toBeVisible()
    await expect(flip).toHaveAttribute('data-account-status', 'FROZEN')
    await masked()
    await accessible()
  })
  await check('Reveal and hide remain accessible on a narrow screen and without WebGL', async () => {
    await page.setViewportSize({width: 320, height: 844})
    await show.click()
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    await accessible()
    await hide.click()
    await canvas.evaluate(element => element.getContext('webgl2').getExtension('WEBGL_lose_context').loseContext())
    await expect(flip).toHaveAttribute('data-renderer', 'css')
    await show.click()
    assert.ok((await details.locator('dd').textContent()).replaceAll(' ', '') === expectedNumber)
    await flip.click()
    await expect(details.locator('dt')).toHaveText('Demo security code')
    await hide.click()
    await masked()
    await accessible()
  })
  await check('Legacy assigned card still reveals and prefills its original number rule', async () => {
    await fixtures.connection.execute("UPDATE demo_cards SET test_profile='DEMO_4242', last_four='4242' WHERE id=?", [fixtures.user.cardId])
    await navigation('Purchase')
    await navigation('Dashboard')
    await show.click()
    assert.ok((await details.locator('dd').textContent()).replaceAll(' ', '') === '4242'.repeat(4))
    await hide.click()
    await page.getByRole('link', {name: 'Use this card', exact: true}).click()
    await expect(page.getByLabel('Expiry month', {exact: true})).toHaveValue('12')
    assert.ok(await page.getByLabel('Card number', {exact: true}).inputValue() === '4242'.repeat(4))
    assert.ok(errors.length === 0)
  })
} catch (error_) {
  results.push({name: current, passed: false, errorType: error_.name})
  console.error(`FAIL ${current}: ${error_.name}`)
  process.exitCode = 1
} finally {
  sampleCode = ''; adminToken = ''
  await browser.close()
  await fixtures.cleanup()
  await writeFile(new URL('card-wallet-results.json', output), JSON.stringify({date: new Date().toISOString(), realBackend: true, database: 'MySQL', exportedViews: 'Masked views only; revealed pixels, card numbers, codes, and tokens are never exported', injectedCases: ['browser clock advanced for 20-second timeout', 'window blur event', 'hidden-tab visibility event', 'WebGL context loss'], results}, null, 2) + '\n')
}
