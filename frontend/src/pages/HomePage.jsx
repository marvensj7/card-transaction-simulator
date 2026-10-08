export default function HomePage() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <span className="brand">
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          Card Simulator
        </span>
        <span className="project-label">UCI 2123 <span aria-hidden="true">/</span> Capstone project</span>
      </header>

      <main className="home-layout">
        <div className="introduction">
          <p className="eyebrow">Credit card transaction simulator</p>
          <h1>Follow<br />the credit.</h1>
          <p className="intro-copy">
            One purchase. One decision. See exactly what changes in the account.
          </p>
          <div className="scenario-note">
            <span className="scenario-number" aria-hidden="true">01</span>
            <p>A $125 purchase,<br />from request to record.</p>
          </div>
        </div>

        <section className="workbench" aria-labelledby="workbench-heading">
          <header className="workbench-header">
            <div>
              <h2 id="workbench-heading">Sample workbench</h2>
              <p>Fictional display · no live account data</p>
            </div>
            <span className="currency-label">USD</span>
          </header>

          <div className="account-and-decision">
            <section className="account-panel" aria-labelledby="account-heading">
              <p className="panel-label">01 / Account before purchase</p>
              <h3 id="account-heading">Everyday credit</h3>
              <p className="account-status"><span aria-hidden="true" /> Active account</p>
              <dl className="account-values">
                <div><dt>Credit limit</dt><dd>$2,000.00</dd></div>
                <div><dt>Outstanding balance</dt><dd>$500.00</dd></div>
                <div><dt>Available credit</dt><dd>$1,500.00</dd></div>
              </dl>
              <p className="masked-card">Sample card <span>•••• 4242</span></p>
            </section>

            <section className="decision-panel" aria-labelledby="decision-heading">
              <div className="decision-heading">
                <h3 id="decision-heading" className="panel-label">02 / Purchase decision</h3>
                <span className="approved-status"><span aria-hidden="true">✓</span> Approved</span>
              </div>
              <p className="purchase-detail">Demo Bookstore <span>· $125.00 purchase</span></p>

              <div className="credit-change">
                <div className="credit-before">
                  <p>Available before</p>
                  <span>$1,500.00</span>
                </div>
                <span className="change-arrow" aria-hidden="true">→</span>
                <div className="credit-after">
                  <p>Available after</p>
                  <strong>$1,375<span>.00</span></strong>
                </div>
              </div>

              <div
                className="credit-breakdown"
                role="img"
                aria-label="Sample $2,000 credit limit after purchase: $500 previous balance, $125 new purchase, and $1,375 available credit."
              >
                <span className="previous-balance" />
                <span className="new-purchase" />
                <span className="remaining-credit" />
              </div>
              <ul className="credit-legend" aria-hidden="true">
                <li><span className="legend-dot previous-balance" />Previous balance</li>
                <li><span className="legend-dot new-purchase" />New purchase</li>
                <li><span className="legend-dot remaining-credit" />Available</li>
              </ul>

              <p className="decision-reason">Active account. Purchase within available credit.</p>
              <div className="balance-change">
                <span>Outstanding balance</span>
                <p>$500.00 <span aria-label="increases to">→</span> <strong>$625.00</strong></p>
              </div>
            </section>
          </div>

          <section className="history-panel" aria-labelledby="history-heading">
            <div className="history-heading">
              <h3 id="history-heading" className="panel-label">03 / Transaction history</h3>
              <span>Sample records · newest first</span>
            </div>
            <ol className="history-list" role="list">
              <li className="history-record current-purchase">
                <div className="merchant">
                  <h4>Demo Bookstore</h4>
                  <p>Purchase · 10:42</p>
                </div>
                <p className="history-status"><span className="visually-hidden">Decision: </span>Approved</p>
                <dl className="record-values">
                  <div><dt>Amount</dt><dd>$125.00</dd></div>
                  <div><dt>Balance after</dt><dd>$625.00</dd></div>
                </dl>
              </li>
              <li className="history-record">
                <div className="merchant">
                  <h4>Demo Market</h4>
                  <p>Purchase · 09:18</p>
                </div>
                <p className="history-status"><span className="visually-hidden">Decision: </span>Approved</p>
                <dl className="record-values">
                  <div><dt>Amount</dt><dd>$80.00</dd></div>
                  <div><dt>Balance after</dt><dd>$500.00</dd></div>
                </dl>
              </li>
            </ol>
          </section>
          <p className="workbench-note">The same purchase changes the balance and adds a history record.</p>
        </section>
      </main>

      <footer className="site-footer">
        <span>Request. Decision. Record.</span>
        <span>Credit Card Transaction Simulator</span>
      </footer>
    </div>
  )
}
