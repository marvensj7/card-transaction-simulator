import { useEffect, useRef, useState } from 'react'
import { getAccounts, getTransactions, refundPurchase } from '../api/creditCircuitApi.js'
import { ApiError } from '../api/fetchJson.js'
import { money } from '../api/format.js'
import PageHeading from '../components/PageHeading.jsx'
import Card from '../components/Card.jsx'
import Loading from '../components/Loading.jsx'
import Button from '../components/Button.jsx'
import TransactionTable from '../components/TransactionTable.jsx'
import Pagination from '../components/Pagination.jsx'
import ConfirmModal from '../components/ConfirmModal.jsx'

export default function TransactionsPage() {
  const [history, setHistory] = useState(/** @type {import('../api/types.js').Page<import('../api/types.js').Transaction> | null} */ (null))
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [selected, setSelected] = useState(/** @type {import('../api/types.js').Transaction | null} */ (null))
  const [pending, setPending] = useState(false)
  const [uncertain, setUncertain] = useState(false)
  const [refundError, setRefundError] = useState('')
  const submission = useRef(/** @type {{purchaseId: number, requestId: string} | null} */ (null))
  const inFlight = useRef(false)
  const refundButton = useRef(/** @type {HTMLElement | null} */ (null))

  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    async function load() {
      try {
        const account = (await getAccounts())[0]
        const result = account ? await getTransactions(account.id, page) : {items: [], page: 0, size: 10, totalPages: 0, totalElements: 0}
        if (active) setHistory(result)
      } catch (error_) { if (active) setError(error_ instanceof Error ? error_.message : 'History could not be loaded.') }
      finally { if (active) setLoading(false) }
    }
    load()
    return () => { active = false }
  }, [page, reload])

  /** @param {import('../api/types.js').Transaction} transaction */
  function selectRefund(transaction) {
    refundButton.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    submission.current = { purchaseId: transaction.id, requestId: crypto.randomUUID() }
    setSelected(transaction); setRefundError('')
  }

  function closeModal() {
    setSelected(null); submission.current = null; setRefundError('')
    refundButton.current?.focus()
  }

  async function refund() {
    if (!submission.current || inFlight.current) return
    inFlight.current = true; setPending(true); setRefundError(''); setMessage('')
    try {
      const result = await refundPurchase(submission.current.purchaseId, submission.current.requestId)
      setMessage(`Full refund saved: ${money(result.transaction.amount)}. Available credit: ${money(result.account.availableCredit)}.`)
      setUncertain(false); closeModal(); setReload(previous => previous + 1)
    } catch (error_) {
      setRefundError(error_ instanceof Error ? error_.message : 'The refund could not be confirmed.')
      if (!(error_ instanceof ApiError) || error_.status === 0 || error_.status >= 500) {
        setUncertain(true); setSelected(null); refundButton.current?.focus()
      } else if (uncertain) {
        setUncertain(false); submission.current = null; setError(error_.message)
      }
    } finally { inFlight.current = false; setPending(false) }
  }

  let content = null
  if (loading) content = <Loading skeleton />
  else if (error) content = <div role="alert"><p className="error">{error}</p><Button onClick={() => setReload(reload + 1)}>Try again</Button></div>
  else if (history) content = <Card title="Your history">
      {history.items.length ? <TransactionTable items={history.items} pending={pending || uncertain} onRefund={selectRefund} /> : <p>No transactions yet. Your next fictional purchase will appear here.</p>}
      <Pagination page={page} totalPages={history.totalPages} pending={loading || pending || uncertain} onPage={setPage} />
    </Card>

  return <main id="main-content" className="content-page" tabIndex={-1}>
    <PageHeading eyebrow="Credit Circuit / History" title="Transactions" description="Follow a request through to its outcome. Refunds reverse the full original amount once." />
    {message && <output className="notice">{message}</output>}
    {uncertain && <Card title="Refund outcome uncertain"><p>Keep this page open. Retry the same refund to confirm the saved result without creating another reversal.</p>{refundError && <p role="alert" className="error">{refundError}</p>}<Button onClick={refund} pending={pending}>Retry same refund</Button></Card>}
    {content}
    <ConfirmModal open={selected !== null} title="Confirm full refund" pending={pending} onConfirm={refund} onClose={closeModal}>
      <p>Refund {selected ? money(selected.amount) : ''} for purchase #{selected?.id}? The original purchase stays in history.</p>
      {refundError && <p role="alert" className="error">{refundError}</p>}
    </ConfirmModal>
  </main>
}
