import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelObligation, completeObligation, createObligation, listObligations, rescheduleObligation } from './obligationsApi'
import type { ObligationPriority, ObligationStatus } from '@/shared/types/obligation'
import type { ApiError } from '@/shared/api/client'

const empty = { title: '', description: '', dueAt: '', priority: 'MEDIUM' as ObligationPriority, status: 'OPEN' as ObligationStatus, recurrenceRule: '' }

export function ObligationsPage() {
  const qc = useQueryClient()
  const [status, setStatus] = useState<ObligationStatus | ''>('')
  const [form, setForm] = useState(empty)
  const [error, setError] = useState<string | null>(null)
  const [rescheduleId, setRescheduleId] = useState<string | null>(null)
  const [rescheduleDueAt, setRescheduleDueAt] = useState('')

  const query = useQuery({ queryKey: ['obligations', status], queryFn: () => listObligations(status || undefined) })
  const invalidate = async () => { await qc.invalidateQueries({ queryKey: ['obligations'] }); await qc.invalidateQueries({ queryKey: ['dashboard'] }) }

  const createMut = useMutation({
    mutationFn: () => createObligation({ ...form, dueAt: new Date(form.dueAt).toISOString(), recurrenceRule: form.recurrenceRule || undefined }),
    onSuccess: async () => { setForm(empty); setError(null); await invalidate() },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao criar obrigação'),
  })
  const completeMut = useMutation({ mutationFn: completeObligation, onSuccess: invalidate })
  const cancelMut = useMutation({ mutationFn: cancelObligation, onSuccess: invalidate })
  const rescheduleMut = useMutation({
    mutationFn: ({ id, dueAt }: { id: string; dueAt: string }) => rescheduleObligation(id, new Date(dueAt).toISOString()),
    onSuccess: async () => { setRescheduleId(null); setRescheduleDueAt(''); await invalidate() },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao reagendar'),
  })

  const rows = useMemo(() => query.data ?? [], [query.data])

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-accent">Obrigações</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">O que precisa ser resolvido</h1>
        <p className="mt-2 text-sm text-muted">Acompanhe prazos, recorrência, responsáveis e status.</p>
      </div>

      <form className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4" onSubmit={(e) => { e.preventDefault(); setError(null); createMut.mutate() }}>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="text-sm"><span className="mb-1 block text-muted">Título</span><input className="w-full rounded-lg border border-line px-3 py-2" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></label>
          <label className="text-sm"><span className="mb-1 block text-muted">Vencimento</span><input type="datetime-local" className="w-full rounded-lg border border-line px-3 py-2" value={form.dueAt} onChange={(e) => setForm({ ...form, dueAt: e.target.value })} required /></label>
          <label className="text-sm"><span className="mb-1 block text-muted">Prioridade</span><select className="w-full rounded-lg border border-line px-3 py-2" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as ObligationPriority })}><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option></select></label>
          <label className="text-sm"><span className="mb-1 block text-muted">Recorrência</span><select className="w-full rounded-lg border border-line px-3 py-2" value={form.recurrenceRule} onChange={(e) => setForm({ ...form, recurrenceRule: e.target.value })}><option value="">Nenhuma</option><option value="DAILY">DAILY</option><option value="WEEKLY">WEEKLY</option><option value="MONTHLY">MONTHLY</option><option value="YEARLY">YEARLY</option></select></label>
        </div>
        <label className="block text-sm"><span className="mb-1 block text-muted">Descrição</span><textarea className="w-full rounded-lg border border-line px-3 py-2" rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
        {error && <p className="text-sm text-danger">{error}</p>}
        <button disabled={createMut.isPending} className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{createMut.isPending ? 'Salvando...' : 'Criar obrigação'}</button>
      </form>

      <div className="flex gap-3">
        <select className="rounded-lg border border-line px-3 py-2 text-sm" value={status} onChange={(e) => setStatus(e.target.value as ObligationStatus | '')}>
          <option value="">Todos</option><option value="OPEN">OPEN</option><option value="COMPLETED">COMPLETED</option><option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      <div className="rounded-2xl border border-line bg-white shadow-panel overflow-hidden">
        {query.isLoading && <div className="p-6 text-sm text-muted">Carregando...</div>}
        {!query.isLoading && rows.length === 0 && <div className="p-8 text-sm text-muted">Nenhuma obrigação.</div>}
        <ul className="divide-y divide-line">
          {rows.map((o) => (
            <li key={o.id} className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-medium">{o.title}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-xs text-muted">
                  <span className="rounded-full bg-canvas px-2 py-1">{o.priority}</span>
                  <span className="rounded-full bg-canvas px-2 py-1">{o.status}</span>
                  <span className="rounded-full bg-canvas px-2 py-1">Vence: {new Date(o.dueAt).toLocaleString()}</span>
                  {o.recurrenceRule && <span className="rounded-full bg-accentSoft px-2 py-1 text-accent">{o.recurrenceRule}</span>}
                </div>
                {o.description && <p className="mt-1 text-sm text-muted">{o.description}</p>}
                {rescheduleId === o.id && (
                  <div className="mt-2 flex gap-2">
                    <input type="datetime-local" className="rounded-lg border border-line px-3 py-2 text-sm" value={rescheduleDueAt} onChange={(e) => setRescheduleDueAt(e.target.value)} />
                    <button disabled={!rescheduleDueAt || rescheduleMut.isPending} onClick={() => rescheduleMut.mutate({ id: o.id, dueAt: rescheduleDueAt })} className="rounded-lg bg-accent px-3 py-2 text-sm text-white disabled:opacity-60">Salvar</button>
                  </div>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {o.status === 'OPEN' && <button onClick={() => completeMut.mutate(o.id)} className="rounded-lg border border-line px-3 py-2 text-sm">Concluir</button>}
                {o.status === 'OPEN' && <button onClick={() => { setRescheduleId(rescheduleId === o.id ? null : o.id); setRescheduleDueAt('') }} className="rounded-lg border border-line px-3 py-2 text-sm">Reagendar</button>}
                {o.status === 'OPEN' && <button onClick={() => cancelMut.mutate(o.id)} className="rounded-lg border border-line px-3 py-2 text-sm text-danger">Cancelar</button>}
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
