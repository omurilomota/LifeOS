import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { confirmSuggestion, deleteDocument, downloadDocumentUrl, generateSuggestions, getAiStatus, getDocumentLinks, linkDocument, listDocuments, listSuggestions, rejectSuggestion, uploadDocument } from './documentsApi'
import type { DocumentStatus } from '@/shared/types/document'
import type { ApiError } from '@/shared/api/client'

export function DocumentsPage() {
  const qc = useQueryClient()
  const [status, setStatus] = useState<DocumentStatus | ''>('')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [linkDocId, setLinkDocId] = useState<string | null>(null)
  const [recordId, setRecordId] = useState('')
  const [linkType, setLinkType] = useState('ATTACHMENT')
  const [suggestionDocId, setSuggestionDocId] = useState<string | null>(null)

  const docsQuery = useQuery({ queryKey: ['documents', status], queryFn: () => listDocuments(status || undefined) })
  const linksQuery = useQuery({ queryKey: ['document-links', linkDocId], queryFn: () => getDocumentLinks(linkDocId!), enabled: !!linkDocId })
  const aiStatusQuery = useQuery({ queryKey: ['ai-status'], queryFn: getAiStatus })
  const suggestionsQuery = useQuery({ queryKey: ['suggestions', suggestionDocId], queryFn: () => listSuggestions(suggestionDocId!), enabled: !!suggestionDocId })

  const uploadMutation = useMutation({
    mutationFn: (f: File) => uploadDocument(f),
    onSuccess: async () => { setFile(null); setError(null); await qc.invalidateQueries({ queryKey: ['documents'] }) },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha no upload'),
  })
  const deleteMutation = useMutation({ mutationFn: deleteDocument, onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['documents'] }) } })
  const linkMutation = useMutation({
    mutationFn: () => linkDocument(linkDocId!, { recordId, linkType }),
    onSuccess: async () => { setRecordId(''); await qc.invalidateQueries({ queryKey: ['document-links', linkDocId] }) },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao vincular'),
  })
  const generateMut = useMutation({
    mutationFn: (docId: string) => generateSuggestions(docId),
    onSuccess: async (_, docId) => { setError(null); await qc.invalidateQueries({ queryKey: ['suggestions', docId] }) },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'IA indisponível'),
  })
  const confirmMut = useMutation({
    mutationFn: confirmSuggestion,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['suggestions', suggestionDocId] }); await qc.invalidateQueries({ queryKey: ['records'] }); await qc.invalidateQueries({ queryKey: ['obligations'] }); await qc.invalidateQueries({ queryKey: ['dashboard'] }) },
  })
  const rejectMut = useMutation({ mutationFn: rejectSuggestion, onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['suggestions', suggestionDocId] }) } })

  const counts = useMemo(() => {
    const all = docsQuery.data ?? []
    return { total: all.length, pending: all.filter((d) => d.extractionStatus === 'PENDING' || d.extractionStatus === 'PROCESSING').length, ready: all.filter((d) => d.extractionStatus === 'READY').length }
  }, [docsQuery.data])

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-accent">Documentos</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">Arquivos e comprovantes</h1>
        <p className="mt-2 text-sm text-muted">Envie documentos, acompanhe processamento, vincule e revise sugestões de IA.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Total</p><p className="mt-2 text-2xl font-semibold">{counts.total}</p></div>
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Em processamento</p><p className="mt-2 text-2xl font-semibold">{counts.pending}</p></div>
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Prontos</p><p className="mt-2 text-2xl font-semibold">{counts.ready}</p></div>
      </div>

      <div className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4">
        <h2 className="font-medium">Upload</h2>
        <div className="flex flex-col gap-3 md:flex-row md:items-center">
          <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="block w-full text-sm" />
          <button disabled={!file || uploadMutation.isPending} onClick={() => file && uploadMutation.mutate(file)} className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{uploadMutation.isPending ? 'Enviando...' : 'Enviar'}</button>
        </div>
        <p className="text-xs text-muted">Tipos suportados no baseline: PDF, PNG, JPG, TXT. Máx. 10MB. IA: {aiStatusQuery.data?.enabled ? 'habilitada' : 'desabilitada (fluxo manual disponível)'}.</p>
        {error && <p className="text-sm text-danger">{error}</p>}
      </div>

      <div className="flex gap-3">
        <select className="rounded-lg border border-line px-3 py-2 text-sm" value={status} onChange={(e) => setStatus(e.target.value as DocumentStatus | '')}>
          <option value="">Todos</option><option value="PENDING">PENDING</option><option value="PROCESSING">PROCESSING</option><option value="READY">READY</option><option value="FAILED">FAILED</option>
        </select>
      </div>

      <div className="rounded-2xl border border-line bg-white shadow-panel overflow-hidden">
        {docsQuery.isLoading && <div className="p-6 text-sm text-muted">Carregando...</div>}
        {!docsQuery.isLoading && (docsQuery.data?.length ?? 0) === 0 && <div className="p-8 text-sm text-muted">Nenhum documento no inbox.</div>}
        <ul className="divide-y divide-line">
          {docsQuery.data?.map((doc) => (
            <li key={doc.id} className="p-4 space-y-3">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <p className="font-medium">{doc.originalFilename}</p>
                  <div className="mt-1 flex flex-wrap gap-2 text-xs text-muted">
                    <span className="rounded-full bg-canvas px-2 py-1">{doc.mimeType}</span>
                    <span className="rounded-full bg-canvas px-2 py-1">{Math.round(doc.sizeBytes / 1024)} KB</span>
                    <span className="rounded-full bg-accentSoft px-2 py-1 text-accent">{doc.extractionStatus}</span>
                    <span className="rounded-full bg-canvas px-2 py-1">{doc.reviewStatus}</span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  <a href={downloadDocumentUrl(doc.id)} className="rounded-lg border border-line px-3 py-2 text-sm" target="_blank" rel="noreferrer">Download</a>
                  <button onClick={() => setLinkDocId(linkDocId === doc.id ? null : doc.id)} className="rounded-lg border border-line px-3 py-2 text-sm">Vincular</button>
                  <button onClick={() => { setSuggestionDocId(suggestionDocId === doc.id ? null : doc.id); if (suggestionDocId !== doc.id) void qc.invalidateQueries({ queryKey: ['suggestions', doc.id] }) }} className="rounded-lg border border-line px-3 py-2 text-sm">Sugestões IA</button>
                  <button onClick={() => { if (confirm('Excluir documento?')) deleteMutation.mutate(doc.id) }} className="rounded-lg border border-line px-3 py-2 text-sm text-danger">Excluir</button>
                </div>
              </div>
              {doc.extractedText && <pre className="whitespace-pre-wrap rounded-lg bg-canvas p-3 text-xs text-muted max-h-32 overflow-auto">{doc.extractedText}</pre>}

              {linkDocId === doc.id && (
                <div className="rounded-lg border border-line p-3 space-y-2">
                  <div className="flex flex-col gap-2 md:flex-row">
                    <input className="flex-1 rounded-lg border border-line px-3 py-2 text-sm" placeholder="Record ID" value={recordId} onChange={(e) => setRecordId(e.target.value)} />
                    <input className="rounded-lg border border-line px-3 py-2 text-sm" placeholder="Link type" value={linkType} onChange={(e) => setLinkType(e.target.value)} />
                    <button disabled={!recordId || linkMutation.isPending} onClick={() => linkMutation.mutate()} className="rounded-lg bg-accent px-3 py-2 text-sm text-white disabled:opacity-60">Salvar vínculo</button>
                  </div>
                  <div className="text-xs text-muted">{linksQuery.data?.length ? linksQuery.data.map((l) => <div key={l.id}>{l.linkType} → {l.recordId}</div>) : 'Sem vínculos.'}</div>
                </div>
              )}

              {suggestionDocId === doc.id && (
                <div className="rounded-lg border border-line p-3 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium">Sugestões (revisão humana obrigatória)</p>
                    <button disabled={generateMut.isPending || !aiStatusQuery.data?.enabled} onClick={() => generateMut.mutate(doc.id)} className="rounded-lg border border-line px-3 py-2 text-sm disabled:opacity-50">{generateMut.isPending ? 'Gerando...' : 'Gerar sugestões'}</button>
                  </div>
                  {!aiStatusQuery.data?.enabled && <p className="text-xs text-muted">IA desabilitada. Configure `AI_ENABLED=true` para habilitar sugestões; o fluxo manual continua funcionando.</p>}
                  <ul className="space-y-2">
                    {(suggestionsQuery.data ?? []).map((s) => (
                      <li key={s.id} className="rounded-lg bg-canvas p-3 text-sm">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium">{s.suggestionType}</span>
                          <span className="text-xs text-muted">{s.status}</span>
                          <span className="text-xs text-muted">confiança: {s.confidence ?? 'n/a'}</span>
                        </div>
                        <div className="mt-1 text-xs text-muted">{s.evidenceText}</div>
                        <pre className="mt-2 whitespace-pre-wrap text-xs text-muted">{JSON.stringify(s.payload, null, 2)}</pre>
                        {s.status === 'PENDING' && (
                          <div className="mt-2 flex gap-2">
                            <button onClick={() => confirmMut.mutate(s.id)} className="rounded-lg bg-accent px-3 py-1.5 text-xs text-white">Confirmar</button>
                            <button onClick={() => rejectMut.mutate(s.id)} className="rounded-lg border border-line px-3 py-1.5 text-xs">Rejeitar</button>
                          </div>
                        )}
                      </li>
                    ))}
                    {suggestionsQuery.data?.length === 0 && <li className="text-xs text-muted">Sem sugestões ainda.</li>}
                  </ul>
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
