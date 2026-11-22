export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED'
export type DocumentReviewStatus = 'PENDING' | 'REVIEWED' | 'REJECTED'

export type DocumentResponse = {
  id: string
  householdId: string
  uploadedByUserId: string
  originalFilename: string
  mimeType: string
  sizeBytes: number
  checksumSha256: string
  extractionStatus: DocumentStatus
  extractedText: string | null
  extractionError: string | null
  reviewStatus: DocumentReviewStatus
  createdAt: string
  updatedAt: string
}

export type DocumentLinkResponse = {
  id: string
  recordId: string
  documentId: string
  linkType: string
  createdAt: string
}
