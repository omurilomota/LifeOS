import { api } from '@/shared/api/client'
export const exportPrivacyData = () => api<Record<string, unknown>>('/privacy/export')
export const deleteAccount = () => api<void>('/privacy/account', { method: 'DELETE' })
