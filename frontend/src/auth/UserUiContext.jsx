import { createContext, useContext, useState } from 'react'

/**
 * Safe user details for display, never proof of authentication.
 * @typedef {{ id: number, displayName: string, role: 'USER' | 'ADMIN' }} UiUser
 * @typedef {{ user: UiUser | null, setUser: import('react').Dispatch<import('react').SetStateAction<UiUser | null>> }} UserUiState
 */
const UserUiContext = createContext(/** @type {UserUiState | undefined} */ (undefined))

/** @param {{ children: import('react').ReactNode }} props */
export function UserUiProvider({ children }) {
  const [user, setUser] = useState(/** @type {UiUser | null} */ (null))

  // Later, only a verified sign-in/current-user response may supply user details.
  // Nothing writes them today. A reload starts anonymous; the server decides access.
  return (
    <UserUiContext.Provider value={{ user, setUser }}>
      {children}
    </UserUiContext.Provider>
  )
}

export function useUserUi() {
  const context = useContext(UserUiContext)

  if (context === undefined) {
    throw new Error('User UI state needs UserUiProvider.')
  }

  return context
}
