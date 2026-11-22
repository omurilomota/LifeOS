import { api } from '@/shared/api/client'
import type { AuthResponse } from '@/shared/types/auth'

export function register(payload: {
  email: string
  password: string
  displayName: string
}) {
  return api<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(payload) })
}

export function login(payload: { email: string; password: string }) {
  return api<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(payload) })
}

export function logout() {
  return api<void>('/auth/logout', { method: 'POST' })
}

export function me() {
  return api<AuthResponse>('/auth/me')
}
