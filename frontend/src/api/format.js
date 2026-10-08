/** @param {number} amount */
export function money(amount) { return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount) }
/** @param {string | null} reason */
export function reasonLabel(reason) {
  if (reason === 'ACCOUNT_FROZEN') return 'The account is frozen.'
  if (reason === 'INSUFFICIENT_CREDIT') return 'There is not enough available credit.'
  if (reason === 'CARD_EXPIRED') return 'The fictional card has expired.'
  return ''
}
