import AccessNotice from '../components/AccessNotice.jsx'
import PageHeading from '../components/PageHeading.jsx'

export default function PurchasePage() {
  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Request"
        title="Purchase"
        description="Every purchase starts a signal."
      />
      <AccessNotice>
        This page will accept fictional test-card purchases and show the decision and reason. Purchases are unavailable for now.
      </AccessNotice>
    </main>
  )
}
