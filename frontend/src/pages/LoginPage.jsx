import { Link } from 'react-router'
import PageHeading from '../components/PageHeading.jsx'

export default function LoginPage() {
  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Access"
        title="Sign in"
        description="Your starting point for the simulator."
      />
      <section className="notice-panel" aria-labelledby="login-status">
        <p className="notice-label">Not available yet</p>
        <h2 id="login-status">Sign-in is still being built.</h2>
        <p>Account access will open when secure sign-in is ready. Registration will be available here too.</p>
        <p>Credit Circuit is a classroom simulation. Only fictional test cards and money belong here.</p>
        <Link className="text-link" to="/">Return home <span aria-hidden="true">↗</span></Link>
      </section>
    </main>
  )
}
