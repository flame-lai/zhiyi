export function formatAmount(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined || amount === '') return '¥0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  return '¥' + (isNaN(num) ? 0 : num).toFixed(2)
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 16).replace('T', ' ')
}

export function formatPercent(rate: number | string | null | undefined): string {
  if (rate === null || rate === undefined || rate === '') return '0.0%'
  const num = typeof rate === 'string' ? parseFloat(rate) : rate
  return (isNaN(num) ? 0 : num * 100).toFixed(1) + '%'
}
