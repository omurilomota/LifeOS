export type RecordType = 'ASSET' | 'VEHICLE' | 'PROPERTY' | 'SERVICE' | 'SUBSCRIPTION' | 'CONTRACT' | 'OTHER'
export type RecordStatus = 'ACTIVE' | 'ARCHIVED'

export type RecordSummary = {
  id: string
  type: RecordType
  title: string
  category: string | null
  status: RecordStatus
  responsibleMemberId: string | null
  updatedAt: string
}

export type RecordResponse = {
  id: string
  householdId: string
  type: RecordType
  title: string
  description: string | null
  category: string | null
  status: RecordStatus
  responsibleMemberId: string | null
  acquiredAt: string | null
  startedAt: string | null
  archivedAt: string | null
  amountMinor: number | null
  currency: string | null
  metadataJson: string | null
  createdAt: string
  updatedAt: string
}

export type RelationshipResponse = {
  id: string
  fromRecordId: string
  toRecordId: string
  relationshipType: string
  createdAt: string
}

export type RecordDetail = {
  record: RecordResponse
  relationships: RelationshipResponse[]
}

export type AuditEventResponse = {
  id: string
  actorUserId: string | null
  entityType: string
  entityId: string | null
  action: string
  metadataJson: string | null
  occurredAt: string
}

export type RecordRequest = {
  type: RecordType
  title: string
  description?: string
  category?: string
  status?: RecordStatus
  responsibleMemberId?: string
  acquiredAt?: string
  startedAt?: string
  amountMinor?: number
  currency?: string
  metadataJson?: string
}
