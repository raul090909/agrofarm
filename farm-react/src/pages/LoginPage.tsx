import { FormEvent, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login, loggedIn } = useAuth()
  const navigate = useNavigate()
  const [loginValue, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [showPwd, setShowPwd] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (loggedIn) return <Navigate to="/dashboard" replace />

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!loginValue.trim() || !password) {
      setError('Введите логин и пароль')
      return
    }
    setBusy(true)
    try {
      await login(loginValue.trim(), password)
      navigate('/dashboard')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <div className="card login-card">
        <div className="card-body p-4 p-md-5">
          <div className="text-center mb-4">
            <div className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
                 style={{ width: 64, height: 64, background: 'rgba(79,122,58,0.12)' }}>
              <i className="bi bi-flower3 fs-2" style={{ color: 'var(--primary)' }}></i>
            </div>
            <h4 className="fw-bold mb-1">Сельхозферма</h4>
            <div className="text-muted small">Вход в панель агронома</div>
          </div>

          {error && (
            <div className="alert alert-danger py-2 small" role="alert">
              <i className="bi bi-exclamation-circle me-1"></i>{error}
            </div>
          )}

          <form onSubmit={submit} noValidate>
            <div className="mb-3">
              <label className="form-label fw-semibold" htmlFor="login">Логин</label>
              <div className="input-group">
                <span className="input-group-text"><i className="bi bi-person"></i></span>
                <input id="login" className="form-control" autoComplete="username" autoFocus
                       value={loginValue} onChange={(e) => setLogin(e.target.value)} placeholder="agronom" />
              </div>
            </div>
            <div className="mb-4">
              <label className="form-label fw-semibold" htmlFor="password">Пароль</label>
              <div className="input-group">
                <span className="input-group-text"><i className="bi bi-lock"></i></span>
                <input id="password" className="form-control" type={showPwd ? 'text' : 'password'}
                       autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
                <button type="button" className="btn btn-outline-secondary" onClick={() => setShowPwd(!showPwd)}
                        title={showPwd ? 'Скрыть пароль' : 'Показать пароль'}>
                  <i className={`bi ${showPwd ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                </button>
              </div>
            </div>
            <button className="btn btn-primary w-100 py-2 fw-semibold" disabled={busy}>
              {busy ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-box-arrow-in-right me-2"></i>}
              Войти
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
