const money = new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
const num = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 })

export const rub = (v: number | null | undefined) => (v == null ? '—' : `${money.format(v)} ₽`)
export const n = (v: number | null | undefined) => (v == null ? '—' : num.format(v))

/** Короткая запись крупных сумм для осей графиков: 1,2 млн, 350 тыс. */
export const compactRub = (v: number) => {
  const a = Math.abs(v)
  if (a >= 1_000_000) return `${num.format(+(v / 1_000_000).toFixed(1))} млн`
  if (a >= 1_000) return `${num.format(Math.round(v / 1_000))} тыс`
  return num.format(v)
}

export const date = (iso: string | null | undefined) => {
  if (!iso) return '—'
  const d = new Date(iso.length === 10 ? iso + 'T00:00:00' : iso)
  return d.toLocaleDateString('ru-RU')
}

export const dateTime = (iso: string | null | undefined) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export const UNIT_ICONS: Record<string, string> = {
  field: 'bi-flower1',
  livestock: 'bi-house-heart',
  greenhouse: 'bi-brightness-high',
  storage: 'bi-box-seam',
}

export const TOPICS: Record<string, { label: string; icon: string }> = {
  general: { label: 'Общее', icon: 'bi-chat-left-text' },
  crops: { label: 'Растениеводство', icon: 'bi-flower2' },
  livestock: { label: 'Животноводство', icon: 'bi-egg-fried' },
  finance: { label: 'Финансы', icon: 'bi-cash-coin' },
}
