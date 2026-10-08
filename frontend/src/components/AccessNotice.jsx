import { Link } from 'react-router'
import { useUserUi } from '../auth/UserUiContext.jsx'

/**
 * @param {{ children: import('react').ReactNode }} props
 */
export default function AccessNotice({ children }) {
  const { user } = useUserUi()

  return (
    <section className="notice-panel" aria-labelledby="access-status">
      <p className="notice-label">Access unavailable</p>
      <h2 id="access-status">{user ? 'Account tools are still being built.' : 'Secure sign-in comes first.'}</h2>
      <p>{children}</p>
      {user ? (
        <p>This page remains unavailable while its account tools are being built.</p>
      ) : (
        <>
          <p>Sign-in is still being built. This page will open after secure account access is ready.</p>
          <Link className="text-link" to="/login">About sign-in <span aria-hidden="true">↗</span></Link>
        </>
      )}
    </section>
  )
}
