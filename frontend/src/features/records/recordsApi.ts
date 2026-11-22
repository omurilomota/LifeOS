import { api } from '@/shared/api/client'
import type { AuditEventResponse, RecordDetail, RecordRequest, RecordResponse, RecordStatus, RecordSummary, RecordType, RelationshipResponse } from '@/shared/types/record'

export function listRecords(params: { status?: RecordStatus; type?: RecordType; q?: string }) {
  const search = new URLSearchParams()
  if (params.status) search.set('status', params.status)
  if (params.type) search.set('type', params.type)
  if (params.q) search.set('q', params.q)
  const qs = search.toString()
  return api<RecordSummary[]>(`/records${qs ? `?${qs}` : ''}`)
}

export function getRecord(id: string) {
  return api<RecordDetail>(`/records/${id}`)
}

export function createRecord(payload: RecordRequest) {
  return api<RecordResponse>('/records', { method: 'POST', body: JSON.stringify(payload) })
}

export function updateRecord(id: string, payload: RecordRequest) {
  return api<RecordResponse>(`/records/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}

export function archiveRecord(id: string) {
  return api<RecordResponse>(`/records/${id}/archive`, { method: 'PATCH' })
}

export function addRelationship(id: string, payload: { toRecordId: string; relationshipType: string }) {
  return api<RelationshipResponse>(`/records/${id}/relationships`, { method: 'POST', body: JSON.stringify(payload) })
}

export function getRecordHistory(id: string) {
  return api<AuditEventResponse[]>(`/records/${id}/history`)
}
