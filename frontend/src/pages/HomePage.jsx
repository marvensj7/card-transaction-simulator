import FlippableCard from '../components/FlippableCard.jsx'

export default function HomePage() {
  return (
    <main id="main-content" className="opening" tabIndex={-1}>
      <div className="introduction">
        <p className="eyebrow">Credit Card Transaction Simulator</p>
        <h1>Credit<br /><span>Circuit.</span></h1>
        <p className="intro-copy">Every purchase starts a signal.</p>
        <ol className="signal-route" aria-label="Purchase path">
          <li><span className="route-node" aria-hidden="true" />Request</li>
          <li><span className="route-node" aria-hidden="true" />Checks</li>
          <li><span className="route-node" aria-hidden="true" />Outcome</li>
        </ol>
      </div>

      <div className="card-stage">
        <svg className="signal-traces" viewBox="0 0 600 600" fill="none" aria-hidden="true" focusable="false">
          <path d="M0 330H96L152 274H332L398 208H600" />
          <path d="M70 510H210L278 442H600" />
          <path d="M366 0V72L440 146V224" />
          <circle cx="70" cy="510" r="5" />
          <circle cx="366" cy="12" r="5" />
        </svg>
        <FlippableCard />
      </div>
    </main>
  )
}
