import { money } from '../api/format.js'

/** @param {{account: import('../api/types.js').Account}} props */
export default function AccountSummary({ account }) {
  let creditUsedPercent = 0
  if (account.creditLimit > 0) {
    creditUsedPercent = Math.min(100, Math.max(0, account.outstandingBalance / account.creditLimit * 100))
  }
  const creditUsedLabel = `${creditUsedPercent.toFixed(1)}%`

  return (
    <div>
      <p>Account #{account.id} · {account.status}</p>
      <dl className="summary-grid">
        <div>
          <dt>Credit limit</dt>
          <dd>{money(account.creditLimit)}</dd>
        </div>
        <div>
          <dt>Outstanding balance</dt>
          <dd>{money(account.outstandingBalance)}</dd>
        </div>
        <div>
          <dt>Available credit</dt>
          <dd>{money(account.availableCredit)}</dd>
        </div>
      </dl>
      <div className="credit-use">
        <p className="credit-use-label">
          <span>Credit used</span>
          <span>{creditUsedLabel} of your limit</span>
        </p>
        <div
          className="credit-use-track"
          role="meter"
          aria-label="Credit used"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={creditUsedPercent}
          aria-valuetext={`${money(account.outstandingBalance)} of ${money(account.creditLimit)} used (${creditUsedLabel})`}
        >
          <span className="credit-use-fill" style={{ width: `${creditUsedPercent}%` }} />
        </div>
      </div>
    </div>
  )
}
