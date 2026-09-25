import { useState } from 'react'
import { downloadExport } from '../api/api'
import { PageTitle } from '../components/Common'

const iso = (d: Date) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)

export default function ExportPage() {
  const today = new Date()
  const [from, setFrom] = useState(iso(new Date(today.getFullYear(), today.getMonth(), 1)))
  const [to, setTo] = useState(iso(today))
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState<'' | 'csv' | 'excel'>('')

  const preset = (months: number) => {
    const start = new Date(today.getFullYear(), today.getMonth() - months + 1, 1)
    setFrom(iso(start))
    setTo(iso(today))
  }

  const download = async (format: 'csv' | 'excel') => {
    setErr('')
    if (!from || !to) { setErr('Укажите обе даты'); return }
    if (from > to) { setErr('Дата начала не может быть позже даты окончания'); return }
    setBusy(format)
    try {
      await downloadExport(format, from, to)
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setBusy('')
    }
  }

  return (
    <>
      <PageTitle icon="bi-download">Выгрузка отчётов</PageTitle>
      <div className="row justify-content-center">
        <div className="col-lg-7 col-xl-6">
          <div className="card page-card">
            <div className="card-body p-4">
              <p className="text-muted">Выберите период и формат — в файл попадут все хозяйственные операции фермеров за этот период с итогами.</p>
              {err && <div className="alert alert-danger py-2 small"><i className="bi bi-exclamation-circle me-1"></i>{err}</div>}
              <div className="d-flex gap-2 mb-3 flex-wrap">
                <button className="btn btn-sm btn-outline-secondary" onClick={() => preset(1)}>Текущий месяц</button>
                <button className="btn btn-sm btn-outline-secondary" onClick={() => preset(3)}>3 месяца</button>
                <button className="btn btn-sm btn-outline-secondary" onClick={() => preset(12)}>12 месяцев</button>
              </div>
              <div className="row g-3 mb-4">
                <div className="col-sm-6">
                  <label className="form-label fw-semibold">Дата начала</label>
                  <input type="date" className="form-control" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
                </div>
                <div className="col-sm-6">
                  <label className="form-label fw-semibold">Дата окончания</label>
                  <input type="date" className="form-control" value={to} min={from} onChange={(e) => setTo(e.target.value)} />
                </div>
              </div>
              <div className="d-grid gap-2">
                <button className="btn btn-outline-primary btn-lg" onClick={() => download('csv')} disabled={!!busy}>
                  {busy === 'csv' ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-filetype-csv me-2"></i>}
                  Скачать CSV
                </button>
                <button className="btn btn-primary btn-lg" onClick={() => download('excel')} disabled={!!busy}>
                  {busy === 'excel' ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-file-earmark-excel me-2"></i>}
                  Скачать Excel (.xlsx)
                </button>
              </div>
            </div>
          </div>
          <div className="card page-card mt-3">
            <div className="card-body">
              <h6 className="fw-semibold"><i className="bi bi-info-circle me-2" style={{ color: 'var(--primary)' }}></i>Состав отчёта</h6>
              <div className="small text-muted">
                Номер и дата операции, фермер, участок, тип (доход или расход), статья, сумма, количество
                с единицей измерения и описание. В Excel-файле в конце добавлены итоги: доходы, расходы и финансовый результат.
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
