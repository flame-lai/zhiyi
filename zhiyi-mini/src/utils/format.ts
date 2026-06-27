export function formatAmount(amount: number | string): string {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  return '¥' + (isNaN(num) ? '0.00' : num.toFixed(2))
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 16).replace('T', ' ')
}

export function formatPercent(rate: number): string {
  return (rate * 100).toFixed(1) + '%'
}
