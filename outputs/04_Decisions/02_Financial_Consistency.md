# Decision: one locked account transaction per financial action

I kept BigDecimal, account write locks, READ_COMMITTED, and @Transactional around purchases/refunds. Balance/history must succeed together. Locks serialize competing changes. READ_COMMITTED lets waiting retries see committed results.

An account/request UUID identifies a submission and has a unique constraint. Identical retries return 200 with the saved transaction/current summary. New approvals and declines return 201. The interface retains the UUID/input during an uncertain response.

A full refund copies the original owned approved purchase's amount and links one new row. Unique original_purchase_id prevents two refunds. The original stays in history. A frozen account can receive an eligible refund. No cascade deletion or additional ledger tables are needed.

Real MySQL tests cover concurrency, waiting retries, and purchase/refund/registration rollback. Browser checks deliberately lose responses after real saves and confirm identical retries.
