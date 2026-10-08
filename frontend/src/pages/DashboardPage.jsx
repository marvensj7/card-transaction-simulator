import AccessNotice from '../components/AccessNotice.jsx'
import PageHeading from '../components/PageHeading.jsx'

export default function DashboardPage() {
  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Account"
        title="Dashboard"
        description="A place to see your credit at a glance."
      />
      <AccessNotice>
        Your account summary and masked fictional card will appear here after sign-in. Account details are unavailable for now.
      </AccessNotice>
    </main>
  )
}
