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
  const [registration, setRegistration] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState(/** @type {Record<string, string>} */ ({}))
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />

  /** @param {import('react').FormEvent<HTMLFormElement>} event */
  async function submit(event) {
    event.preventDefault()
    if (pending) return
    setError(''); setFields({}); setMessage('')
    if (new TextEncoder().encode(password).length > 72) {
      setFields({ password: 'Use a password of at most 72 UTF-8 bytes.' }); return
    }
    setPending(true)
    try {
      if (registration) {
        await register({ displayName: name.trim(), email: email.trim(), password })
        setRegistration(false); setPassword(''); setMessage('Your fictional account is ready. Sign in to continue.')
      } else {
        const result = await login({ email: email.trim(), password })
        setPassword(''); signIn(result)
        navigate(result.user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
      }
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Please try again.')
      if (failure instanceof ApiError) setFields(failure.fields)
    } finally { setPending(false) }
  }
  return <main id="main-content" className="content-page" tabIndex={-1}>
    <PageHeading eyebrow="Credit Circuit / Access" title={registration ? 'Create account' : 'Sign in'} description="Fictional cards. Real application rules." />
    <Card title={registration ? 'Start your simulation' : 'Welcome back'}>
      <p>Use a fictional identity and a password you do not use elsewhere.</p>
      {notice && <p role="status">{notice}</p>}{message && <p role="status">{message}</p>}
      {error && <p className="error" role="alert">{error}</p>}
      <form onSubmit={submit}>
        <fieldset disabled={pending}>
          {registration && <Input label="Display name" name="displayName" autoComplete="nickname" required maxLength={100} value={name} onChange={e => setName(e.target.value)} error={fields.displayName} />}
          <Input label="Email" name="email" type="email" autoComplete="username" required maxLength={150} value={email} onChange={e => setEmail(e.target.value)} error={fields.email} />
          <Input label="Password" name="password" type="password" autoComplete={registration ? 'new-password' : 'current-password'} required minLength={registration ? 12 : 1} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} error={fields.password} />
          {registration && <p className="hint">Use 12–72 characters, within 72 UTF-8 bytes.</p>}
          <Button type="submit" pending={pending}>{registration ? 'Create account' : 'Sign in'}</Button>
        </fieldset>
      </form>
      <Button type="button" disabled={pending} onClick={() => { setRegistration(!registration); setPassword(''); setError(''); setFields({}); setMessage('') }}>{registration ? 'Already registered? Sign in' : 'New here? Create account'}</Button>
      <p className="hint">Sign-in lasts 15 minutes. Reloading or signing out clears access on this device. A copied token remains usable until it expires.</p>
    </Card>
  </main>
}
