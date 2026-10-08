import { Link } from 'react-router'
import PageHeading from '../components/PageHeading.jsx'

export default function NotFoundPage() {
  return (
    <main className="content-page">
      <PageHeading
        eyebrow="Credit Circuit / 404"
        title="Page not found"
        description="This signal lost its way. There is no page at this address."
      />
      <Link className="text-link" to="/">Return home <span aria-hidden="true">↗</span></Link>
    </main>
  )
}
