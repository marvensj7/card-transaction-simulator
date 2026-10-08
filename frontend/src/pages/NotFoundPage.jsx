import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <main className="content-page">
      <p className="eyebrow">404 / Page not found</p>
      <h1>This signal<br /><span>lost its way.</span></h1>
      <p>There is no Credit Circuit page at this address.</p>
      <Link to="/">Return home</Link>
    </main>
  )
}
