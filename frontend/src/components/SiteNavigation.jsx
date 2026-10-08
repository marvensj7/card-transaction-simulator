import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router'
import { useUserUi } from '../auth/UserUiContext.jsx'

export default function SiteNavigation() {
  const { user, signOut } = useUserUi()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const menuButton = useRef(/** @type {HTMLButtonElement | null} */ (null))
  const navigation = useRef(/** @type {HTMLElement | null} */ (null))
  const location = useLocation()

  // Includes back/forward and navigation outside the menu.
  useEffect(() => {
    setIsMenuOpen(false)
  }, [location])

  // Resizing can hide the focused link or button even when the menu is closed.
  useEffect(() => {
    const phoneLayout = window.matchMedia('(max-width: 50rem)')

    function handleResize() {
      if (phoneLayout.matches) {
        if (navigation.current?.contains(document.activeElement)) {
          menuButton.current?.focus()
        }
      } else {
        setIsMenuOpen(false)
        if (document.activeElement === menuButton.current) {
          navigation.current?.querySelector('a')?.focus()
        }
      }
    }

    phoneLayout.addEventListener('change', handleResize)
    return () => phoneLayout.removeEventListener('change', handleResize)
  }, [])

  useEffect(() => {
    if (!isMenuOpen) {
      return
    }

    /** @param {KeyboardEvent} event */
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setIsMenuOpen(false)
        menuButton.current?.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isMenuOpen])

  /** @param {import('react').FocusEvent<HTMLElement>} event */
  function handleBlur(event) {
    // CSS can blur a hidden control before the viewport listener runs.
    if (event.relatedTarget !== null) {
      return
    }

    const phoneLayout = window.matchMedia('(max-width: 50rem)')
    if (phoneLayout.matches) {
      if (!isMenuOpen && navigation.current?.contains(event.target)) {
        menuButton.current?.focus()
      }
    } else if (event.target === menuButton.current) {
      navigation.current?.querySelector('a')?.focus()
    }
  }

  function closeMenu() {
    setIsMenuOpen(false)
    // A link to the current page also closes the menu without losing focus.
    if (isMenuOpen) {
      menuButton.current?.focus()
    }
  }

  return (
    <div className="navigation-area" onBlur={handleBlur}>
      <button
        className="menu-toggle"
        type="button"
        ref={menuButton}
        aria-expanded={isMenuOpen}
        aria-controls="main-navigation"
        onClick={() => setIsMenuOpen(!isMenuOpen)}
      >
        {isMenuOpen ? 'Close menu' : 'Menu'}
      </button>
      <span className="navigation-status">{user ? 'Signed in' : 'Not signed in'}</span>
      <nav ref={navigation} id="main-navigation" className="site-navigation" aria-label="Main navigation" data-open={isMenuOpen}>
        <NavLink to="/" end onClick={closeMenu}>Home</NavLink>
        {!user && <NavLink to="/login" end onClick={closeMenu}>Sign in</NavLink>}
        <NavLink to="/dashboard" end onClick={closeMenu}>Dashboard</NavLink>
        <NavLink to="/purchase" end onClick={closeMenu}>Purchase</NavLink>
        <NavLink to="/transactions" end onClick={closeMenu}>Transactions</NavLink>
        {user?.role === 'ADMIN' && <NavLink to="/admin" end onClick={closeMenu}>Admin</NavLink>}
        {user && <button className="menu-toggle signout" type="button" onClick={() => { signOut(); closeMenu() }}>Sign out</button>}
      </nav>
    </div>
  )
}
