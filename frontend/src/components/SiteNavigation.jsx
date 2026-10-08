import { NavLink } from 'react-router'

export default function SiteNavigation() {
  return (
    <nav className="site-navigation" aria-label="Main navigation">
      <NavLink to="/" end>Home</NavLink>
      <NavLink to="/login" end>Sign in</NavLink>
      <NavLink to="/dashboard" end>Dashboard</NavLink>
      <NavLink to="/purchase" end>Purchase</NavLink>
      <NavLink to="/transactions" end>Transactions</NavLink>
      <NavLink to="/admin" end>Admin</NavLink>
    </nav>
  )
}
