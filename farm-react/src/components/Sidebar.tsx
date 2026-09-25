import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const links = [
  { to: '/dashboard', icon: 'bi-speedometer2', label: 'Сводка' },
  { to: '/farmers', icon: 'bi-people', label: 'Фермеры' },
  { to: '/units', icon: 'bi-grid-3x3-gap', label: 'Участки' },
  { to: '/anomalies', icon: 'bi-exclamation-triangle', label: 'Аномалии' },
  { to: '/recommendations', icon: 'bi-lightbulb', label: 'Рекомендации' },
  { to: '/reports', icon: 'bi-download', label: 'Выгрузка отчётов' },
]

export default function Sidebar() {
  const { name, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <aside className="sidebar">
      <div className="sidebar-inner">
      <div className="brand">
        <i className="bi bi-flower3 fs-4" style={{ color: '#A0C478' }}></i>
        <div>
          Сельхозферма
          <small>Панель агронома</small>
        </div>
      </div>

      <nav className="px-2 py-3 flex-grow-1">
        <div className="nav-caption">Навигация</div>
        <ul className="nav flex-column gap-1">
          {links.map((l) => (
            <li key={l.to} className="nav-item">
              <NavLink to={l.to} className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
                <i className={`bi ${l.icon}`}></i>
                {l.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="px-3 py-3" style={{ borderTop: '1px solid rgba(243,246,236,0.12)' }}>
        <div className="d-flex align-items-center gap-2 mb-2">
          <div className="rounded-circle d-flex align-items-center justify-content-center"
               style={{ width: 32, height: 32, background: 'rgba(160,196,120,0.3)', flexShrink: 0 }}>
            <i className="bi bi-person" style={{ color: '#F3F6EC' }}></i>
          </div>
          <span style={{ color: '#F3F6EC', fontSize: '0.85rem', fontWeight: 600 }}>{name || 'Агроном'}</span>
        </div>
        <button className="btn btn-sm w-100"
                style={{ border: '1px solid rgba(243,246,236,0.25)', color: 'rgba(243,246,236,0.8)' }}
                onClick={async () => { await logout(); navigate('/login') }}>
          <i className="bi bi-box-arrow-left me-1"></i>Выйти
        </button>
      </div>
      </div>
    </aside>
  )
}
