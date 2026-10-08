import { createContext, useCallback, useContext, useEffect, useMemo, useReducer, useRef } from 'react'
import { configureAuthentication } from '../api/fetchJson.js'

/** @typedef {{id: number, displayName: string, email: string, role: 'USER' | 'ADMIN'}} UiUser */
/** @typedef {{user: UiUser | null, expiresAt: number, notice: string}} AuthState */
/** @typedef {{type: 'signedIn', user: UiUser, expiresAt: number} | {type: 'signedOut' | 'expired'}} AuthAction */
/** @typedef {{user: UiUser, accessToken: string, expiresAt: string}} LoginResult */
/** @typedef {{user: UiUser | null, notice: string, signIn: (result: LoginResult) => void, signOut: () => void}} UserUiState */

/** @param {AuthState} state @param {AuthAction} action @returns {AuthState} */
export function authReducer(state, action) {
  if (action.type === 'signedIn') return { user: action.user, expiresAt: action.expiresAt, notice: '' }
  if (action.type === 'expired') return { user: null, expiresAt: 0, notice: 'Your sign-in expired. Sign in again to continue.' }
  return { user: null, expiresAt: 0, notice: 'You signed out.' }
}
const UserUiContext = createContext(/** @type {UserUiState | undefined} */ (undefined))

/** @param {{children: import('react').ReactNode}} props */
export function UserUiProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, { user: null, expiresAt: 0, notice: '' })
  // The credential has one home in React memory. No browser storage is used.
  const token = useRef('')
  const expire = useCallback(() => { token.current = ''; dispatch({ type: 'expired' }) }, [])
  const signOut = useCallback(() => { token.current = ''; dispatch({ type: 'signedOut' }) }, [])
  const signIn = useCallback(/** @param {LoginResult} result */ (result) => {
    const expiresAt = Date.parse(result.expiresAt)
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) { expire(); return }
    token.current = result.accessToken
    dispatch({ type: 'signedIn', user: result.user, expiresAt })
  }, [expire])

  useEffect(() => {
    configureAuthentication(() => {
      if (state.expiresAt && Date.now() >= state.expiresAt) { expire(); return '' }
      return token.current
    }, expire)
    return () => configureAuthentication(() => '', () => {})
  }, [state.expiresAt, expire])

  useEffect(() => {
    if (!state.expiresAt) return
    const timer = window.setTimeout(expire, Math.max(0, state.expiresAt - Date.now()))
    function checkTime() { if (Date.now() >= state.expiresAt) expire() }
    document.addEventListener('visibilitychange', checkTime)
    return () => { window.clearTimeout(timer); document.removeEventListener('visibilitychange', checkTime) }
  }, [state.expiresAt, expire])

  // Stable handlers and value keep every context consumer from rerendering for unrelated parent renders.
  const value = useMemo(() => ({ user: state.user, notice: state.notice, signIn, signOut }), [state.user, state.notice, signIn, signOut])
  return <UserUiContext.Provider value={value}>{children}</UserUiContext.Provider>
}
export function useUserUi() {
  const context = useContext(UserUiContext)
  if (!context) throw new Error('User UI state needs UserUiProvider.')
  return context
}
