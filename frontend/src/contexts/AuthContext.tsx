import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api'
import { useNavigate } from 'react-router-dom'
import { expirationTime, expireSession, SESSION_EXPIRED } from '../services/session'

type UserRole = 'ADMIN' | 'CUSTOMER'

type User = {
  id: string
  nome: string
  email: string
  role: UserRole
}

type LoginData = {
  email: string
  senha: string
}

type RegisterData = {
  nome: string
  email: string
  senha: string
}

type AuthContextType = {
  carregandoAuth: boolean
  sessaoExpirada: boolean
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isAdmin: boolean
  login: (data: LoginData) => Promise<void>
  register: (data: RegisterData) => Promise<void>
  logout: () => void
  atualizarUsuario: (usuario: User) => void
  
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const [sessaoExpirada, setSessaoExpirada] = useState(false)
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [carregandoAuth, setCarregandoAuth] = useState(true)

  useEffect(() => {
    function aoExpirar() {
      setToken(null)
      setUser(null)
      setSessaoExpirada(true)
      // A recovery link must remain usable even when an old login has expired.
      if (!['/esqueci-senha', '/redefinir-senha'].includes(window.location.pathname)) {
        navigate('/login', { replace: true })
      }
    }
    window.addEventListener(SESSION_EXPIRED, aoExpirar)
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      try {
        const parsedUser = JSON.parse(savedUser)
        if (!parsedUser || typeof parsedUser.email !== 'string' || expirationTime(savedToken) <= Date.now()) {
          expireSession(savedToken)
        } else {
          setToken(savedToken)
          setUser(parsedUser)
        }
      } catch { expireSession(savedToken) }
    } else if (savedToken || savedUser) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }

    setCarregandoAuth(false)
    return () => window.removeEventListener(SESSION_EXPIRED, aoExpirar)
  }, [navigate])

  useEffect(() => {
    if (!token) return
    const verificar = () => {
      if (expirationTime(token) <= Date.now()) expireSession(token)
    }
    const timer = window.setTimeout(verificar, Math.min(Math.max(0, expirationTime(token) - Date.now()), 2147483647))
    window.addEventListener('focus', verificar)
    document.addEventListener('visibilitychange', verificar)
    return () => {
      window.clearTimeout(timer)
      window.removeEventListener('focus', verificar)
      document.removeEventListener('visibilitychange', verificar)
    }
  }, [token])

  async function login(data: LoginData) {
    const response = await api.post('/auth/login', data)

    const loggedUser: User = {
      id: response.data.id,
      nome: response.data.nome,
      email: response.data.email,
      role: response.data.role,
    }

    localStorage.setItem('token', response.data.token)
    localStorage.setItem('user', JSON.stringify(loggedUser))

    setToken(response.data.token)
    setUser(loggedUser)
    setSessaoExpirada(false)
  }

  async function register(data: RegisterData) {
    await api.post('/auth/register', data)
  }

  function logout() {
    setSessaoExpirada(false)
    localStorage.removeItem('token')
    localStorage.removeItem('user')

    setToken(null)
    setUser(null)
  }

  function atualizarUsuario(usuario: User) {
    localStorage.setItem('user', JSON.stringify(usuario))
    setUser(usuario)
  }

  return (
    <AuthContext.Provider
      value={{
        carregandoAuth,
        sessaoExpirada,
        user,
        token,
        isAuthenticated: !!user && !!token,
        isAdmin: user?.role === 'ADMIN',
        login,
        register,
        logout,
        atualizarUsuario,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider')
  }

  return context
}
