import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

type AdminRouteProps = {
  children: React.ReactNode
}

export default function AdminRoute({ children }: AdminRouteProps) {
  const { isAuthenticated, isAdmin, carregandoAuth } = useAuth()

  if (carregandoAuth) {
    return null
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!isAdmin) {
    return <Navigate to="/" replace />
  }

  return children
}