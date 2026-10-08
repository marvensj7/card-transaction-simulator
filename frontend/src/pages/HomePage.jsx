import CircuitMark from '../components/CircuitMark.jsx'

export default function HomePage() {
  return (
    <main id="main-content" className="opening" tabIndex={-1}>
      <div className="introduction">
        <p className="eyebrow">Credit Card Transaction Simulator</p>
        <h1>Credit<br /><span>Circuit.</span></h1>
        <p className="intro-copy">Every purchase starts a signal.</p>
        <ol className="signal-route" aria-label="Purchase path" role="list">
          <li><span className="route-node" aria-hidden="true" />Request</li>
          <li><span className="route-node" aria-hidden="true" />Checks</li>
          <li><span className="route-node" aria-hidden="true" />Outcome</li>
        </ol>
      </div>

      <figure className="card-stage" aria-label="Fictional Credit Circuit display card">
        <svg className="signal-traces" viewBox="0 0 600 600" fill="none" aria-hidden="true" focusable="false">
          <path d="M0 330H96L152 274H332L398 208H600" />
          <path d="M70 510H210L278 442H600" />
          <path d="M366 0V72L440 146V224" />
          <circle cx="70" cy="510" r="5" />
          <circle cx="366" cy="12" r="5" />
        </svg>
        <div className="display-card">
          <div className="card-heading">
            <span>Credit Circuit</span>
            <span className="card-chip" aria-hidden="true" />
          </div>
          <div className="card-emblem"><CircuitMark /></div>
          <div className="card-details">
            <p className="masked-number">
              <span aria-hidden="true">•••• 4242</span>
              <span className="sr-only">Card ending in 4242</span>
            </p>
            <div className="card-footer">
              <span>Fictional card</span>
              <span className="card-edition" aria-hidden="true">CC / 01</span>
            </div>
          </div>
        </div>
        <figcaption>One signal. A decision ahead.</figcaption>
      </figure>
    </main>
  )
}
