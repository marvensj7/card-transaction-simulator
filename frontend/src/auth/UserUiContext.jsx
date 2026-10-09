import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useRef,
} from 'react'
import { configureAuthentication } from '../api/fetchJson.js'

/** @typedef {{id: number, displayName: string, email: string, role: 'USER' | 'ADMIN'}} UiUser */
/** @typedef {{user: UiUser | null, expiresAt: number, notice: string}} AuthState */
/** @typedef {{type: 'signedIn', user: UiUser, expiresAt: number} | {type: 'signedOut' | 'expired'}} AuthAction */
/** @typedef {{user: UiUser, accessToken: string, expiresAt: string}} LoginResult */
/** @typedef {{user: UiUser | null, notice: string, signIn: (result: LoginResult) => void, signOut: () => void}} UserUiState */

/** @param {AuthState} state @param {AuthAction} action @returns {AuthState} */
export function authReducer(state, action) {
  if (action.type === 'signedIn') {
    return { user: action.user, expiresAt: action.expiresAt, notice: '' }
  } else if (action.type === 'expired') {
    return { user: null, expiresAt: 0, notice: 'Your sign-in expired. Sign in again to continue.' }
  } else {
    return { user: null, expiresAt: 0, notice: 'You signed out.' }
  }
}

const UserUiContext = createContext(/** @type {UserUiState | undefined} */ (undefined))

/** @param {{children: import('react').ReactNode}} props */
export function UserUiProvider({ children }) {
  const [authenticationState, dispatchAuthentication] = useReducer(authReducer, { user: null, expiresAt: 0, notice: '' })
  // The credential has one home in React memory. No browser storage is used.
  const accessToken = useRef('')

  const expireAuthentication = useCallback(() => {
    accessToken.current = ''
    dispatchAuthentication({ type: 'expired' })
  }, [])
  const signOut = useCallback(() => {
    accessToken.current = ''
    dispatchAuthentication({ type: 'signedOut' })
  }, [])
  const signIn = useCallback(/** @param {LoginResult} loginResult */ (loginResult) => {
    const expiresAt = Date.parse(loginResult.expiresAt)
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      expireAuthentication()
      return
    }
    accessToken.current = loginResult.accessToken
    dispatchAuthentication({ type: 'signedIn', user: loginResult.user, expiresAt })
  }, [expireAuthentication])

  useEffect(() => {
    function readAccessToken() {
      if (authenticationState.expiresAt && Date.now() >= authenticationState.expiresAt) {
        expireAuthentication()
        return ''
      }
      return accessToken.current
    }
    configureAuthentication(readAccessToken, expireAuthentication)
    return () => {
      configureAuthentication(() => '', () => {})
    }
  }, [authenticationState.expiresAt, expireAuthentication])

  useEffect(() => {
    if (!authenticationState.expiresAt) {
      return
    }
    const expirationTimer = window.setTimeout(expireAuthentication, Math.max(0, authenticationState.expiresAt - Date.now()))
    function checkExpirationTime() {
      if (Date.now() >= authenticationState.expiresAt) {
        expireAuthentication()
      }
    }
    // Background tabs can delay timers. Recheck when browser visibility changes.
    document.addEventListener('visibilitychange', checkExpirationTime)
    return () => {
      window.clearTimeout(expirationTimer)
      document.removeEventListener('visibilitychange', checkExpirationTime)
    }
  }, [authenticationState.expiresAt, expireAuthentication])

  const userUiState = useMemo(() => ({
    user: authenticationState.user,
    notice: authenticationState.notice,
    signIn,
    signOut,
  }), [authenticationState.user, authenticationState.notice, signIn, signOut])

  return (
    <UserUiContext.Provider value={userUiState}>
      {children}
    </UserUiContext.Provider>
  )
}

export function useUserUi() {
  const userUiState = useContext(UserUiContext)
  if (!userUiState) {
    throw new Error('User UI state needs UserUiProvider.')
  }
  return userUiState
}
