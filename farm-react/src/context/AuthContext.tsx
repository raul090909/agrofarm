import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import { api, getToken, setToken, setUnauthorizedHandler } from '../api/api'

interface AuthContextType {
  loggedIn: boolean
  name: string
  login: (login: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType>(null!)

function readName() {
  try { return sessionStorage.getItem('agro_name') || '' } catch { return '' }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [loggedIn, setLoggedIn] = useState(() => !!getToken())
  const [name, setName] = useState(readName)

  const clear = () => {
    setToken(null)
    try { sessionStorage.removeItem('agro_name') } catch {  }
    setLoggedIn(false)
    setName('')
  }

  useEffect(() => { setUnauthorizedHandler(clear) }, [])

  const login = async (loginValue: string, password: string) => {
    const r = await api.login(loginValue, password)
    setToken(r.token)
    try { sessionStorage.setItem('agro_name', r.profile.fullName) } catch {  }
    setName(r.profile.fullName)
    setLoggedIn(true)
  }

  const logout = async () => {
    try { await api.logout() } catch {  }
    clear()
  }

  return <AuthContext.Provider value={{ loggedIn, name, login, logout }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
