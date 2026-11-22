import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { archiveRecord, createRecord, listRecords } from './recordsApi'
import type { RecordRequest, RecordStatus, RecordType } from '@/shared/types/record'
import type { ApiError } from '@/shared/api/client'

const TYPE_OPTIONS: { value: RecordType; label: string }[] = [
  { value: 'ASSET', label: 'Ativo' },
  { value: 'VEHICLE', label: 'Veículo' },
  { value: 'PROPERTY', label: 'Imóvel' },
  { value: 'SERVICE', label: 'Serviço' },
  { value: 'SUBSCRIPTION', label: 'Assinatura' },
  { value: 'CONTRACT', label: 'Contrato' },
  { value: 'OTHER', label: 'Outro' },
]

const emptyForm: RecordRequest = { type: 'ASSET', title: '', description: '', category: '', status: 'ACTIVE' }

export function RecordsPage() {
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [status, setStatus] = useState<RecordStatus | ''>('')
  const [type, setType] = useState<RecordType | ''>('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<RecordRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  const params = useMemo(() => ({ q: q || undefined, status: status || undefined, type: type || undefined }), [q, status, type])
  const recordsQuery = useQuery({ queryKey: ['records', params], queryFn: () => listRecords(params) })

  const createMutation = useMutation({
    mutationFn: createRecord,
    onSuccess: async () => {
      setForm(emptyForm)
      setShowForm(false)
      setError(null)
      await qc.invalidateQueries({ queryKey: ['records'] })
    },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao criar registro'),
  })

  const archiveMutation = useMutation({
    mutationFn: archiveRecord,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: ['records'] }) },
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-sm font-medium text-accent">Registros</p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight">O que você administra</h1>
          <p className="mt-2 text-sm text-muted">Registre bens, serviços, contratos e outras informações importantes.</p>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="rounded-lg bg-accent px-4 py-2.5 text-sm font-medium text-white">
          {showForm ? 'Fechar' : 'Novo registro'}
        </button>
      </div>

      {showForm && (
        <form
          className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4"
          onSubmit={(e) => { e.preventDefault(); setError(null); createMutation.mutate({ ...form, title: form.title.trim() }) }}
        >
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm"><span className="mb-1 block text-muted">Título</span>
              <input className="w-full rounded-lg border border-line px-3 py-2" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={200} />
            </label>
            <label className="text-sm"><span className="mb-1 block text-muted">Tipo</span>
              <select className="w-full rounded-lg border border-line px-3 py-2" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as RecordType })}>
                {TYPE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>
            <label className="text-sm"><span className="mb-1 block text-muted">Categoria</span>
              <input className="w-full rounded-lg border border-line px-3 py-2" value={form.category ?? ''} onChange={(e) => setForm({ ...form, category: e.target.value })} />
            </label>
            <label className="text-sm"><span className="mb-1 block text-muted">Status</span>
              <select className="w-full rounded-lg border border-line px-3 py-2" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as RecordStatus })}>
                <option value="ACTIVE">Ativo</option><option value="ARCHIVED">Arquivado</option>
              </select>
            </label>
          </div>
          <label className="block text-sm"><span className="mb-1 block text-muted">Descrição</span>
            <textarea className="w-full rounded-lg border border-line px-3 py-2" rows={3} value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </label>
          {error && <p className="text-sm text-danger">{error}</p>}
          <div className="flex gap-2">
            <button disabled={createMutation.isPending} className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{createMutation.isPending ? 'Salvando...' : 'Salvar'}</button>
            <button type="button" onClick={() => setShowForm(false)} className="rounded-lg border border-line px-4 py-2 text-sm">Cancelar</button>
          </div>
        </form>
      )}

      <div className="grid gap-3 rounded-2xl border border-line bg-white p-4 shadow-panel md:grid-cols-[1fr_180px_180px]">
        <input className="rounded-lg border border-line px-3 py-2 text-sm" placeholder="Buscar por título, descrição ou categoria" value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="rounded-lg border border-line px-3 py-2 text-sm" value={status} onChange={(e) => setStatus(e.target.value as RecordStatus | '')}>
          <option value="">Todos status</option><option value="ACTIVE">Ativo</option><option value="ARCHIVED">Arquivado</option>
        </select>
        <select className="rounded-lg border border-line px-3 py-2 text-sm" value={type} onChange={(e) => setType(e.target.value as RecordType | '')}>
          <option value="">Todos tipos</option>
          {TYPE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      <div className="rounded-2xl border border-line bg-white shadow-panel overflow-hidden">
        {recordsQuery.isLoading && <div className="p-6 text-sm text-muted">Carregando...</div>}
        {recordsQuery.isError && <div className="p-6 text-sm text-danger">Falha ao carregar registros.</div>}
        {!recordsQuery.isLoading && (recordsQuery.data?.length ?? 0) === 0 && (
          <div className="p-8 text-sm text-muted">Nenhum registro encontrado. Crie o primeiro registro para começar.</div>
        )}
        <ul className="divide-y divide-line">
          {recordsQuery.data?.map((record) => (
            <li key={record.id} className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
              <div className="min-w-0">
                <Link to={`/records/${record.id}`} className="font-medium hover:text-accent">{record.title}</Link>
                <div className="mt-1 flex flex-wrap gap-2 text-xs text-muted">
                  <span className="rounded-full bg-canvas px-2 py-1">{record.type}</span>
                  {record.category && <span className="rounded-full bg-canvas px-2 py-1">{record.category}</span>}
                  <span className={`rounded-full px-2 py-1 ${record.status === 'ACTIVE' ? 'bg-accentSoft text-accent' : 'bg-canvas text-muted'}`}>{record.status}</span>
                </div>
              </div>
              <div className="flex gap-2">
                <Link to={`/records/${record.id}`} className="rounded-lg border border-line px-3 py-2 text-sm">Abrir</Link>
                {record.status !== 'ARCHIVED' && (
                  <button onClick={() => archiveMutation.mutate(record.id)} className="rounded-lg border border-line px-3 py-2 text-sm text-muted hover:text-danger">Arquivar</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
