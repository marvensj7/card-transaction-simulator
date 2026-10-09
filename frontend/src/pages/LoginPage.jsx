import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { login, register } from '../api/creditCircuitApi.js'
import { ApiError } from '../api/fetchJson.js'
import { useUserUi } from '../auth/UserUiContext.jsx'
import PageHeading from '../components/PageHeading.jsx'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import Card from '../components/Card.jsx'

export default function LoginPage() {
  const { user, notice, signIn } = useUserUi()
  const navigate = useNavigate()
  const [registrationFormVisible, setRegistrationFormVisible] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [authenticationRequestPending, setAuthenticationRequestPending] = useState(false)
  const [registrationMessage, setRegistrationMessage] = useState('')
  const [authenticationError, setAuthenticationError] = useState('')
  const [fieldErrors, setFieldErrors] = useState(/** @type {Record<string, string>} */ ({}))

  if (user) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />
  }

  /** @param {import('react').SubmitEvent<HTMLFormElement>} event */
  async function handleAuthenticationSubmit(event) {
    event.preventDefault()
    if (authenticationRequestPending) {
      return
    }
    setAuthenticationError('')
    setFieldErrors({})
    setRegistrationMessage('')
    if (new TextEncoder().encode(password).length > 72) {
      setFieldErrors({ password: 'Use a password of at most 72 UTF-8 bytes.' })
      return
    }

    setAuthenticationRequestPending(true)
    try {
      if (registrationFormVisible) {
        await register({ displayName: displayName.trim(), email: email.trim(), password })
        setRegistrationFormVisible(false)
        setPassword('')
        setRegistrationMessage('Your fictional account is ready. Sign in to continue.')
      } else {
        const loginResult = await login({ email: email.trim(), password })
        setPassword('')
        signIn(loginResult)
        navigate(loginResult.user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
      }
    } catch (failure) {
      setAuthenticationError(failure instanceof Error ? failure.message : 'Please try again.')
      if (failure instanceof ApiError) {
        setFieldErrors(failure.fields)
      }
    } finally {
      setAuthenticationRequestPending(false)
    }
  }

  function handleAuthenticationFormToggle() {
    setRegistrationFormVisible(previousVisible => !previousVisible)
    setPassword('')
    setAuthenticationError('')
    setFieldErrors({})
    setRegistrationMessage('')
  }

  return (
    <main id="main-content" className="content-page" tabIndex={-1}>
      <PageHeading
        eyebrow="Credit Circuit / Access"
        title={registrationFormVisible ? 'Create account' : 'Sign in'}
        description="Fictional cards. Real application rules."
      />
      <Card title={registrationFormVisible ? 'Start your simulation' : 'Welcome back'}>
        <p>Use a fictional identity and a password you do not use elsewhere.</p>
        {notice && <output className="notice">{notice}</output>}
        {registrationMessage && <output className="notice">{registrationMessage}</output>}
        {authenticationError && (
          <p className="error" role="alert">{authenticationError}</p>
        )}

        <form onSubmit={handleAuthenticationSubmit}>
          <fieldset disabled={authenticationRequestPending}>
            {registrationFormVisible && (
              <Input
                label="Display name"
                name="displayName"
                autoComplete="nickname"
                required
                maxLength={100}
                value={displayName}
                onChange={event => setDisplayName(event.target.value)}
                error={fieldErrors.displayName}
              />
            )}
            <Input
              label="Email"
              name="email"
              type="email"
              autoComplete="username"
              required
              maxLength={150}
              value={email}
              onChange={event => setEmail(event.target.value)}
              error={fieldErrors.email}
            />
            <Input
              label="Password"
              name="password"
              type="password"
              autoComplete={registrationFormVisible ? 'new-password' : 'current-password'}
              required
              minLength={registrationFormVisible ? 12 : 1}
              maxLength={72}
              value={password}
              onChange={event => setPassword(event.target.value)}
              error={fieldErrors.password}
            />
            {registrationFormVisible && (
              <p className="hint">Use 12–72 characters, within 72 UTF-8 bytes.</p>
            )}
            <Button type="submit" pending={authenticationRequestPending}>
              {registrationFormVisible ? 'Create account' : 'Sign in'}
            </Button>
          </fieldset>
        </form>

        <Button
          type="button"
          disabled={authenticationRequestPending}
          onClick={handleAuthenticationFormToggle}
        >
          {registrationFormVisible ? 'Already registered? Sign in' : 'New here? Create account'}
        </Button>
        <p className="hint">
          Sign-in lasts 15 minutes. Reloading or signing out clears access on this device.
          A copied token remains usable until it expires.
        </p>
      </Card>
    </main>
  )
}
