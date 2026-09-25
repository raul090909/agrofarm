import { useNavigate } from 'react-router-dom'
import { api } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, LimitStatus, Loading, PageTitle } from '../components/Common'
import { n, rub } from '../utils/format'

export default function AnomaliesPage() {
  const navigate = useNavigate()
  const { data, error, loading, reload } = useLoad(api.anomalies)

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} onRetry={reload} />

  const advise = (userId: number, text: string, topic = 'finance') =>
    navigate(`/recommendations?user=${userId}&topic=${topic}&text=${encodeURIComponent(text)}`)
  const total = data.limitAnomalies.length + data.expenseGrowth.length + data.unprofitableUnits.length

  return (
    <>
      <PageTitle icon="bi-exclamation-triangle" right={
        <span className={`badge fs-6 ${total ? 'bg-danger' : 'bg-success'}`}>
          {total ? `Найдено: ${total}` : 'Аномалий нет'}
        </span>
      }>Аномалии</PageTitle>

      <div className="card page-card mb-4">
        <div className="card-header"><i className="bi bi-speedometer me-2"></i>Лимиты расходов, использованные на 80 % и более</div>
        <div className="card-body p-0 table-responsive">
          <table className="table align-middle mb-0">
            <thead className="table-light">
              <tr><th>Фермер</th><th>Статья</th><th>Период</th><th className="text-end">Лимит</th><th className="text-end">Израсходовано</th><th className="text-end">%</th><th>Статус</th><th></th></tr>
            </thead>
            <tbody>
              {data.limitAnomalies.map((l) => (
                <tr key={l.id}>
                  <td className="fw-semibold">{l.userName}</td>
                  <td>{l.categoryName}</td>
                  <td className="nowrap">{l.periodLabel}</td>
                  <td className="text-end num">{rub(l.amount)}</td>
                  <td className="text-end num">{rub(l.spent)}</td>
                  <td className="text-end num fw-semibold">{n(l.percent)}</td>
                  <td><LimitStatus status={l.status} /></td>
                  <td className="text-end">
                    <button className="btn btn-sm btn-outline-primary"
                            onClick={() => advise(l.userId, `Расходы по статье «${l.categoryName}» достигли ${n(l.percent)} % лимита. Рекомендую пересмотреть план закупок.`)}>
                      <i className="bi bi-send"></i>
                    </button>
                  </td>
                </tr>
              ))}
              {data.limitAnomalies.length === 0 && <tr><td colSpan={8} className="empty-state">Все лимиты в норме</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card page-card mb-4">
        <div className="card-header"><i className="bi bi-graph-up-arrow me-2"></i>Резкий рост расходов (более 50 % к прошлому месяцу)</div>
        <div className="card-body p-0 table-responsive">
          <table className="table align-middle mb-0">
            <thead className="table-light">
              <tr><th>Фермер</th><th className="text-end">Прошлый месяц</th><th className="text-end">Текущий месяц</th><th className="text-end">Рост</th><th></th></tr>
            </thead>
            <tbody>
              {data.expenseGrowth.map((g) => (
                <tr key={g.userId}>
                  <td className="fw-semibold">{g.userName}</td>
                  <td className="text-end num">{rub(g.lastMonth)}</td>
                  <td className="text-end num">{rub(g.thisMonth)}</td>
                  <td className="text-end num text-danger fw-semibold">+{n(g.growthPercent)} %</td>
                  <td className="text-end">
                    <button className="btn btn-sm btn-outline-primary"
                            onClick={() => advise(g.userId, `Расходы в этом месяце выросли на ${n(g.growthPercent)} % по сравнению с прошлым. Проверьте, не связано ли это с разовыми закупками.`)}>
                      <i className="bi bi-send"></i>
                    </button>
                  </td>
                </tr>
              ))}
              {data.expenseGrowth.length === 0 && <tr><td colSpan={5} className="empty-state">Резкого роста расходов не обнаружено</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card page-card">
        <div className="card-header"><i className="bi bi-arrow-down-right-circle me-2"></i>Убыточные участки (последние 3 месяца)</div>
        <div className="card-body p-0 table-responsive">
          <table className="table align-middle mb-0">
            <thead className="table-light">
              <tr><th>Участок</th><th>Владелец</th><th className="text-end">Доходы</th><th className="text-end">Расходы</th><th className="text-end">Убыток</th><th></th></tr>
            </thead>
            <tbody>
              {data.unprofitableUnits.map((u) => (
                <tr key={u.id}>
                  <td className="fw-semibold">{u.name} <span className="small text-muted fw-normal">· {u.typeLabel}</span></td>
                  <td>{u.userName}</td>
                  <td className="text-end num text-income">{rub(u.income)}</td>
                  <td className="text-end num text-expense">{rub(u.expense)}</td>
                  <td className="text-end num text-danger fw-semibold">{rub(u.profit)}</td>
                  <td className="text-end">
                    <button className="btn btn-sm btn-outline-primary"
                            onClick={() => advise(u.userId, `Участок «${u.name}» за последние 3 месяца убыточен (${rub(u.profit)}). Предлагаю обсудить структуру затрат.`, u.type === 'livestock' ? 'livestock' : 'crops')}>
                      <i className="bi bi-send"></i>
                    </button>
                  </td>
                </tr>
              ))}
              {data.unprofitableUnits.length === 0 && <tr><td colSpan={6} className="empty-state">Убыточных участков нет</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
