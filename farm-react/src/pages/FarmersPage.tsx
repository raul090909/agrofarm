import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, Loading, PageTitle } from '../components/Common'
import { n, rub } from '../utils/format'

type SortKey = 'fullName' | 'unitCount' | 'areaHa' | 'monthProfit'

export default function FarmersPage() {
  const navigate = useNavigate()
  const { data, error, loading, reload, setData } = useLoad(api.farmers)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<SortKey>('fullName')
  const [actionError, setActionError] = useState('')

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    const list = (data ?? []).filter((f) => !q || f.fullName.toLowerCase().includes(q) || f.email.toLowerCase().includes(q))
    return [...list].sort((a, b) => sort === 'fullName'
      ? a.fullName.localeCompare(b.fullName, 'ru')
      : (b[sort] as number) - (a[sort] as number))
  }, [data, query, sort])

  // Оптимистичное обновление: переключатель меняется сразу, при ошибке сервера возвращается обратно
  const toggleActive = async (id: number, active: boolean) => {
    setActionError('')
    setData((prev) => prev && prev.map((f) => (f.id === id ? { ...f, active } : f)))
    try {
      await api.setFarmerActive(id, active)
    } catch (e) {
      setData((prev) => prev && prev.map((f) => (f.id === id ? { ...f, active: !active } : f)))
      setActionError((e as Error).message)
    }
  }

  if (loading) return <Loading />
  if (error) return <ErrorAlert message={error} onRetry={reload} />

  return (
    <>
      <PageTitle icon="bi-people">Фермеры</PageTitle>
      {actionError && <ErrorAlert message={actionError} />}

      <div className="card page-card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <div className="input-group" style={{ maxWidth: 320 }}>
            <span className="input-group-text bg-white"><i className="bi bi-search"></i></span>
            <input className="form-control" placeholder="Поиск по ФИО или email" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
          <select className="form-select" style={{ maxWidth: 240 }} value={sort} onChange={(e) => setSort(e.target.value as SortKey)}>
            <option value="fullName">Сортировка: по ФИО</option>
            <option value="unitCount">По числу участков</option>
            <option value="areaHa">По площади</option>
            <option value="monthProfit">По результату месяца</option>
          </select>
          <span className="ms-auto text-muted small fw-normal">Найдено: {rows.length}</span>
        </div>
        <div className="card-body p-0 table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Фермер</th><th className="text-center">Участков</th><th className="text-end">Площадь, га</th>
                <th className="text-end">Поголовье</th><th className="text-center">Операций за мес.</th>
                <th className="text-end">Результат месяца</th><th className="text-center">Доступ</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((f) => (
                <tr key={f.id} className="clickable" onClick={() => navigate(`/farmers/${f.id}`)}>
                  <td>
                    <div className="fw-semibold">{f.fullName}</div>
                    <div className="small text-muted">{f.email}</div>
                  </td>
                  <td className="text-center">{f.unitCount}</td>
                  <td className="text-end num">{n(f.areaHa)}</td>
                  <td className="text-end num">{n(f.heads)}</td>
                  <td className="text-center">{f.opsThisMonth}</td>
                  <td className={`text-end num fw-semibold ${f.monthProfit >= 0 ? 'text-income' : 'text-danger'}`}>{rub(f.monthProfit)}</td>
                  <td className="text-center" onClick={(e) => e.stopPropagation()}>
                    <div className="form-check form-switch d-inline-block m-0" title={f.active ? 'Заблокировать' : 'Разблокировать'}>
                      <input className="form-check-input" type="checkbox" checked={f.active}
                             onChange={(e) => toggleActive(f.id, e.target.checked)} />
                    </div>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan={7} className="empty-state">Никого не найдено</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
