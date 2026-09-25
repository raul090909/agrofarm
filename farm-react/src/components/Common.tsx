import { ReactNode } from 'react'

export function Loading() {
  return (
    <div className="d-flex justify-content-center align-items-center py-5 text-muted gap-2">
      <div className="spinner-border spinner-border-sm" role="status"></div> Загрузка…
    </div>
  )
}

export function ErrorAlert({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="alert alert-danger d-flex align-items-center gap-2">
      <i className="bi bi-exclamation-octagon"></i>
      <span className="flex-grow-1">{message}</span>
      {onRetry && <button className="btn btn-sm btn-outline-danger" onClick={onRetry}>Повторить</button>}
    </div>
  )
}

export function PageTitle({ icon, children, right }: { icon: string; children: ReactNode; right?: ReactNode }) {
  return (
    <div className="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
      <h4 className="page-title mb-0"><i className={`bi ${icon}`}></i>{children}</h4>
      {right}
    </div>
  )
}

export function StatCard({ icon, label, value, valueClass = '', onClick }:
  { icon: string; label: string; value: ReactNode; valueClass?: string; onClick?: () => void }) {
  return (
    <div className={`card stat-card h-100 ${onClick ? 'clickable' : ''}`} onClick={onClick}>
      <div className="card-body py-3 px-3">
        <i className={`bi ${icon} stat-icon`}></i>
        <div className={`stat-value mt-1 ${valueClass}`}>{value}</div>
        <div className="stat-label">{label}</div>
      </div>
    </div>
  )
}

export function TypeBadge({ type }: { type: 'income' | 'expense' }) {
  return <span className={`badge ${type === 'income' ? 'badge-income' : 'badge-expense'}`}>
    {type === 'income' ? 'Доход' : 'Расход'}
  </span>
}

export function LimitStatus({ status }: { status: 'ok' | 'warning' | 'exceeded' }) {
  const map = {
    ok: ['bg-success', 'В норме'],
    warning: ['bg-warning text-dark', 'Почти исчерпан'],
    exceeded: ['bg-danger', 'Превышен'],
  } as const
  const [cls, label] = map[status]
  return <span className={`badge ${cls}`}>{label}</span>
}
