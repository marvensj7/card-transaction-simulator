export default function HomePage() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <span className="project-name">Card Transaction Simulator</span>
        <span className="simulation-label">Fictional simulation</span>
      </header>

      <main className="home-page">
        <p className="eyebrow">UCI 2123 · Capstone project</p>
        <h1>Credit Card Transaction Simulator</h1>
        <p className="introduction">
          A learning project about credit card transactions, built with
          fictional cards, balances, and purchases.
        </p>
        <p className="simulation-notice">No real payments are processed.</p>

        <section className="about-section" aria-labelledby="about-heading">
          <h2 id="about-heading">About this simulator</h2>
          <div className="about-grid">
            <article className="about-panel">
              <h3>Fictional data only</h3>
              <p>
                All accounts, cards, and money belong to the simulation.
                Real payment card details do not belong here.
              </p>
            </article>
            <article className="about-panel">
              <h3>A local learning project</h3>
              <p>
                The project brings together a React interface, a Spring Boot
                API, and a MySQL database. It does not connect to a bank or
                payment network.
              </p>
            </article>
          </div>
        </section>
      </main>

      <footer className="site-footer">
        Built for learning with fictional cards and balances.
      </footer>
    </div>
  )
}
