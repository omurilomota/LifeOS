import { api } from '@/shared/api/client'
import type { Notification } from '@/shared/types/obligation'

export const listNotifications = () => api<Notification[]>('/notifications')
export const markNotificationRead = (id: string) => api<Notification>(`/notifications/${id}/read`, { method: 'PATCH' })
export const markAllNotificationsRead = () => api<void>('/notifications/read-all', { method: 'PATCH' })
