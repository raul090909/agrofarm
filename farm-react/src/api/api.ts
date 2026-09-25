const TOKEN_KEY = 'agro_token'

export function getToken(): string | null {
  try { return sessionStorage.getItem(TOKEN_KEY) } catch { return null }
}
export function setToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token)
    else sessionStorage.removeItem(TOKEN_KEY)
  } catch {  }
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(fn: () => void) { onUnauthorized = fn }

async function req<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined })
  } catch {
    throw new ApiError(0, 'Сервер недоступен. Проверьте, что бэкенд запущен.')
  }
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    if (res.status === 401 && path !== '/api/agronomist/login') onUnauthorized()
    throw new ApiError(res.status, (data && data.error) || `Ошибка ${res.status}`)
  }
  return data as T
}

export async function downloadExport(format: 'csv' | 'excel', from: string, to: string) {
  const res = await fetch(`/export/${format}?from=${from}&to=${to}`, {
    headers: { Authorization: `Bearer ${getToken() ?? ''}` },
  })
  if (!res.ok) {
    const data = await res.json().catch(() => null)
    throw new ApiError(res.status, (data && data.error) || `Ошибка ${res.status}`)
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `operations_${from}_${to}.${format === 'csv' ? 'csv' : 'xlsx'}`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const A = '/api/agronomist'

export const api = {
  login: (login: string, password: string) =>
    req<LoginResponse>('POST', `${A}/login`, { login, password }),
  logout: () => req<{ success: boolean }>('POST', '/api/auth/logout'),
  dashboard: () => req<DashboardData>('GET', `${A}/dashboard`),
  farmers: () => req<FarmerRow[]>('GET', `${A}/farmers`),
  farmer: (id: number) => req<FarmerDetail>('GET', `${A}/farmers/${id}`),
  setFarmerActive: (id: number, active: boolean) =>
    req<{ success: boolean }>('PUT', `${A}/farmers/${id}/active`, { active }),
  units: () => req<UnitRow[]>('GET', `${A}/units`),
  unitOperations: (id: number) => req<Operation[]>('GET', `${A}/units/${id}/operations`),
  anomalies: () => req<AnomaliesData>('GET', `${A}/anomalies`),
  recommendations: () => req<Recommendation[]>('GET', `${A}/recommendations`),
  sendRecommendation: (userId: number, topic: string, message: string) =>
    req<Recommendation>('POST', `${A}/recommendations`, { userId, topic, message }),
  editRecommendation: (id: number, message: string) =>
    req<Recommendation>('PUT', `${A}/recommendations/${id}`, { message }),
  deleteRecommendation: (id: number) => req<{ success: boolean }>('DELETE', `${A}/recommendations/${id}`),
}

export interface LoginResponse {
  token: string
  expiresAt: string
  profile: { id: number; fullName: string; login: string }
}

export interface MonthRow { monthName: string; income: number; expense: number; profit: number }

export interface Operation {
  id: number
  unitId: number
  unitName: string
  categoryId: number
  categoryName: string
  categoryIcon: string
  type: 'income' | 'expense'
  amount: number
  quantity: number | null
  quantityUnit: string | null
  description: string | null
  operationDate: string
  userName?: string
}

export interface DashboardData {
  totalFarmers: number
  totalUnits: number
  totalOperations: number
  cultivatedAreaHa: number
  totalHeads: number
  monthIncome: number
  monthExpense: number
  monthProfit: number
  monthly: MonthRow[]
  expenseStructure: { name: string; total: number }[]
  recentOperations: Operation[]
}

export interface FarmerRow {
  id: number
  fullName: string
  email: string
  active: boolean
  unitCount: number
  areaHa: number
  heads: number
  opsThisMonth: number
  monthIncome: number
  monthExpense: number
  monthProfit: number
}

export interface Unit {
  id: number
  name: string
  type: 'field' | 'livestock' | 'greenhouse' | 'storage'
  typeLabel: string
  areaHa: number
  headCount: number
  description: string | null
}

export interface LimitRow {
  id: number
  userId: number
  userName: string
  categoryId: number
  categoryName: string
  amount: number
  period: 'month' | 'quarter'
  periodLabel: string
  spent: number
  percent: number
  status: 'ok' | 'warning' | 'exceeded'
}

export interface FarmerDetail {
  id: number
  fullName: string
  email: string
  createdAt: string
  areaHa: number
  heads: number
  units: Unit[]
  monthly: MonthRow[]
  recentOperations: Operation[]
  limits: LimitRow[]
  recommendationCount: number
}

export interface UnitRow extends Unit {
  userId: number
  userName: string
  income: number
  expense: number
  profit: number
  costPerHa: number | null
  costPerHead: number | null
}

export interface GrowthRow {
  userId: number
  userName: string
  lastMonth: number
  thisMonth: number
  growthPercent: number
}

export interface AnomaliesData {
  limitAnomalies: LimitRow[]
  expenseGrowth: GrowthRow[]
  unprofitableUnits: UnitRow[]
}

export interface Recommendation {
  id: number
  userId: number
  userName: string
  userEmail: string
  topic: 'general' | 'crops' | 'livestock' | 'finance'
  message: string
  read: boolean
  auto: boolean
  createdAt: string
}
