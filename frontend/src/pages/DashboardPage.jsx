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
  const [customerAccount, setCustomerAccount] = useState(/** @type {import('../api/types.js').Account | null} */ (null))
  const [assignedCard, setAssignedCard] = useState(/** @type {import('../api/types.js').DemoCard | null} */ (null))
  const [accountDetailsLoading, setAccountDetailsLoading] = useState(true)
  const [accountLoadError, setAccountLoadError] = useState('')
  const [accountRefreshCounter, setAccountRefreshCounter] = useState(0)

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
        }
      } catch (failure) {
        if (pageIsActive) {
          setAccountLoadError(failure instanceof Error ? failure.message : 'Account could not be loaded.')
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
  }, [accountRefreshCounter])

  function handleAccountRefresh() {
    setAccountRefreshCounter(previousCounter => previousCounter + 1)
  }

  let dashboardContent
  if (accountDetailsLoading) {
    dashboardContent = <Loading skeleton />
  } else if (accountLoadError) {
    dashboardContent = (
      <div role="alert">
        <p className="error">{accountLoadError}</p>
        <Button onClick={handleAccountRefresh}>Try again</Button>
      </div>
    )
  } else if (customerAccount) {
    dashboardContent = (
      <>
        <Card title="Your credit account">
          <AccountSummary account={customerAccount} />
          {customerAccount.status === 'FROZEN' && (
            <p>New purchases will be declined while this account is frozen. Eligible refunds remain available.</p>
          )}
          <Link className="text-link" to="/purchase">Make a fictional purchase</Link>
        </Card>
        <Card title="Your fictional card">
          {assignedCard ? (
            <>
              <p>{assignedCard.label} · {assignedCard.maskedNumber}</p>
              <p>Expiry {assignedCard.expiryMonth}/{assignedCard.expiryYear}</p>
              <p className="hint">Follow your assigned card's entry instruction in the purchase form.</p>
            </>
          ) : (
            <p>No fictional card is assigned.</p>
          )}
        </Card>
      </>
    )
  } else {
    dashboardContent = (
      <Card title="No account">
        <p>No credit account is available. Please contact the demo administrator.</p>
      </Card>
    )
  }

  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Account"
        title="Dashboard"
        description={`Welcome, ${user?.displayName || 'customer'}. Your fictional credit at a glance.`}
      />
      {dashboardContent}
    </main>
  )
}
