import { api } from '@/shared/api/client'

export type Member = { membershipId: string; userId: string; displayName: string; email: string; role: string; joinedAt: string }
export type Invitation = { id: string; invitedEmail: string; role: string; expiresAt: string; acceptedAt: string | null; revokedAt: string | null; createdAt: string }
export type HouseholdDetail = { id: string; name: string; myRole: string; members: Member[]; invitations: Invitation[] }

export const getHouseholdDetail = () => api<HouseholdDetail>('/households/current/detail')
export const inviteMember = (payload: { email: string; role: string }) => api<{ invitation: Invitation; token: string }>('/households/current/invitations', { method: 'POST', body: JSON.stringify(payload) })
export const acceptInvite = (token: string) => api('/households/invitations/accept', { method: 'POST', body: JSON.stringify({ token }) })
export const revokeInvite = (id: string) => api<void>(`/households/current/invitations/${id}/revoke`, { method: 'POST' })
export const updateMemberRole = (membershipId: string, role: string) => api<Member>(`/households/current/members/${membershipId}/role`, { method: 'PATCH', body: JSON.stringify({ role }) })
export const removeMember = (membershipId: string) => api<void>(`/households/current/members/${membershipId}`, { method: 'DELETE' })
