import { Link } from 'react-router'

/**
 * @param {{ children: import('react').ReactNode }} props
 */
export default function AccessNotice({ children }) {
  return (
    <section className="notice-panel" aria-labelledby="access-status">
      <p className="notice-label">Access unavailable</p>
      <h2 id="access-status">Secure sign-in comes first.</h2>
      <p>{children}</p>
      <p>Sign-in is still being built. This page will open after secure account access is ready.</p>
      <Link className="text-link" to="/login">About sign-in <span aria-hidden="true">↗</span></Link>
    </section>
  )
}
