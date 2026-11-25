function validDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return ''
  const date = new Date(value + 'T00:00:00Z')
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value ? value : ''
}

/** Restore filters from a link or from browser history. */
export function readListFilters(query, statuses) {
  const status = typeof query.status === 'string' && statuses.includes(query.status) ? query.status : ''
  const createdDate = validDate(query.createdDate)
  const from = validDate(query.from) || createdDate
  const to = validDate(query.to) || createdDate
  if (from && to && from > to) return { status, createdDate, from: '', to: '' }
  return { status, createdDate, from, to }
}

export function readText(value, limit) {
  return typeof value === 'string' ? value.trim().slice(0, limit) : ''
}

export function readPage(value) {
  const page = Number(value)
  return Number.isSafeInteger(page) && page > 0 && page <= 2147483647 ? page : 1
}

export function listQuery(filters, page, size, defaultSize = 10) {
  const result = Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
  if (page > 1) result.page = String(page)
  if (size !== defaultSize) result.size = String(size)
  return result
}
