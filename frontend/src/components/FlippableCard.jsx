import { useState } from 'react'
import CircuitMark from './CircuitMark.jsx'

/** @param {{card?: import('../api/types.js').DemoCard | null}} props */
export default function FlippableCard({ card = null }) {
  const [showBack, setShowBack] = useState(false)
  const maskedNumber = card?.maskedNumber || '•••• 4242'
  const label = card?.label || 'Fictional display card'
  const expiry = card ? `${String(card.expiryMonth).padStart(2, '0')}/${card.expiryYear}` : 'DEMO'

  return (
    <figure className="credit-card-preview" aria-label="Fictional Credit Circuit display card">
      <p className="sr-only">{label}. Card ending in {maskedNumber.slice(-4)}. {card && `Expiry ${expiry}.`}</p>
      <button
        type="button"
        className="credit-card-flip"
        aria-label="Flip fictional card"
        aria-pressed={showBack}
        onClick={() => setShowBack(previousSide => !previousSide)}
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
      </button>
      <figcaption>{showBack ? 'Back' : 'Front'} · Click, tap, or press Enter to flip</figcaption>
    </figure>
  )
}
