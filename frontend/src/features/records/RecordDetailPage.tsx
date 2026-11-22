import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addRelationship, archiveRecord, getRecord, getRecordHistory, updateRecord } from './recordsApi'
import type { RecordRequest, RecordStatus, RecordType } from '@/shared/types/record'
import type { ApiError } from '@/shared/api/client'

const TYPES: RecordType[] = ['ASSET', 'VEHICLE', 'PROPERTY', 'SERVICE', 'SUBSCRIPTION', 'CONTRACT', 'OTHER']

export function RecordDetailPage() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<RecordRequest | null>(null)
  const [relTo, setRelTo] = useState('')
  const [relType, setRelType] = useState('RELATED_TO')
  const [error, setError] = useState<string | null>(null)

  const detailQuery = useQuery({ queryKey: ['record', id], queryFn: () => getRecord(id), enabled: !!id })
  const historyQuery = useQuery({ queryKey: ['record-history', id], queryFn: () => getRecordHistory(id), enabled: !!id })

  const saveMutation = useMutation({
    mutationFn: (payload: RecordRequest) => updateRecord(id, payload),
    onSuccess: async () => {
      setEditing(false); setError(null)
      await qc.invalidateQueries({ queryKey: ['record', id] })
      await qc.invalidateQueries({ queryKey: ['record-history', id] })
      await qc.invalidateQueries({ queryKey: ['records'] })
    },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao salvar'),
  })

  const archiveMutation = useMutation({
    mutationFn: () => archiveRecord(id),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['record', id] })
      await qc.invalidateQueries({ queryKey: ['record-history', id] })
      await qc.invalidateQueries({ queryKey: ['records'] })
    },
  })

  const relMutation = useMutation({
    mutationFn: () => addRelationship(id, { toRecordId: relTo, relationshipType: relType }),
    onSuccess: async () => {
      setRelTo(''); setError(null)
      await qc.invalidateQueries({ queryKey: ['record', id] })
      await qc.invalidateQueries({ queryKey: ['record-history', id] })
    },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao criar relação'),
  })

  if (detailQuery.isLoading) return <div className="text-sm text-muted">Carregando...</div>
  if (detailQuery.isError || !detailQuery.data) return <div className="text-sm text-danger">Registro não encontrado.</div>

  const { record, relationships } = detailQuery.data
  const currentForm: RecordRequest = form ?? {
    type: record.type, title: record.title, description: record.description ?? '', category: record.category ?? '',
    status: record.status, acquiredAt: record.acquiredAt ?? undefined, startedAt: record.startedAt ?? undefined,
    amountMinor: record.amountMinor ?? undefined, currency: record.currency ?? undefined, metadataJson: record.metadataJson ?? undefined,
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <Link to="/records" className="text-sm text-muted hover:text-accent">← Registros</Link>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight">{record.title}</h1>
          <p className="mt-1 text-sm text-muted">{record.type} · {record.status}</p>
        </div>
        <div className="flex gap-2">
          {record.status !== 'ARCHIVED' && <button onClick={() => archiveMutation.mutate()} className="rounded-lg border border-line px-3 py-2 text-sm">Arquivar</button>}
          <button onClick={() => { setForm(currentForm); setEditing((v) => !v) }} className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white">{editing ? 'Cancelar' : 'Editar'}</button>
        </div>
      </div>

      {error && <p className="text-sm text-danger">{error}</p>}

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4">
          <h2 className="font-medium">Detalhes</h2>
          {!editing ? (
            <dl className="grid gap-4 text-sm md:grid-cols-2">
              <div><dt className="text-muted">Descrição</dt><dd>{record.description || '—'}</dd></div>
              <div><dt className="text-muted">Categoria</dt><dd>{record.category || '—'}</dd></div>
              <div><dt className="text-muted">Status</dt><dd>{record.status}</dd></div>
              <div><dt className="text-muted">Atualizado</dt><dd>{new Date(record.updatedAt).toLocaleString()}</dd></div>
            </dl>
          ) : (
            <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); if (form) saveMutation.mutate(form) }}>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="text-sm"><span className="mb-1 block text-muted">Título</span><input className="w-full rounded-lg border border-line px-3 py-2" value={currentForm.title} onChange={(e) => setForm({ ...currentForm, title: e.target.value })} required /></label>
                <label className="text-sm"><span className="mb-1 block text-muted">Tipo</span><select className="w-full rounded-lg border border-line px-3 py-2" value={currentForm.type} onChange={(e) => setForm({ ...currentForm, type: e.target.value as RecordType })}>{TYPES.map(t => <option key={t} value={t}>{t}</option>)}</select></label>
                <label className="text-sm"><span className="mb-1 block text-muted">Categoria</span><input className="w-full rounded-lg border border-line px-3 py-2" value={currentForm.category ?? ''} onChange={(e) => setForm({ ...currentForm, category: e.target.value })} /></label>
                <label className="text-sm"><span className="mb-1 block text-muted">Status</span><select className="w-full rounded-lg border border-line px-3 py-2" value={currentForm.status} onChange={(e) => setForm({ ...currentForm, status: e.target.value as RecordStatus })}><option value="ACTIVE">ACTIVE</option><option value="ARCHIVED">ARCHIVED</option></select></label>
              </div>
              <label className="block text-sm"><span className="mb-1 block text-muted">Descrição</span><textarea className="w-full rounded-lg border border-line px-3 py-2" rows={4} value={currentForm.description ?? ''} onChange={(e) => setForm({ ...currentForm, description: e.target.value })} /></label>
              <button disabled={saveMutation.isPending} className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{saveMutation.isPending ? 'Salvando...' : 'Salvar alterações'}</button>
            </form>
          )}
        </section>

        <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4">
          <h2 className="font-medium">Relações</h2>
          <ul className="space-y-2 text-sm">
            {relationships.length === 0 && <li className="text-muted">Sem relações.</li>}
            {relationships.map((r) => <li key={r.id} className="rounded-lg bg-canvas px-3 py-2"><span className="text-muted">{r.relationshipType}</span> → <span className="font-mono text-xs">{r.toRecordId}</span></li>)}
          </ul>
          <div className="space-y-2 border-t border-line pt-4">
            <input className="w-full rounded-lg border border-line px-3 py-2 text-sm" placeholder="ID do registro relacionado" value={relTo} onChange={(e) => setRelTo(e.target.value)} />
            <input className="w-full rounded-lg border border-line px-3 py-2 text-sm" placeholder="Tipo (ex: INSURED_BY)" value={relType} onChange={(e) => setRelType(e.target.value)} />
            <button disabled={!relTo || relMutation.isPending} onClick={() => relMutation.mutate()} className="rounded-lg border border-line px-3 py-2 text-sm disabled:opacity-50">Adicionar relação</button>
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
        <h2 className="font-medium">Histórico</h2>
        <ul className="mt-4 space-y-2 text-sm">
          {historyQuery.data?.length === 0 && <li className="text-muted">Sem eventos.</li>}
          {historyQuery.data?.map((e) => (
            <li key={e.id} className="flex flex-col gap-1 border-b border-line pb-2 md:flex-row md:items-center md:justify-between">
              <div><span className="font-medium">{e.action}</span> <span className="text-muted">{e.entityType}</span></div>
              <div className="text-xs text-muted">{new Date(e.occurredAt).toLocaleString()}</div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
