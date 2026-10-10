import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { getAccounts, getCards, submitPurchase } from '../api/creditCircuitApi.js'
import { ApiError } from '../api/fetchJson.js'
import { money, reasonLabel } from '../api/format.js'
import PageHeading from '../components/PageHeading.jsx'
import Card from '../components/Card.jsx'
import AccountSummary from '../components/AccountSummary.jsx'
import Input from '../components/Input.jsx'
import Button from '../components/Button.jsx'
import Loading from '../components/Loading.jsx'
import FlippableCard from '../components/FlippableCard.jsx'
import { fictionalCardNumber } from '../api/fictionalCardNumber.js'

const emptyPurchaseForm = {
  testCardNumber: '',
  expiryMonth: '',
  expiryYear: '',
  testSecurityCode: '',
  merchantName: '',
  amount: '',
}

export default function PurchasePage() {
  const [searchParams] = useSearchParams()
  const useCardRequested = searchParams.get('useCard') === '1'
  const cardPrefillUsed = useRef(false)
  const [customerAccount, setCustomerAccount] = useState(/** @type {import('../api/types.js').Account | null} */ (null))
  const [assignedCard, setAssignedCard] = useState(/** @type {import('../api/types.js').DemoCard | null} */ (null))
  const [purchaseForm, setPurchaseForm] = useState(emptyPurchaseForm)
  const [accountDetailsLoading, setAccountDetailsLoading] = useState(true)
  const [accountLoadError, setAccountLoadError] = useState('')
  const [accountRefreshCounter, setAccountRefreshCounter] = useState(0)
  const [purchaseSubmissionStatus, setPurchaseSubmissionStatus] = useState('editing')
  const [purchaseError, setPurchaseError] = useState('')
  const [fieldErrors, setFieldErrors] = useState(/** @type {Record<string, string>} */ ({}))
  const [purchaseResult, setPurchaseResult] = useState(/** @type {import('../api/types.js').TransactionResult | null} */ (null))
  const purchaseRequestInProgress = useRef(false)
  const pendingPurchaseRequest = useRef(/** @type {import('../api/creditCircuitApi.js').PurchaseRequest | null} */ (null))

  useEffect(() => {
    let pageIsActive = true
    setAccountDetailsLoading(true)
    setAccountLoadError('')

    async function loadCustomerAccountAndCard() {
      try {
        const customerAccounts = await getAccounts()
        const loadedAccount = customerAccounts[0] || null
        let loadedCard = null
        if (loadedAccount) {
          const assignedCards = await getCards(loadedAccount.id)
          loadedCard = assignedCards[0] || null
        }

        if (pageIsActive) {
          setCustomerAccount(loadedAccount)
          setAssignedCard(loadedCard)
          if (loadedCard && loadedAccount) {
            let prefilledNumber = ''
            if (useCardRequested && !cardPrefillUsed.current) {
              prefilledNumber = fictionalCardNumber(loadedCard, loadedAccount.id)
              cardPrefillUsed.current = true
            }
            setPurchaseForm(previousForm => ({
              ...previousForm,
              testCardNumber: prefilledNumber || previousForm.testCardNumber,
              expiryMonth: String(loadedCard.expiryMonth),
              expiryYear: String(loadedCard.expiryYear),
            }))
          }
        }
      } catch (error_) {
        if (pageIsActive) {
          setAccountLoadError(error_ instanceof Error ? error_.message : 'Purchase details could not be loaded.')
        }
      } finally {
        if (pageIsActive) {
          setAccountDetailsLoading(false)
        }
      }
    }

    loadCustomerAccountAndCard()
    return () => {
      pageIsActive = false
    }
  }, [accountRefreshCounter, useCardRequested])

  function handleAccountRefresh() {
    setAccountRefreshCounter(previousCounter => previousCounter + 1)
  }

  /** @param {import('react').ChangeEvent<HTMLInputElement>} event */
  function handlePurchaseFieldChange(event) {
    const { name, value } = event.target
    setPurchaseForm(previousForm => ({ ...previousForm, [name]: value }))
    setFieldErrors(previousErrors => ({ ...previousErrors, [name]: '' }))
  }

  /** @param {import('react').SubmitEvent<HTMLFormElement>} event */
  async function handlePurchaseSubmit(event) {
    event.preventDefault()
    if (!assignedCard || !customerAccount || purchaseRequestInProgress.current || purchaseSubmissionStatus !== 'editing') {
      return
    }

    // These checks give format feedback. The server decides ownership and approval.
    /** @type {Record<string, string>} */
    const validationErrors = {}
    if (!/^\d{16}$/.test(purchaseForm.testCardNumber)) {
      validationErrors.testCardNumber = 'Enter the assigned 16-digit card number.'
    }
    if (!/^\d{3,4}$/.test(purchaseForm.testSecurityCode)) {
      validationErrors.testSecurityCode = 'Enter 3 or 4 digits.'
    }
    if (!/^\d{1,2}$/.test(purchaseForm.expiryMonth)
        || Number(purchaseForm.expiryMonth) < 1 || Number(purchaseForm.expiryMonth) > 12) {
      validationErrors.expiryMonth = 'Enter a month from 1 to 12.'
    }
    if (!/^\d{4}$/.test(purchaseForm.expiryYear) || Number(purchaseForm.expiryYear) < 2000) {
      validationErrors.expiryYear = 'Enter a four-digit year from 2000.'
    }
    if (!purchaseForm.merchantName.trim()) {
      validationErrors.merchantName = 'Enter a merchant name.'
    }
    if (!/^\d{1,12}(\.\d{1,2})?$/.test(purchaseForm.amount) || Number(purchaseForm.amount) <= 0) {
      validationErrors.amount = 'Enter a positive amount with at most 12 whole digits and 2 decimal places.'
    }

    setFieldErrors(validationErrors)
    setPurchaseError('')
    if (Object.keys(validationErrors).length > 0) {
      setPurchaseError('Check the highlighted fields.')
      return
    }

    pendingPurchaseRequest.current = {
      cardId: assignedCard.id,
      ...purchaseForm,
      expiryMonth: Number(purchaseForm.expiryMonth),
      expiryYear: Number(purchaseForm.expiryYear),
      merchantName: purchaseForm.merchantName.trim(),
      requestId: crypto.randomUUID(),
    }
    await sendPurchaseRequest()
  }

  async function sendPurchaseRequest() {
    if (!customerAccount || !pendingPurchaseRequest.current || purchaseRequestInProgress.current) {
      return
    }
    purchaseRequestInProgress.current = true
    setPurchaseSubmissionStatus('pending')
    setPurchaseError('')

    try {
      const purchaseOutcome = await submitPurchase(customerAccount.id, pendingPurchaseRequest.current)
      setPurchaseResult(purchaseOutcome)
      setCustomerAccount(purchaseOutcome.account)
      setPurchaseSubmissionStatus('complete')
      pendingPurchaseRequest.current = null
      setPurchaseForm(previousForm => ({ ...previousForm, testCardNumber: '', testSecurityCode: '' }))
    } catch (error_) {
      setPurchaseError(error_ instanceof Error ? error_.message : 'The purchase could not be confirmed.')
      if (!(error_ instanceof ApiError) || error_.status === 0 || error_.status >= 500) {
        // A lost response may follow a saved purchase. Retry the exact body and UUID.
        setPurchaseSubmissionStatus('uncertain')
      } else {
        pendingPurchaseRequest.current = null
        setPurchaseSubmissionStatus('editing')
        setFieldErrors(error_.fields)
        setPurchaseForm(previousForm => ({ ...previousForm, testSecurityCode: '' }))
      }
    } finally {
      purchaseRequestInProgress.current = false
    }
  }

  function handleStartAnotherPurchase() {
    if (!assignedCard) {
      return
    }
    setPurchaseResult(null)
    setPurchaseError('')
    setFieldErrors({})
    setPurchaseSubmissionStatus('editing')
    setPurchaseForm({
      ...emptyPurchaseForm,
      expiryMonth: String(assignedCard.expiryMonth),
      expiryYear: String(assignedCard.expiryYear),
    })
  }

  let purchaseContent
  if (accountDetailsLoading) {
    purchaseContent = <Loading skeleton />
  } else if (accountLoadError) {
    purchaseContent = (
      <div role="alert">
        <p className="error">{accountLoadError}</p>
        <Button onClick={handleAccountRefresh}>Try again</Button>
      </div>
    )
  } else if (!customerAccount || !assignedCard) {
    purchaseContent = <p>No assigned account and card are available.</p>
  } else {
    purchaseContent = (
      <>
        <Card title="Available credit">
          <AccountSummary account={customerAccount} />
        </Card>
        <Card title="Purchase details">
          <FlippableCard key={assignedCard.id} card={assignedCard} account={customerAccount} />
          <p>
            Assigned card: {assignedCard.maskedNumber}.
            {' '}Expiry {assignedCard.expiryMonth}/{assignedCard.expiryYear}.
          </p>
          <p className="hint">{assignedCard.numberEntryHint}</p>
          <p className="hint">The security code checks format only.</p>

          {purchaseError && (
            <p className="error" role="alert">{purchaseError}</p>
          )}
          {purchaseSubmissionStatus === 'uncertain' && (
            <section aria-live="polite" aria-label="Uncertain purchase">
              <p>
                The outcome is uncertain. Keep this page open and retry the same purchase.
                The request ID and details stay unchanged.
              </p>
              <Button onClick={sendPurchaseRequest}>Retry same purchase</Button>
            </section>
          )}
          {purchaseResult && (
            <section aria-live="polite" aria-label="Purchase outcome">
              <h3>Purchase {purchaseResult.transaction.status === 'APPROVED' ? 'approved' : 'declined'}</h3>
              <p>{money(purchaseResult.transaction.amount)} · Transaction #{purchaseResult.transaction.id}</p>
              <p>{reasonLabel(purchaseResult.transaction.reasonCode)}</p>
            </section>
          )}

          {purchaseSubmissionStatus === 'complete' ? (
            <Button onClick={handleStartAnotherPurchase}>Start another purchase</Button>
          ) : (
            <form onSubmit={handlePurchaseSubmit} noValidate>
              <fieldset disabled={purchaseSubmissionStatus !== 'editing'}>
                <Input
                  label="Card number"
                  name="testCardNumber"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={16}
                  required
                  value={purchaseForm.testCardNumber}
                  onChange={handlePurchaseFieldChange}
                  error={fieldErrors.testCardNumber}
                />
                <div className="form-grid">
                  <Input
                    label="Expiry month"
                    name="expiryMonth"
                    inputMode="numeric"
                    maxLength={2}
                    required
                    value={purchaseForm.expiryMonth}
                    onChange={handlePurchaseFieldChange}
                    error={fieldErrors.expiryMonth}
                  />
                  <Input
                    label="Expiry year"
                    name="expiryYear"
                    inputMode="numeric"
                    maxLength={4}
                    required
                    value={purchaseForm.expiryYear}
                    onChange={handlePurchaseFieldChange}
                    error={fieldErrors.expiryYear}
                  />
                </div>
                <Input
                  label="Security code"
                  name="testSecurityCode"
                  type="password"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={4}
                  required
                  value={purchaseForm.testSecurityCode}
                  onChange={handlePurchaseFieldChange}
                  error={fieldErrors.testSecurityCode}
                />
                <Input
                  label="Merchant"
                  name="merchantName"
                  maxLength={100}
                  required
                  value={purchaseForm.merchantName}
                  onChange={handlePurchaseFieldChange}
                  error={fieldErrors.merchantName}
                />
                <Input
                  label="Amount (USD)"
                  name="amount"
                  inputMode="decimal"
                  maxLength={15}
                  required
                  value={purchaseForm.amount}
                  onChange={handlePurchaseFieldChange}
                  error={fieldErrors.amount}
                />
                <Button type="submit" pending={purchaseSubmissionStatus === 'pending'}>
                  Submit purchase
                </Button>
              </fieldset>
            </form>
          )}
        </Card>
      </>
    )
  }

  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Request"
        title="Purchase"
        description="Every purchase starts a signal."
      />
      {purchaseContent}
    </main>
  )
}
