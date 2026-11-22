import { useEffect, useMemo, useState } from 'react'
import type { AuthResponse } from '@/shared/types/auth'
import * as authApi from './authApi'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    try {
      const me = await authApi.me()
      setAuth(me)
    } catch {
      setAuth(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    auth,
    loading,
    refresh,
    async login(email, password) {
      setAuth(await authApi.login({ email, password }))
    },
    async register(displayName, email, password) {
      setAuth(await authApi.register({ displayName, email, password }))
    },
    async logout() {
      await authApi.logout()
      setAuth(null)
    },
  }), [auth, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
