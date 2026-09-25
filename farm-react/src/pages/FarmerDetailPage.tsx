import { Link, useNavigate, useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, LimitStatus, Loading, PageTitle, StatCard, TypeBadge } from '../components/Common'
import { compactRub, date, n, rub, UNIT_ICONS } from '../utils/format'

export default function FarmerDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const farmerId = Number(id)
  const { data, error, loading, reload } = useLoad(() => api.farmer(farmerId), [farmerId])

  if (!Number.isInteger(farmerId) || farmerId <= 0) return <ErrorAlert message="Некорректный идентификатор фермера" />
  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} onRetry={reload} />

  const half = data.monthly.reduce((a, m) => ({ inc: a.inc + m.income, exp: a.exp + m.expense }), { inc: 0, exp: 0 })

  return (
    <>
      <PageTitle icon="bi-person-badge" right={
        <div className="d-flex gap-2">
          <Link to="/farmers" className="btn btn-outline-secondary btn-sm"><i className="bi bi-arrow-left me-1"></i>К списку</Link>
          <button className="btn btn-primary btn-sm" onClick={() => navigate(`/recommendations?user=${data.id}`)}>
            <i className="bi bi-send me-1"></i>Дать рекомендацию
          </button>
        </div>
      }>
        {data.fullName}
      </PageTitle>
      <div className="text-muted small mb-3">{data.email} · в системе с {date(data.createdAt)}</div>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3"><StatCard icon="bi-grid-3x3-gap" label="Участков" value={data.units.length} /></div>
        <div className="col-6 col-lg-3"><StatCard icon="bi-map" label="Площадь, га" value={n(data.areaHa)} /></div>
        <div className="col-6 col-lg-3"><StatCard icon="bi-house-heart" label="Поголовье" value={n(data.heads)} /></div>
        <div className="col-6 col-lg-3">
          <StatCard icon="bi-cash-stack" label="Результат за 6 мес." value={rub(half.inc - half.exp)}
                    valueClass={half.inc - half.exp >= 0 ? 'text-income' : 'text-danger'} />
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-xl-7">
          <div className="card page-card h-100">
            <div className="card-header"><i className="bi bi-bar-chart me-2"></i>Доходы и расходы по месяцам</div>
            <div className="card-body" style={{ height: 290 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.monthly}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E4E8DE" />
                  <XAxis dataKey="monthName" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickFormatter={compactRub} tickLine={false} axisLine={false} fontSize={12} width={70} />
                  <Tooltip formatter={(v: number) => rub(v)} />
                  <Legend />
                  <Bar dataKey="income" name="Доходы" fill="#3B8F4F" radius={[4, 4, 0, 0]} maxBarSize={26} isAnimationActive={false} />
                  <Bar dataKey="expense" name="Расходы" fill="#B5652E" radius={[4, 4, 0, 0]} maxBarSize={26} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
        <div className="col-xl-5">
          <div className="card page-card h-100">
            <div className="card-header"><i className="bi bi-grid-3x3-gap me-2"></i>Участки</div>
            <ul className="list-group list-group-flush">
              {data.units.map((u) => (
                <li key={u.id} className="list-group-item d-flex align-items-center gap-3">
                  <i className={`bi ${UNIT_ICONS[u.type]} fs-5`} style={{ color: 'var(--primary)' }}></i>
                  <div className="flex-grow-1">
                    <div className="fw-semibold">{u.name}</div>
                    <div className="small text-muted">{u.typeLabel}{u.description ? ` · ${u.description}` : ''}</div>
                  </div>
                  <div className="text-end small num">
                    {u.areaHa > 0 && <div>{n(u.areaHa)} га</div>}
                    {u.headCount > 0 && <div>{n(u.headCount)} гол.</div>}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      <div className="card page-card mb-4">
        <div className="card-header"><i className="bi bi-speedometer me-2"></i>Лимиты расходов</div>
        <div className="card-body p-0 table-responsive">
          <table className="table align-middle mb-0">
            <thead className="table-light">
              <tr><th>Статья</th><th>Период</th><th className="text-end">Лимит</th><th className="text-end">Израсходовано</th><th style={{ width: 200 }}>Использование</th><th>Статус</th></tr>
            </thead>
            <tbody>
              {data.limits.map((l) => (
                <tr key={l.id}>
                  <td>{l.categoryName}</td>
                  <td>{l.periodLabel}</td>
                  <td className="text-end num">{rub(l.amount)}</td>
                  <td className="text-end num">{rub(l.spent)}</td>
                  <td>
                    <div className="progress">
                      <div className={`progress-bar ${l.status === 'exceeded' ? 'bg-danger' : l.status === 'warning' ? 'bg-warning' : 'bg-success'}`}
                           style={{ width: `${Math.min(100, l.percent)}%` }}></div>
                    </div>
                    <div className="small text-muted">{n(l.percent)} %</div>
                  </td>
                  <td><LimitStatus status={l.status} /></td>
                </tr>
              ))}
              {data.limits.length === 0 && <tr><td colSpan={6} className="empty-state">Лимиты не установлены</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card page-card">
        <div className="card-header"><i className="bi bi-list-ul me-2"></i>Последние операции</div>
        <div className="card-body p-0 table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr><th>Дата</th><th>Участок</th><th>Статья</th><th>Тип</th><th className="text-end">Количество</th><th className="text-end">Сумма</th><th>Описание</th></tr>
            </thead>
            <tbody>
              {data.recentOperations.map((o) => (
                <tr key={o.id}>
                  <td className="small text-muted">{date(o.operationDate)}</td>
                  <td className="small">{o.unitName}</td>
                  <td className="small">{o.categoryName}</td>
                  <td><TypeBadge type={o.type} /></td>
                  <td className="text-end small num">{o.quantity != null ? `${n(o.quantity)} ${o.quantityUnit}` : '—'}</td>
                  <td className={`text-end num fw-semibold ${o.type === 'income' ? 'text-income' : 'text-expense'}`}>{rub(o.amount)}</td>
                  <td className="small text-muted">{o.description ?? ''}</td>
                </tr>
              ))}
              {data.recentOperations.length === 0 && <tr><td colSpan={7} className="empty-state">Операций нет</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
