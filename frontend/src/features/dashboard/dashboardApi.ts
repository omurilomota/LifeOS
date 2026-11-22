import { api } from '@/shared/api/client'
import type { DashboardResponse, SearchResponse } from '@/shared/types/obligation'

export const getDashboard = () => api<DashboardResponse>('/dashboard')
export const search = (q: string) => api<SearchResponse>(`/search?q=${encodeURIComponent(q)}`)
