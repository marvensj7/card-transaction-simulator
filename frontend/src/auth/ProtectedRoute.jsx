import { Navigate } from 'react-router'
import { useUserUi } from './UserUiContext.jsx'

/** @param {{role: 'USER' | 'ADMIN', children: import('react').ReactNode}} props */
export default function ProtectedRoute({ role, children }) {
  const { user } = useUserUi()
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <main id="main-content" className="content-page" tabIndex={-1}><h1>Access restricted</h1><p>This page needs the {role === 'ADMIN' ? 'administrator' : 'customer'} role.</p></main>
  return <>{children}</>
}
