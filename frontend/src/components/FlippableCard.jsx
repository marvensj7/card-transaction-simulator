import { useEffect, useRef, useState } from 'react'
import CircuitMark from './CircuitMark.jsx'

/** @param {{card?: import('../api/types.js').DemoCard | null}} props */
export default function FlippableCard({ card = null }) {
  const [showBack, setShowBack] = useState(false)
  const [modelReady, setModelReady] = useState(false)
  const modelHost = useRef(/** @type {HTMLSpanElement | null} */ (null))
  const modelScene = useRef(/** @type {ReturnType<typeof import('./cardScene.js').createCardScene>} */ (null))
  const currentSide = useRef(false)
  const maskedNumber = card?.maskedNumber || '•••• 4242'
  const label = card?.label || 'Fictional display card'
  const expiry = card ? `${String(card.expiryMonth).padStart(2, '0')}/${card.expiryYear}` : 'DEMO'

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
    <figure className="credit-card-preview" aria-label="Fictional Credit Circuit display card">
      <p className="sr-only">{label}. Card ending in {maskedNumber.slice(-4)}. {card && `Expiry ${expiry}.`}</p>
      <button
        type="button"
        className="credit-card-flip"
        data-renderer={modelReady ? 'three' : 'css'}
        aria-label="Flip fictional card"
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
              <span className="credit-card-number"><span aria-hidden="true">{maskedNumber}</span></span>
              <span className="credit-card-meta"><span>{label}</span><span>Valid thru<br />{expiry}</span></span>
              <span className="credit-card-footer"><span>Fictional card</span><span>CC / 01</span></span>
            </span>
          </span>
          <span className="credit-card-face credit-card-back" aria-hidden={!showBack}>
            <span className="credit-card-stripe" />
            <span className="credit-card-signature"><span>Simulation only</span><span>•••</span></span>
            <span className="credit-card-back-copy">Every purchase starts a signal.</span>
            <span className="credit-card-back-note">Fictional card · No real payments</span>
            <span className="credit-card-back-brand"><CircuitMark /><span>Credit Circuit</span></span>
          </span>
        </span>
        <span className="credit-card-viewport" ref={modelHost} aria-hidden="true" />
      </button>
      <figcaption>{showBack ? 'Back' : 'Front'} · Click, tap, or press Enter to flip</figcaption>
    </figure>
  )
}
