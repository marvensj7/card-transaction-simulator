import AccessNotice from '../components/AccessNotice.jsx'
import PageHeading from '../components/PageHeading.jsx'

export default function AdminPage() {
  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Oversight"
        title="Administration"
        description="A place to review account activity."
      />
      <AccessNotice>
        Account and activity lists will require an administrator account. Lists and freeze or reactivate actions are unavailable for now.
      </AccessNotice>
    </main>
  )
}
