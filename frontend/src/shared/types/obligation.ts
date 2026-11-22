export type ObligationPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type ObligationStatus = 'OPEN' | 'COMPLETED' | 'CANCELLED'

export type Obligation = {
  id: string
  householdId: string
  recordId: string | null
  title: string
  description: string | null
  responsibleMemberId: string | null
  dueAt: string
  priority: ObligationPriority
  status: ObligationStatus
  recurrenceRule: string | null
  sourceType: string
  sourceDocumentId: string | null
  completedAt: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export type Notification = {
  id: string
  type: string
  title: string
  body: string | null
  readAt: string | null
  relatedEntityType: string | null
  relatedEntityId: string | null
  createdAt: string
}

export type DashboardResponse = {
  overdue: Obligation[]
  dueSoon: Obligation[]
  recentRecords: { id: string; type: string; title: string; category: string | null; status: string; responsibleMemberId: string | null; updatedAt: string }[]
  notifications: Notification[]
}

export type SearchResponse = {
  records: { id: string; type: string; title: string; subtitle: string | null }[]
  documents: { id: string; type: string; title: string; subtitle: string | null }[]
  obligations: { id: string; type: string; title: string; subtitle: string | null }[]
}
