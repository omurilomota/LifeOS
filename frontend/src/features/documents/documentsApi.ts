import { api } from '@/shared/api/client'
import type { DocumentLinkResponse, DocumentResponse, DocumentStatus } from '@/shared/types/document'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

function getCookie(name: string): string | undefined {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) return parts.pop()?.split(';').shift()
  return undefined
}

export function listDocuments(status?: DocumentStatus) {
  const qs = status ? `?status=${encodeURIComponent(status)}` : ''
  return api<DocumentResponse[]>(`/documents${qs}`)
}

export async function uploadDocument(file: File) {
  const form = new FormData()
  form.append('file', file)
  const csrf = getCookie('XSRF-TOKEN')
  const res = await fetch(`${API_BASE}/documents`, {
    method: 'POST',
    credentials: 'include',
    headers: csrf ? { 'X-XSRF-TOKEN': decodeURIComponent(csrf) } : undefined,
    body: form,
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) throw data
  return data as DocumentResponse
}

export function deleteDocument(id: string) {
  return api<void>(`/documents/${id}`, { method: 'DELETE' })
}

export function linkDocument(id: string, payload: { recordId: string; linkType: string }) {
  return api<DocumentLinkResponse>(`/documents/${id}/links`, { method: 'POST', body: JSON.stringify(payload) })
}

export function getDocumentLinks(id: string) {
  return api<DocumentLinkResponse[]>(`/documents/${id}/links`)
}

export function downloadDocumentUrl(id: string) {
  return `${API_BASE}/documents/${id}/download`
}

export type AiSuggestion = {
  id: string; documentId: string; provider: string; status: string; suggestionType: string;
  payload: Record<string, unknown>; confidence: string | null; evidenceText: string | null; createdAt: string;
}
export const getAiStatus = () => api<{ enabled: boolean }>('/ai/status')
export const listSuggestions = (documentId: string) => api<AiSuggestion[]>(`/documents/${documentId}/suggestions`)
export const generateSuggestions = (documentId: string) => api<AiSuggestion[]>(`/documents/${documentId}/suggestions`, { method: 'POST' })
export const confirmSuggestion = (id: string) => api<{ suggestionId: string; status: string; createdEntityId: string; createdEntityType: string }>(`/ai/suggestions/${id}/confirm`, { method: 'POST' })
export const rejectSuggestion = (id: string) => api<AiSuggestion>(`/ai/suggestions/${id}/reject`, { method: 'POST' })
