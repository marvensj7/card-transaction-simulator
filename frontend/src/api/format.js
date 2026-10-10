/** @param {number} amount */
export function money(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
}

/** @param {string | null} reasonCode */
export function reasonLabel(reasonCode) {
  if (reasonCode === 'ACCOUNT_FROZEN') {
    return 'The account is frozen.'
  } else if (reasonCode === 'INSUFFICIENT_CREDIT') {
    return 'There is not enough available credit.'
  } else if (reasonCode === 'CARD_EXPIRED') {
    return 'The card has expired.'
  } else {
    return ''
  }
}
