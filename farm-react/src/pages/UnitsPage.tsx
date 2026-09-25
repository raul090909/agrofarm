import { Fragment, useMemo, useState } from 'react'
import { api, Operation } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, Loading, PageTitle, TypeBadge } from '../components/Common'
import { date, n, rub, UNIT_ICONS } from '../utils/format'

const TYPES = [
  { v: '', label: 'Все типы' },
  { v: 'field', label: 'Поля' },
  { v: 'livestock', label: 'Животноводство' },
  { v: 'greenhouse', label: 'Теплицы' },
  { v: 'storage', label: 'Склады' },
]

export default function UnitsPage() {
  const { data, error, loading, reload } = useLoad(api.units)
  const [type, setType] = useState('')
  const [open, setOpen] = useState<number | null>(null)
  const [ops, setOps] = useState<Record<number, Operation[]>>({})
  const [opsError, setOpsError] = useState('')

  const rows = useMemo(() => (data ?? []).filter((u) => !type || u.type === type), [data, type])
  const totals = rows.reduce((a, u) => ({ inc: a.inc + u.income, exp: a.exp + u.expense }), { inc: 0, exp: 0 })

  const toggle = async (id: number) => {
    if (open === id) { setOpen(null); return }
    setOpen(id)
    setOpsError('')
    if (!ops[id]) {
      try {
        const list = await api.unitOperations(id)
        setOps((p) => ({ ...p, [id]: list }))
      } catch (e) {
        setOpsError((e as Error).message)
      }
    }
  }

  if (loading) return <Loading />
  if (error) return <ErrorAlert message={error} onRetry={reload} />

  return (
    <>
      <PageTitle icon="bi-grid-3x3-gap">Участки хозяйств</PageTitle>
      <div className="card page-card">
        <div className="card-header d-flex flex-wrap align-items-center gap-2">
          <select className="form-select" style={{ maxWidth: 220 }} value={type} onChange={(e) => setType(e.target.value)}>
            {TYPES.map((t) => <option key={t.v} value={t.v}>{t.label}</option>)}
          </select>
          <span className="text-muted small fw-normal">Результат за последние 3 месяца. Нажмите на строку, чтобы увидеть операции.</span>
        </div>
        <div className="card-body p-0 table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Участок</th><th>Владелец</th><th className="text-end">Площадь / поголовье</th>
                <th className="text-end">Доходы</th><th className="text-end">Расходы</th><th className="text-end">Результат</th>
                <th className="text-end">Затраты на га / гол.</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <Fragment key={u.id}>
                  <tr className="clickable" onClick={() => toggle(u.id)}>
                    <td>
                      <i className={`bi ${open === u.id ? 'bi-chevron-down' : 'bi-chevron-right'} me-2 text-muted small`}></i>
                      <i className={`bi ${UNIT_ICONS[u.type]} me-2`} style={{ color: 'var(--primary)' }}></i>
                      <span className="fw-semibold">{u.name}</span>
                      <div className="small text-muted ms-5">{u.typeLabel}</div>
                    </td>
                    <td className="small">{u.userName}</td>
                    <td className="text-end num small">
                      {u.areaHa > 0 ? `${n(u.areaHa)} га` : ''}{u.areaHa > 0 && u.headCount > 0 ? ' · ' : ''}
                      {u.headCount > 0 ? `${n(u.headCount)} гол.` : ''}
                    </td>
                    <td className="text-end num text-income">{rub(u.income)}</td>
                    <td className="text-end num text-expense">{rub(u.expense)}</td>
                    <td className={`text-end num fw-semibold ${u.profit >= 0 ? 'text-income' : 'text-danger'}`}>{rub(u.profit)}</td>
                    <td className="text-end num small">
                      {u.costPerHa != null && <div>{rub(u.costPerHa)}/га</div>}
                      {u.costPerHead != null && <div>{rub(u.costPerHead)}/гол.</div>}
                      {u.costPerHa == null && u.costPerHead == null && '—'}
                    </td>
                  </tr>
                  {open === u.id && (
                    <tr>
                      <td colSpan={7} className="bg-light">
                        {opsError && <ErrorAlert message={opsError} />}
                        {!ops[u.id] && !opsError && <Loading />}
                        {ops[u.id] && (
                          <table className="table table-sm mb-0 bg-white">
                            <thead><tr><th>Дата</th><th>Статья</th><th>Тип</th><th className="text-end">Кол-во</th><th className="text-end">Сумма</th><th>Описание</th></tr></thead>
                            <tbody>
                              {ops[u.id].map((o) => (
                                <tr key={o.id}>
                                  <td className="small">{date(o.operationDate)}</td>
                                  <td className="small">{o.categoryName}</td>
                                  <td><TypeBadge type={o.type} /></td>
                                  <td className="text-end small num">{o.quantity != null ? `${n(o.quantity)} ${o.quantityUnit}` : '—'}</td>
                                  <td className="text-end small num">{rub(o.amount)}</td>
                                  <td className="small text-muted">{o.description}</td>
                                </tr>
                              ))}
                              {ops[u.id].length === 0 && <tr><td colSpan={6} className="empty-state">Операций нет</td></tr>}
                            </tbody>
                          </table>
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {rows.length === 0 && <tr><td colSpan={7} className="empty-state">Участков нет</td></tr>}
            </tbody>
            {rows.length > 0 && (
              <tfoot className="table-light fw-semibold">
                <tr>
                  <td colSpan={3}>Итого по {rows.length} участкам</td>
                  <td className="text-end num text-income">{rub(totals.inc)}</td>
                  <td className="text-end num text-expense">{rub(totals.exp)}</td>
                  <td className={`text-end num ${totals.inc - totals.exp >= 0 ? 'text-income' : 'text-danger'}`}>{rub(totals.inc - totals.exp)}</td>
                  <td></td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>
    </>
  )
}
