import { useEffect, useRef, useState } from 'react'
import { getAccounts, getCards, submitPurchase } from '../api/creditCircuitApi.js'
import { ApiError } from '../api/fetchJson.js'
import { money, reasonLabel } from '../api/format.js'
import PageHeading from '../components/PageHeading.jsx'
import Card from '../components/Card.jsx'
import AccountSummary from '../components/AccountSummary.jsx'
import Input from '../components/Input.jsx'
import Button from '../components/Button.jsx'
import Loading from '../components/Loading.jsx'

const emptyForm = { testCardNumber: '', expiryMonth: '', expiryYear: '', testSecurityCode: '', merchantName: '', amount: '' }
export default function PurchasePage() {
  const [account, setAccount] = useState(/** @type {import('../api/types.js').Account | null} */ (null))
  const [card, setCard] = useState(/** @type {import('../api/types.js').DemoCard | null} */ (null))
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [reload, setReload] = useState(0)
  const [phase, setPhase] = useState('editing')
  const [error, setError] = useState('')
  const [fields, setFields] = useState(/** @type {Record<string, string>} */ ({}))
  const [result, setResult] = useState(/** @type {import('../api/types.js').TransactionResult | null} */ (null))
  const inFlight = useRef(false)
  const submission = useRef(/** @type {import('../api/creditCircuitApi.js').PurchaseRequest | null} */ (null))

  useEffect(() => {
    let active = true
    setLoading(true); setLoadError('')
    async function load() {
      try {
        const rows = await getAccounts()
        const own = rows[0] || null
        const assigned = own ? (await getCards(own.id))[0] || null : null
        if (active) {
          setAccount(own); setCard(assigned)
          if (assigned) setForm(previous => ({ ...previous, expiryMonth: String(assigned.expiryMonth), expiryYear: String(assigned.expiryYear) }))
        }
      } catch (failure) { if (active) setLoadError(failure instanceof Error ? failure.message : 'Purchase details could not be loaded.') }
      finally { if (active) setLoading(false) }
    }
    load()
    return () => { active = false }
  }, [reload])

  /** @param {import('react').ChangeEvent<HTMLInputElement>} event */
  function change(event) {
    const { name, value } = event.target
    setForm(previous => ({ ...previous, [name]: value }))
    setFields(previous => ({ ...previous, [name]: '' }))
  }

  /** @param {import('react').SubmitEvent<HTMLFormElement>} event */
  async function submit(event) {
    event.preventDefault()
    if (!card || !account || inFlight.current || phase !== 'editing') return
    /** @type {Record<string, string>} */
    const invalid = {}
    if (!/^\d{16}$/.test(form.testCardNumber)) invalid.testCardNumber = 'Enter the 16-digit fictional test number.'
    if (!/^\d{3,4}$/.test(form.testSecurityCode)) invalid.testSecurityCode = 'Enter 3 or 4 fictional digits.'
    if (!/^\d{1,2}$/.test(form.expiryMonth) || Number(form.expiryMonth) < 1 || Number(form.expiryMonth) > 12) invalid.expiryMonth = 'Enter a month from 1 to 12.'
    if (!/^\d{4}$/.test(form.expiryYear) || Number(form.expiryYear) < 2000) invalid.expiryYear = 'Enter a four-digit year from 2000.'
    if (!form.merchantName.trim()) invalid.merchantName = 'Enter a fictional merchant.'
    if (!/^\d{1,12}(\.\d{1,2})?$/.test(form.amount) || Number(form.amount) <= 0) invalid.amount = 'Enter a positive amount with at most 12 whole digits and 2 decimal places.'
    setFields(invalid); setError('')
    if (Object.keys(invalid).length) { setError('Check the highlighted fields.'); return }
    submission.current = { cardId: card.id, ...form, expiryMonth: Number(form.expiryMonth), expiryYear: Number(form.expiryYear), merchantName: form.merchantName.trim(), requestId: crypto.randomUUID() }
    await send()
  }

  async function send() {
    if (!account || !submission.current || inFlight.current) return
    inFlight.current = true; setPhase('pending'); setError('')
    try {
      const outcome = await submitPurchase(account.id, submission.current)
      setResult(outcome); setAccount(outcome.account); setPhase('complete')
      submission.current = null
      setForm(previous => ({ ...previous, testCardNumber: '', testSecurityCode: '' }))
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'The purchase could not be confirmed.')
      if (!(failure instanceof ApiError) || failure.status === 0 || failure.status >= 500) {
        setPhase('uncertain')
      } else {
        submission.current = null; setPhase('editing'); setFields(failure.fields)
        setForm(previous => ({ ...previous, testSecurityCode: '' }))
      }
    } finally { inFlight.current = false }
  }

  let content
  if (loading) content = <Loading skeleton />
  else if (loadError) content = <div role="alert"><p className="error">{loadError}</p><Button onClick={() => setReload(reload + 1)}>Try again</Button></div>
  else if (!account || !card) content = <p>No assigned account and card are available.</p>
  else content = <>
      <Card title="Available credit"><AccountSummary account={account} /></Card>
      <Card title="Fictional purchase">
        <p>Assigned card: {card.maskedNumber}. Expiry {card.expiryMonth}/{card.expiryYear}. Test profile: DEMO_4242.</p>
        <p className="hint">For this classroom profile, use 4242 repeated four times. The security code checks format only. Never enter real card details.</p>
        {error && <p className="error" role="alert">{error}</p>}
        {phase === 'uncertain' && <section aria-live="polite" aria-label="Uncertain purchase"><p>The outcome is uncertain. Keep this page open and retry the same purchase. The request ID and details stay unchanged.</p><Button onClick={send}>Retry same purchase</Button></section>}
        {result && <section aria-live="polite" aria-label="Purchase outcome"><h3>Purchase {result.transaction.status === 'APPROVED' ? 'approved' : 'declined'}</h3><p>{money(result.transaction.amount)} · Transaction #{result.transaction.id}</p><p>{reasonLabel(result.transaction.reasonCode)}</p></section>}
        {phase === 'complete' ? <Button onClick={() => { setResult(null); setPhase('editing'); setForm({ ...emptyForm, expiryMonth: String(card.expiryMonth), expiryYear: String(card.expiryYear) }) }}>Start another purchase</Button> : <form onSubmit={submit} noValidate>
          <fieldset disabled={phase !== 'editing'}>
            <Input label="Fictional card number" name="testCardNumber" inputMode="numeric" autoComplete="off" maxLength={16} required value={form.testCardNumber} onChange={change} error={fields.testCardNumber} />
            <div className="form-grid"><Input label="Expiry month" name="expiryMonth" inputMode="numeric" maxLength={2} required value={form.expiryMonth} onChange={change} error={fields.expiryMonth} /><Input label="Expiry year" name="expiryYear" inputMode="numeric" maxLength={4} required value={form.expiryYear} onChange={change} error={fields.expiryYear} /></div>
            <Input label="Fictional security code" name="testSecurityCode" type="password" inputMode="numeric" autoComplete="off" maxLength={4} required value={form.testSecurityCode} onChange={change} error={fields.testSecurityCode} />
            <Input label="Fictional merchant" name="merchantName" maxLength={100} required value={form.merchantName} onChange={change} error={fields.merchantName} />
            <Input label="Amount (USD)" name="amount" inputMode="decimal" maxLength={15} required value={form.amount} onChange={change} error={fields.amount} />
            <Button type="submit" pending={phase === 'pending'}>Submit purchase</Button>
          </fieldset>
        </form>}
      </Card>
    </>

  return <main id="main-content" className="content-page" tabIndex={-1}>
    <PageHeading eyebrow="Credit Circuit / Request" title="Purchase" description="Every purchase starts a signal. Use fictional test details only." />
    {content}
  </main>
}
