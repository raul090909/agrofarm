import { FormEvent, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, Recommendation } from '../api/api'
import { useLoad } from '../hooks'
import { ErrorAlert, Loading, PageTitle } from '../components/Common'
import { dateTime, TOPICS } from '../utils/format'

const MAX = 1000

export default function RecommendationsPage() {
  const [params] = useSearchParams()
  const recs = useLoad(api.recommendations)
  const farmers = useLoad(api.farmers)

  const [userId, setUserId] = useState(params.get('user') ?? '')
  const [topic, setTopic] = useState(params.get('topic') ?? 'general')
  const [message, setMessage] = useState(params.get('text') ?? '')
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  const [filter, setFilter] = useState<'all' | 'unread' | 'manual' | 'auto'>('all')
  const [editId, setEditId] = useState<number | null>(null)
  const [editText, setEditText] = useState('')
  const [listError, setListError] = useState('')

  const list = useMemo(() => (recs.data ?? []).filter((r) =>
    filter === 'all' || (filter === 'unread' && !r.read) || (filter === 'manual' && !r.auto) || (filter === 'auto' && r.auto),
  ), [recs.data, filter])

  const send = async (e: FormEvent) => {
    e.preventDefault()
    setFormError('')
    setSuccess('')
    if (!userId) { setFormError('Выберите фермера'); return }
    if (!message.trim()) { setFormError('Введите текст рекомендации'); return }
    if (message.length > MAX) { setFormError(`Текст не длиннее ${MAX} символов`); return }
    setBusy(true)
    try {
      const created = await api.sendRecommendation(Number(userId), topic, message.trim())
      recs.setData((prev) => [created, ...(prev ?? [])])
      setMessage('')
      setSuccess(`Рекомендация отправлена: ${created.userName}`)
    } catch (err) {
      setFormError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const saveEdit = async (r: Recommendation) => {
    setListError('')
    if (!editText.trim()) { setListError('Текст не может быть пустым'); return }
    try {
      const updated = await api.editRecommendation(r.id, editText.trim())
      recs.setData((prev) => prev && prev.map((x) => (x.id === r.id ? updated : x)))
      setEditId(null)
    } catch (err) {
      setListError((err as Error).message)
    }
  }

  const remove = async (r: Recommendation) => {
    if (!window.confirm(`Удалить рекомендацию для «${r.userName}»?`)) return
    setListError('')
    try {
      await api.deleteRecommendation(r.id)
      recs.setData((prev) => prev && prev.filter((x) => x.id !== r.id))
    } catch (err) {
      setListError((err as Error).message)
    }
  }

  return (
    <>
      <PageTitle icon="bi-lightbulb">Рекомендации</PageTitle>
      <div className="row g-4">
        <div className="col-xl-4">
          <div className="card page-card">
            <div className="card-header"><i className="bi bi-send me-2"></i>Новая рекомендация</div>
            <div className="card-body">
              {formError && <div className="alert alert-danger py-2 small">{formError}</div>}
              {success && <div className="alert alert-success py-2 small">{success}</div>}
              <form onSubmit={send} noValidate>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Фермер</label>
                  <select className="form-select" value={userId} onChange={(e) => setUserId(e.target.value)} disabled={farmers.loading}>
                    <option value="">— выберите —</option>
                    {(farmers.data ?? []).map((f) => <option key={f.id} value={f.id}>{f.fullName} ({f.email})</option>)}
                  </select>
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Тема</label>
                  <select className="form-select" value={topic} onChange={(e) => setTopic(e.target.value)}>
                    {Object.entries(TOPICS).map(([k, t]) => <option key={k} value={k}>{t.label}</option>)}
                  </select>
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold">Текст</label>
                  <textarea className="form-control" rows={6} value={message} maxLength={MAX}
                            onChange={(e) => setMessage(e.target.value)} placeholder="Например: провести подкормку озимых азотом по мерзлоталой почве" />
                  <div className={`form-text text-end ${message.length > MAX * 0.9 ? 'text-danger' : ''}`}>{message.length} / {MAX}</div>
                </div>
                <button className="btn btn-primary w-100" disabled={busy}>
                  {busy && <span className="spinner-border spinner-border-sm me-2"></span>}Отправить
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-xl-8">
          <div className="card page-card">
            <div className="card-header d-flex align-items-center gap-2 flex-wrap">
              <span><i className="bi bi-chat-square-text me-2"></i>Отправленные и автоматические</span>
              <div className="btn-group btn-group-sm ms-auto">
                {([['all', 'Все'], ['unread', 'Непрочитанные'], ['manual', 'От агронома'], ['auto', 'Автоматические']] as const).map(([k, l]) => (
                  <button key={k} className={`btn ${filter === k ? 'btn-primary' : 'btn-outline-primary'}`} onClick={() => setFilter(k)}>{l}</button>
                ))}
              </div>
            </div>
            <div className="card-body">
              {listError && <ErrorAlert message={listError} />}
              {recs.loading && <Loading />}
              {recs.error && <ErrorAlert message={recs.error} onRetry={recs.reload} />}
              {!recs.loading && list.length === 0 && <div className="empty-state">Рекомендаций нет</div>}
              <div className="d-flex flex-column gap-2">
                {list.map((r) => (
                  <div key={r.id} className={`card rec-item ${r.auto ? 'auto' : ''}`}>
                    <div className="card-body py-2 px-3">
                      <div className="d-flex align-items-center gap-2 flex-wrap small mb-1">
                        <span className="fw-semibold">{r.userName}</span>
                        <span className="badge bg-light text-dark border"><i className={`bi ${TOPICS[r.topic]?.icon} me-1`}></i>{TOPICS[r.topic]?.label}</span>
                        {r.auto && <span className="badge" style={{ background: 'rgba(192,138,62,.15)', color: '#8A5E22' }}>Автоматически</span>}
                        <span className={`badge ${r.read ? 'bg-secondary-subtle text-secondary' : 'bg-success'}`}>{r.read ? 'Прочитано' : 'Не прочитано'}</span>
                        <span className="text-muted ms-auto">{dateTime(r.createdAt)}</span>
                      </div>
                      {editId === r.id ? (
                        <>
                          <textarea className="form-control form-control-sm mb-2" rows={3} maxLength={MAX} value={editText} onChange={(e) => setEditText(e.target.value)} />
                          <div className="d-flex gap-2">
                            <button className="btn btn-sm btn-primary" onClick={() => saveEdit(r)}>Сохранить</button>
                            <button className="btn btn-sm btn-outline-secondary" onClick={() => setEditId(null)}>Отмена</button>
                          </div>
                        </>
                      ) : (
                        <div className="d-flex gap-2">
                          <div className="flex-grow-1" style={{ whiteSpace: 'pre-wrap' }}>{r.message}</div>
                          {!r.auto && (
                            <div className="d-flex gap-1 align-items-start">
                              <button className="btn btn-sm btn-outline-secondary" title="Изменить"
                                      onClick={() => { setEditId(r.id); setEditText(r.message) }}><i className="bi bi-pencil"></i></button>
                              <button className="btn btn-sm btn-outline-danger" title="Удалить" onClick={() => remove(r)}><i className="bi bi-trash"></i></button>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
