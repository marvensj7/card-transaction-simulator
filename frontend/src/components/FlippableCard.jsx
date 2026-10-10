import { useCallback, useEffect, useId, useRef, useState } from 'react'
import CircuitMark from './CircuitMark.jsx'
import Button from './Button.jsx'
import { fictionalCardNumber } from '../api/fictionalCardNumber.js'

/** @param {{card?: import('../api/types.js').DemoCard | null, account?: import('../api/types.js').Account | null, allowReveal?: boolean}} props */
export default function FlippableCard({ card = null, account = null, allowReveal = false }) {
  const [showBack, setShowBack] = useState(false)
  const [detailsVisible, setDetailsVisible] = useState(false)
  const [sampleCode, setSampleCode] = useState('')
  const detailsId = useId()
  const [modelReady, setModelReady] = useState(false)
  const modelHost = useRef(/** @type {HTMLSpanElement | null} */ (null))
  const modelScene = useRef(/** @type {ReturnType<typeof import('./cardScene.js').createCardScene>} */ (null))
  const currentSide = useRef(false)
  const maskedNumber = card?.maskedNumber || '•••• 4242'
  const label = card?.label || 'Credit Circuit card'
  const expiry = card ? `${String(card.expiryMonth).padStart(2, '0')}/${card.expiryYear}` : 'DEMO'
  const canReveal = Boolean(allowReveal && card && account && fictionalCardNumber(card, account.id))
  let displayNumber = maskedNumber
  if (detailsVisible && canReveal && card && account) {
    displayNumber = fictionalCardNumber(card, account.id).replace(/(.{4})(?=.)/g, '$1 ')
  }
  const hideDetails = useCallback(() => {
    setDetailsVisible(false)
    setSampleCode('')
    // Redraw immediately, even if the tab is now hidden.
    modelScene.current?.setDetails(maskedNumber, '')
  }, [maskedNumber])

  useEffect(() => {
    if (!detailsVisible) return
    const timer = window.setTimeout(hideDetails, 20000)
    return () => window.clearTimeout(timer)
  }, [detailsVisible, hideDetails])

  useEffect(() => {
    function handleVisibilityChange() {
      if (document.hidden) hideDetails()
    }
    window.addEventListener('blur', hideDetails)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      window.removeEventListener('blur', hideDetails)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [hideDetails])

  useEffect(() => {
    let active = true
    setModelReady(false)
    async function loadModel() {
      if (!window.WebGL2RenderingContext || !modelHost.current) return
      try {
        // Load the rendering library only after the ordinary card is on screen.
        const { createCardScene } = await import('./cardScene.js')
        if (!active || !modelHost.current) return
        modelScene.current = createCardScene(modelHost.current, label, maskedNumber, expiry, () => {
          modelScene.current?.dispose()
          modelScene.current = null
          if (active) setModelReady(false)
        })
        modelScene.current?.setSide(currentSide.current)
        setModelReady(Boolean(modelScene.current))
      } catch {
        modelScene.current?.dispose()
        modelScene.current = null
        if (active) setModelReady(false)
      }
    }
    loadModel()
    return () => {
      active = false
      modelScene.current?.dispose()
      modelScene.current = null
    }
  }, [label, maskedNumber, expiry])

  useEffect(() => {
    modelScene.current?.setDetails(displayNumber, sampleCode)
  }, [displayNumber, sampleCode, modelReady])

  useEffect(() => {
    modelScene.current?.setFrozen(account?.status === 'FROZEN')
  }, [account?.status, modelReady])

  function handleReveal() {
    if (detailsVisible) {
      hideDetails()
    } else if (canReveal) {
      setSampleCode(String(Math.floor(Math.random() * 900) + 100))
      setDetailsVisible(true)
    }
  }

  function handleFlip() {
    const nextSide = !currentSide.current
    currentSide.current = nextSide
    setShowBack(nextSide)
    modelScene.current?.setSide(nextSide)
  }

  /** @param {import('react').PointerEvent<HTMLButtonElement>} event */
  function handlePointerMove(event) {
    if (event.pointerType === 'touch') return
    const bounds = event.currentTarget.getBoundingClientRect()
    const x = (event.clientX - bounds.left) / bounds.width * 2 - 1
    const y = (event.clientY - bounds.top) / bounds.height * 2 - 1
    modelScene.current?.setPointer(true, x, y)
  }

  return (
    <figure className="credit-card-preview" aria-label="Credit Circuit display card">
      <p className="sr-only">{label}. Card ending in {maskedNumber.slice(-4)}. {card && `Expiry ${expiry}.`}</p>
      {account && <p className={`card-status ${account.status === 'FROZEN' ? 'card-status-frozen' : ''}`}>{account.status === 'FROZEN' ? 'Frozen · Purchases paused' : 'Active · Ready to use'}</p>}
      <button
        type="button"
        className="credit-card-flip"
        data-renderer={modelReady ? 'three' : 'css'}
        data-account-status={account?.status}
        aria-label="Flip card"
        aria-pressed={showBack}
        onClick={handleFlip}
        onPointerMove={handlePointerMove}
        onPointerLeave={() => modelScene.current?.setPointer(false, 0, 0)}
        onFocus={() => modelScene.current?.setPointer(true, 0, 0)}
        onBlur={() => modelScene.current?.setPointer(false, 0, 0)}
      >
        <span className="credit-card-rotator" data-flipped={showBack}>
          <span className="credit-card-face credit-card-front" aria-hidden={showBack}>
            <span className="card-heading"><span>Credit Circuit</span><span className="card-chip" aria-hidden="true" /></span>
            <span className="credit-card-watermark"><CircuitMark /></span>
            <span className="credit-card-details">
              <span className="credit-card-number"><span aria-hidden="true">{displayNumber}</span></span>
              <span className="credit-card-meta"><span>{label}</span><span>Valid thru<br />{expiry}</span></span>
              <span className="credit-card-footer"><span>Signal / Credit</span><span>CC / 01</span></span>
            </span>
          </span>
          <span className="credit-card-face credit-card-back" aria-hidden={!showBack}>
            <span className="credit-card-stripe" />
            <span className="credit-card-signature"><span>{detailsVisible ? 'Demo security code' : 'Credit Circuit'}</span><span>{detailsVisible ? sampleCode : '•••'}</span></span>
            <span className="credit-card-back-copy">Every purchase starts a signal.</span>
            <span className="credit-card-back-note">Request · Checks · Outcome</span>
            <span className="credit-card-back-brand"><CircuitMark /><span>Credit Circuit</span></span>
          </span>
        </span>
        <span className="credit-card-viewport" ref={modelHost} aria-hidden="true" />
      </button>
      <figcaption>{showBack ? 'Back' : 'Front'} · Click, tap, or press Enter to flip</figcaption>
      {canReveal && (
        <div className="card-privacy-controls">
          <Button type="button" onClick={handleReveal} aria-expanded={detailsVisible} aria-controls={detailsId}>
            {detailsVisible ? 'Hide details' : 'Show details'}
          </Button>
          <p className="hint" aria-live="polite">{detailsVisible ? 'Details hide after 20 seconds or when you leave this window.' : 'Card details are hidden.'}</p>
          <dl className="card-revealed-details" id={detailsId} hidden={!detailsVisible}>
            {detailsVisible && (showBack ? (
              <div><dt>Demo security code</dt><dd>{sampleCode}</dd></div>
            ) : (
              <div><dt>Fictional card number</dt><dd>{displayNumber}</dd></div>
            ))}
          </dl>
          <p className="hint">The demo security code is a sample. The simulator checks its format only.</p>
        </div>
      )}
    </figure>
  )
}
