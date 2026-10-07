export default function HomePage() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <span className="project-name">
          <span className="brand-mark" aria-hidden="true" />
          Card Simulator
        </span>
        <span className="project-label">UCI 2123 · Capstone</span>
      </header>

      <main>
        <section className="hero" aria-labelledby="home-heading">
          <div className="hero-copy">
            <p className="eyebrow">Credit Card Transaction Simulator</p>
            <h1 id="home-heading">Behind every <span>swipe.</span></h1>
            <p className="introduction">
              A purchase is just the beginning. Explore the decisions,
              balance changes, and history that follow.
            </p>
          </div>

          <div className="transaction-art" aria-hidden="true">
            <div className="art-orbit" />
            <div className="illustrated-card">
              <span className="card-caption">THE TRANSACTION JOURNEY</span>
              <span className="card-symbol">↗</span>
              <span className="card-title">One purchase.<br />A chain of decisions.</span>
              <span className="card-line" />
            </div>
            <div className="journey-label"><span /> Request → Decision → Record</div>
          </div>
        </section>

        <section className="journey" aria-labelledby="journey-heading">
          <div className="section-heading">
            <h2 id="journey-heading">Every outcome has a reason.</h2>
            <p>Follow the transaction from start to finish.</p>
          </div>
          <div className="journey-grid">
            <article className="journey-step">
              <span className="step-number">01</span>
              <h3>The purchase</h3>
              <p>Every request starts with a card, a merchant, and an amount.</p>
            </article>
            <article className="journey-step">
              <span className="step-number">02</span>
              <h3>The decision</h3>
              <p>Account status, card expiry, and available credit shape the outcome.</p>
            </article>
            <article className="journey-step">
              <span className="step-number">03</span>
              <h3>The bigger picture</h3>
              <p>Balance changes, saved history, and refunds complete the story.</p>
            </article>
          </div>
        </section>
      </main>

      <footer className="site-footer">
        <span>UCI 2123 · Capstone project</span>
        <span>Fictional cards and balances.</span>
      </footer>
    </div>
  )
}
