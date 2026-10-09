import { money } from '../api/format.js'

/** @param {{account: import('../api/types.js').Account}} props */
export default function AccountSummary({ account }) {
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
    </div>
  )
}
