import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { auth, loading } = useAuth()
  if (loading) return <div className="p-8 text-sm text-muted">Carregando...</div>
  if (!auth) return <Navigate to="/login" replace />
  return <Outlet />
}
