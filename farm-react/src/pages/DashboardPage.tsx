import { useNavigate } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, Loading, PageTitle, StatCard, TypeBadge } from '../components/Common'
import { compactRub, date, n, rub } from '../utils/format'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { data, error, loading, reload } = useLoad(api.dashboard)

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} onRetry={reload} />

  const maxCat = Math.max(1, ...data.expenseStructure.map((c) => c.total))

  return (
    <>
      <PageTitle icon="bi-speedometer2">Сводка по хозяйствам</PageTitle>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-people" label="Фермеров" value={data.totalFarmers} onClick={() => navigate('/farmers')} />
        </div>
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-grid-3x3-gap" label="Участков" value={data.totalUnits} onClick={() => navigate('/units')} />
        </div>
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-map" label="Посевная площадь, га" value={n(data.cultivatedAreaHa)} />
        </div>
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-house-heart" label="Поголовье, гол." value={n(data.totalHeads)} />
        </div>
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-graph-up-arrow" label="Доходы за месяц" value={rub(data.monthIncome)} valueClass="text-income" />
        </div>
        <div className="col-6 col-lg-4 col-xxl-2">
          <StatCard icon="bi-graph-down-arrow" label="Расходы за месяц" value={rub(data.monthExpense)} valueClass="text-expense" />
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-xl-8">
          <div className="card page-card h-100">
            <div className="card-header"><i className="bi bi-bar-chart me-2"></i>Доходы и расходы за 6 месяцев</div>
            <div className="card-body" style={{ height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.monthly} barGap={2} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E4E8DE" />
                  <XAxis dataKey="monthName" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickFormatter={compactRub} tickLine={false} axisLine={false} fontSize={12} width={70} />
                  <Tooltip formatter={(v: number) => rub(v)} cursor={{ fill: 'rgba(79,122,58,0.06)' }} />
                  <Legend />
                  <Bar dataKey="income" name="Доходы" fill="#3B8F4F" radius={[4, 4, 0, 0]} maxBarSize={28} isAnimationActive={false} />
                  <Bar dataKey="expense" name="Расходы" fill="#B5652E" radius={[4, 4, 0, 0]} maxBarSize={28} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
        <div className="col-xl-4">
          <div className="card page-card h-100">
            <div className="card-header"><i className="bi bi-pie-chart me-2"></i>Структура расходов за месяц</div>
            <div className="card-body">
              {data.expenseStructure.length === 0 && <div className="empty-state">Расходов в этом месяце нет</div>}
              {data.expenseStructure.map((c) => (
                <div key={c.name} className="mb-2">
                  <div className="d-flex justify-content-between small">
                    <span>{c.name}</span><span className="num fw-semibold">{rub(c.total)}</span>
                  </div>
                  <div className="progress">
                    <div className="progress-bar" style={{ width: `${(c.total / maxCat) * 100}%`, background: '#B5652E' }}></div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="card page-card mb-4">
        <div className="card-header"><i className="bi bi-calendar3 me-2"></i>Финансовый результат по месяцам</div>
        <div className="card-body p-0 table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr><th>Месяц</th><th className="text-end">Доходы</th><th className="text-end">Расходы</th><th className="text-end">Результат</th></tr>
            </thead>
            <tbody>
              {data.monthly.map((m) => (
                <tr key={m.monthName}>
                  <td>{m.monthName}</td>
                  <td className="text-end num text-income">{rub(m.income)}</td>
                  <td className="text-end num text-expense">{rub(m.expense)}</td>
                  <td className={`text-end num fw-semibold ${m.profit >= 0 ? 'text-income' : 'text-danger'}`}>{rub(m.profit)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card page-card">
        <div className="card-header"><i className="bi bi-clock-history me-2"></i>Последние операции</div>
        <div className="card-body p-0 table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr><th>Дата</th><th>Фермер</th><th>Участок</th><th>Статья</th><th>Тип</th><th className="text-end">Сумма</th></tr>
            </thead>
            <tbody>
              {data.recentOperations.map((o) => (
                <tr key={o.id}>
                  <td className="small text-muted">{date(o.operationDate)}</td>
                  <td className="small">{o.userName}</td>
                  <td className="small">{o.unitName}</td>
                  <td className="small">{o.categoryName}</td>
                  <td><TypeBadge type={o.type} /></td>
                  <td className={`text-end num fw-semibold ${o.type === 'income' ? 'text-income' : 'text-expense'}`}>
                    {o.type === 'income' ? '+' : '−'}{rub(o.amount)}
                  </td>
                </tr>
              ))}
              {data.recentOperations.length === 0 && <tr><td colSpan={6} className="empty-state">Операций пока нет</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
