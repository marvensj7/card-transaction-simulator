import { useEffect, useRef, useState } from 'react'
import { getAdminAccounts, getAdminTransactions, updateAccountStatus } from '../api/creditCircuitApi.js'
import { money } from '../api/format.js'
import PageHeading from '../components/PageHeading.jsx'
import Card from '../components/Card.jsx'
import Loading from '../components/Loading.jsx'
import Button from '../components/Button.jsx'
import Table from '../components/Table.jsx'
import TransactionTable from '../components/TransactionTable.jsx'
import Pagination from '../components/Pagination.jsx'
import ConfirmModal from '../components/ConfirmModal.jsx'

export default function AdminPage() {
  const [accounts, setAccounts] = useState(/** @type {import('../api/types.js').Page<import('../api/types.js').Account> | null} */ (null))
  const [activity, setActivity] = useState(/** @type {import('../api/types.js').Page<import('../api/types.js').Transaction> | null} */ (null))
  const [accountPage, setAccountPage] = useState(0)
  const [activityPage, setActivityPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [accountsLoading, setAccountsLoading] = useState(true)
  const [activityLoading, setActivityLoading] = useState(true)
  const [accountsError, setAccountsError] = useState('')
  const [activityError, setActivityError] = useState('')
  const [selected, setSelected] = useState(/** @type {import('../api/types.js').Account | null} */ (null))
  const [pending, setPending] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [message, setMessage] = useState('')
  const inFlight = useRef(false)

  useEffect(() => {
    let active = true
    setAccountsLoading(true); setAccountsError('')
    getAdminAccounts(accountPage).then(result => { if (active) setAccounts(result) })
      .catch(error_ => { if (active) setAccountsError(error_ instanceof Error ? error_.message : 'Accounts could not be loaded.') })
      .finally(() => { if (active) setAccountsLoading(false) })
    return () => { active = false }
  }, [accountPage, reload])

  useEffect(() => {
    let active = true
    setActivityLoading(true); setActivityError('')
    getAdminTransactions(activityPage).then(result => { if (active) setActivity(result) })
      .catch(error_ => { if (active) setActivityError(error_ instanceof Error ? error_.message : 'Activity could not be loaded.') })
      .finally(() => { if (active) setActivityLoading(false) })
    return () => { active = false }
  }, [activityPage, reload])

  async function changeStatus() {
    if (!selected || inFlight.current) return
    inFlight.current = true; setPending(true); setStatusError(''); setMessage('')
    const target = selected.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE'
    try {
      const result = await updateAccountStatus(selected.id, target)
      setMessage(`Account #${result.id} is ${result.status}.`)
      setSelected(null); setReload(previous => previous + 1)
    } catch (error_) { setStatusError(error_ instanceof Error ? error_.message : 'Status could not be confirmed. Refresh the accounts before retrying.') }
    finally { inFlight.current = false; setPending(false) }
  }

  let accountsContent = null
  if (accountsLoading) accountsContent = <Loading skeleton />
  else if (accountsError) accountsContent = <p role="alert" className="error">{accountsError}</p>
  else if (accounts) accountsContent = <>
        {accounts.items.length ? <Table caption="Credit account oversight" headers={['Account', 'Customer', 'Limit', 'Outstanding', 'Available', 'Status', 'Action']}>
          {accounts.items.map(account => <tr key={account.id}><td>#{account.id}</td><td>{account.ownerName}</td><td>{money(account.creditLimit)}</td><td>{money(account.outstandingBalance)}</td><td>{money(account.availableCredit)}</td><td>{account.status}</td><td><Button disabled={pending} onClick={() => { setSelected(account); setStatusError('') }}>{account.status === 'ACTIVE' ? 'Freeze' : 'Reactivate'} #{account.id}</Button></td></tr>)}
        </Table> : <p>No customer accounts are available.</p>}
        <Pagination page={accountPage} totalPages={accounts.totalPages} pending={pending} onPage={setAccountPage} />
      </>

  let activityContent = null
  if (activityLoading) activityContent = <Loading skeleton />
  else if (activityError) activityContent = <p role="alert" className="error">{activityError}</p>
  else if (activity) activityContent = <>
        {activity.items.length ? <TransactionTable items={activity.items} /> : <p>No transaction activity yet.</p>}
        <Pagination page={activityPage} totalPages={activity.totalPages} pending={pending} onPage={setActivityPage} />
      </>

  return <main id="main-content" className="content-page" tabIndex={-1}>
    <PageHeading eyebrow="Credit Circuit / Oversight" title="Administration" description="Review fictional customer accounts and activity. Freeze controls new spending; eligible refunds remain available." />
    {message && <output className="notice">{message}</output>}
    <Button onClick={() => setReload(reload + 1)} disabled={accountsLoading || activityLoading || pending}>Refresh accounts and activity</Button>
    <Card title="Customer accounts">
      {accountsContent}
    </Card>
    <Card title="Recent activity">
      {activityContent}
    </Card>
    <ConfirmModal open={selected !== null} title="Change account status" pending={pending} onConfirm={changeStatus} onClose={() => setSelected(null)}>
      <p>{selected?.status === 'ACTIVE' ? 'Freeze' : 'Reactivate'} account #{selected?.id} for {selected?.ownerName}?</p>
      {statusError && <p className="error" role="alert">{statusError}</p>}
    </ConfirmModal>
  </main>
}
