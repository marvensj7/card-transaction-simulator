import AccessNotice from '../components/AccessNotice.jsx'
import PageHeading from '../components/PageHeading.jsx'

export default function TransactionsPage() {
  return (
    <main className="content-page">
      <PageHeading
        eyebrow="Credit Circuit / History"
        title="Transactions"
        description="Follow a request through to its outcome."
      />
      <AccessNotice>
        Your purchases, declines, and full refunds will appear here after sign-in. Transaction history and refund actions are unavailable for now.
      </AccessNotice>
    </main>
  )
}
