import { Link, Route, Routes } from 'react-router'
import CircuitMark from './components/CircuitMark.jsx'
import SiteNavigation from './components/SiteNavigation.jsx'
import RouteFocus from './components/RouteFocus.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import PurchasePage from './pages/PurchasePage.jsx'
import TransactionsPage from './pages/TransactionsPage.jsx'
import AdminPage from './pages/AdminPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
    <div className="app-shell">
      <RouteFocus />
      <a className="skip-link" href="#main-content">Skip to content</a>
      <header className="site-header">
        <Link className="brand" to="/"><CircuitMark />Credit Circuit</Link>
        <span className="project-label">UCI 2123 · Capstone project</span>
        <SiteNavigation />
      </header>

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/purchase" element={<PurchasePage />} />
        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>

      <footer className="site-footer">
        <span>Credit Circuit</span>
        <span>Credit Card Transaction Simulator</span>
      </footer>
    </div>
  )
}
