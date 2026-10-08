import { money, reasonLabel } from '../api/format.js'
import Table from './Table.jsx'
import Button from './Button.jsx'
/** @param {{items: import('../api/types.js').Transaction[], pending?: boolean, onRefund?: (transaction: import('../api/types.js').Transaction) => void}} props */
export default function TransactionTable({ items, pending = false, onRefund }) {
  const headers = ['Time', 'Account / ID', 'Merchant', 'Type', 'Outcome', 'Amount', 'Balance after']
  if (onRefund) headers.push('Refund')
  return <Table caption="Transaction activity, newest first" headers={headers}>
    {items.map(transaction => <tr key={transaction.id}>
      <td><time dateTime={transaction.createdAt}>{new Date(transaction.createdAt).toLocaleString()}</time></td>
      <td>#{transaction.accountId} / #{transaction.id}</td><td>{transaction.merchantName}</td><td>{transaction.type}</td>
      <td>{transaction.status}{transaction.refunded && <span> · Refunded</span>}<span className="hint outcome-reason">{reasonLabel(transaction.reasonCode)}</span>{transaction.originalPurchaseId && <span className="hint">Purchase #{transaction.originalPurchaseId}</span>}</td>
      <td>{money(transaction.amount)}</td><td>{money(transaction.outstandingAfter)}</td>
      {onRefund && <td>{transaction.type === 'PURCHASE' && transaction.status === 'APPROVED' && !transaction.refunded ? <Button disabled={pending} onClick={() => onRefund(transaction)}>Full refund #{transaction.id}</Button> : '—'}</td>}
    </tr>)}
  </Table>
}
