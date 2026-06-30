import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api'

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
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isAdmin: boolean
  login: (data: LoginData) => Promise<void>
  register: (data: RegisterData) => Promise<void>
  logout: () => void
  
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [carregandoAuth, setCarregandoAuth] = useState(true)

  useEffect(() => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      setToken(savedToken)
      setUser(JSON.parse(savedUser))
    }

    setCarregandoAuth(false)
  }, [])

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
  }

  async function register(data: RegisterData) {
    await api.post('/auth/register', data)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')

    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        carregandoAuth,
        user,
        token,
        isAuthenticated: !!user && !!token,
        isAdmin: user?.role === 'ADMIN',
        login,
        register,
        logout,
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