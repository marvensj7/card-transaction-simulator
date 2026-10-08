import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getAccounts, getCards } from '../api/creditCircuitApi.js'
import { useUserUi } from '../auth/UserUiContext.jsx'
import PageHeading from '../components/PageHeading.jsx'
import Card from '../components/Card.jsx'
import AccountSummary from '../components/AccountSummary.jsx'
import Loading from '../components/Loading.jsx'
import Button from '../components/Button.jsx'

export default function DashboardPage() {
  const { user } = useUserUi()
  const [account, setAccount] = useState(/** @type {import('../api/types.js').Account | null} */ (null))
  const [card, setCard] = useState(/** @type {import('../api/types.js').DemoCard | null} */ (null))
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    async function load() {
      try {
        const rows = await getAccounts()
        const own = rows[0] || null
        const cards = own ? await getCards(own.id) : []
        if (active) { setAccount(own); setCard(cards[0] || null) }
      } catch (failure) { if (active) setError(failure instanceof Error ? failure.message : 'Account could not be loaded.') }
      finally { if (active) setLoading(false) }
    }
    load()
    return () => { active = false }
  }, [reload])
  let content
  if (loading) content = <Loading skeleton />
  else if (error) content = <div role="alert"><p className="error">{error}</p><Button onClick={() => setReload(reload + 1)}>Try again</Button></div>
  else if (account) content = <>
      <Card title="Your credit account"><AccountSummary account={account} />
        {account.status === 'FROZEN' && <p>New purchases will be declined while this account is frozen. Eligible refunds remain available.</p>}
        <Link className="text-link" to="/purchase">Make a fictional purchase</Link>
      </Card>
      <Card title="Your fictional card">{card ? <><p>{card.label} · {card.maskedNumber}</p><p>Expiry {card.expiryMonth}/{card.expiryYear}</p><p className="hint">Use only the documented test profile in the purchase form.</p></> : <p>No fictional card is assigned.</p>}</Card>
    </>
  else content = <Card title="No account"><p>No credit account is available. Please contact the demo administrator.</p></Card>

  return <main id="main-content" className="content-page" tabIndex={-1}>
    <PageHeading eyebrow="Credit Circuit / Account" title="Dashboard" description={`Welcome, ${user?.displayName || 'customer'}. Your fictional credit at a glance.`} />
    {content}
  </main>
}
