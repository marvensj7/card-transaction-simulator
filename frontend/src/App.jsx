import { Link, Route, Routes } from 'react-router'
import { UserUiProvider } from './auth/UserUiContext.jsx'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
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
    <UserUiProvider>
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
          <Route path="/dashboard" element={<ProtectedRoute role="USER"><DashboardPage /></ProtectedRoute>} />
          <Route path="/purchase" element={<ProtectedRoute role="USER"><PurchasePage /></ProtectedRoute>} />
          <Route path="/transactions" element={<ProtectedRoute role="USER"><TransactionsPage /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute role="ADMIN"><AdminPage /></ProtectedRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>

        <footer className="site-footer">
          <span>Credit Circuit</span>
          <span>Credit Card Transaction Simulator</span>
        </footer>
      </div>
    </UserUiProvider>
  )
}
