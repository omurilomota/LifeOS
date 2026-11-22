import { createContext } from 'react'
import type { AuthResponse } from '@/shared/types/auth'

export type AuthContextValue = {
  auth: AuthResponse | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (displayName: string, email: string, password: string) => Promise<void>
  demo: () => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
