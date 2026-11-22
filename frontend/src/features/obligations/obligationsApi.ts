import { api } from '@/shared/api/client'
import type { Obligation, ObligationPriority, ObligationStatus } from '@/shared/types/obligation'

export type ObligationRequest = {
  title: string; description?: string; recordId?: string; responsibleMemberId?: string;
  dueAt: string; priority: ObligationPriority; status?: ObligationStatus; recurrenceRule?: string;
}

export const listObligations = (status?: ObligationStatus) => api<Obligation[]>(`/obligations${status ? `?status=${status}` : ''}`)
export const createObligation = (payload: ObligationRequest) => api<Obligation>('/obligations', { method: 'POST', body: JSON.stringify(payload) })
export const updateObligation = (id: string, payload: ObligationRequest) => api<Obligation>(`/obligations/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const completeObligation = (id: string) => api<Obligation>(`/obligations/${id}/complete`, { method: 'PATCH' })
export const cancelObligation = (id: string) => api<Obligation>(`/obligations/${id}/cancel`, { method: 'PATCH' })
export const rescheduleObligation = (id: string, dueAt: string) => api<Obligation>(`/obligations/${id}/reschedule`, { method: 'PATCH', body: JSON.stringify({ dueAt }) })
