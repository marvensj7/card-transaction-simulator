/** @typedef {{id: number, ownerName: string, creditLimit: number, outstandingBalance: number, availableCredit: number, status: 'ACTIVE' | 'FROZEN'}} Account */
/** @typedef {{id: number, label: string, maskedNumber: string, expiryMonth: number, expiryYear: number}} DemoCard */
/** @typedef {{id: number, accountId: number, cardId: number, type: 'PURCHASE' | 'REFUND', status: 'APPROVED' | 'DECLINED', amount: number, outstandingAfter: number, merchantName: string, reasonCode: string | null, createdAt: string, originalPurchaseId: number | null, refunded: boolean}} Transaction */
/** @template T @typedef {{items: T[], page: number, size: number, totalElements: number, totalPages: number}} Page */
/** @typedef {{transaction: Transaction, account: Account}} TransactionResult */
export {}
