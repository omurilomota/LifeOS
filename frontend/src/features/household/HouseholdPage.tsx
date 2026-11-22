import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { acceptInvite, getHouseholdDetail, inviteMember, removeMember, revokeInvite, updateMemberRole } from './householdApi'
import type { ApiError } from '@/shared/api/client'

export function HouseholdPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['household-detail'], queryFn: getHouseholdDetail })
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('MEMBER')
  const [token, setToken] = useState('')
  const [inviteToken, setInviteToken] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refresh = async () => { await qc.invalidateQueries({ queryKey: ['household-detail'] }) }
  const inviteMut = useMutation({
    mutationFn: () => inviteMember({ email, role }),
    onSuccess: async (res) => { setEmail(''); setInviteToken(res.token); setError(null); await refresh() },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao convidar'),
  })
  const revokeMut = useMutation({ mutationFn: revokeInvite, onSuccess: refresh })
  const acceptMut = useMutation({
    mutationFn: () => acceptInvite(token),
    onSuccess: async () => { setToken(''); setError(null); await refresh(); await qc.invalidateQueries({ queryKey: ['dashboard'] }) },
    onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao aceitar convite'),
  })
  const roleMut = useMutation({ mutationFn: ({ id, role }: { id: string; role: string }) => updateMemberRole(id, role), onSuccess: refresh, onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao alterar papel') })
  const removeMut = useMutation({ mutationFn: removeMember, onSuccess: refresh, onError: (err: unknown) => setError((err as ApiError)?.message ?? 'Falha ao remover membro') })

  const canManage = data?.myRole === 'OWNER' || data?.myRole === 'ADMIN'
  const isOwner = data?.myRole === 'OWNER'

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-accent">Espaço</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">{data?.name ?? 'Espaço'}</h1>
        <p className="mt-2 text-sm text-muted">Gerencie membros, papéis e convites.</p>
      </div>

      {isLoading && <div className="text-sm text-muted">Carregando...</div>}
      {error && <p className="text-sm text-danger">{error}</p>}

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
        <h2 className="font-medium">Membros</h2>
        <ul className="mt-4 divide-y divide-line">
          {data?.members.map((m) => (
            <li key={m.membershipId} className="flex flex-col gap-3 py-3 text-sm md:flex-row md:items-center md:justify-between">
              <div><p className="font-medium">{m.displayName}</p><p className="text-muted">{m.email}</p></div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-canvas px-2 py-1 text-xs text-muted">{m.role}</span>
                {isOwner && m.role !== 'OWNER' && (
                  <>
                    <select className="rounded-lg border border-line px-2 py-1 text-xs" value={m.role} onChange={(e) => roleMut.mutate({ id: m.membershipId, role: e.target.value })}>
                      <option value="ADMIN">ADMIN</option><option value="MEMBER">MEMBER</option><option value="VIEWER">VIEWER</option>
                    </select>
                    <button onClick={() => { if (confirm('Remover membro?')) removeMut.mutate(m.membershipId) }} className="rounded-lg border border-line px-2 py-1 text-xs text-danger">Remover</button>
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4">
        <h2 className="font-medium">Convidar membro</h2>
        {canManage ? (
          <div className="space-y-3">
            <div className="flex flex-col gap-2 md:flex-row">
              <input className="flex-1 rounded-lg border border-line px-3 py-2 text-sm" placeholder="email@exemplo.com" value={email} onChange={(e) => setEmail(e.target.value)} />
              <select className="rounded-lg border border-line px-3 py-2 text-sm" value={role} onChange={(e) => setRole(e.target.value)}>
                <option value="ADMIN">ADMIN</option><option value="MEMBER">MEMBER</option><option value="VIEWER">VIEWER</option>
              </select>
              <button disabled={!email || inviteMut.isPending} onClick={() => inviteMut.mutate()} className="rounded-lg bg-accent px-4 py-2 text-sm text-white disabled:opacity-60">Convidar</button>
            </div>
            {inviteToken && <div className="rounded-lg bg-accentSoft p-3 text-xs text-accent break-all">Token de convite (entrega manual no baseline): {inviteToken}</div>}
          </div>
        ) : <p className="text-sm text-muted">Apenas OWNER/ADMIN podem convidar.</p>}
      </section>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-4">
        <h2 className="font-medium">Aceitar convite</h2>
        <div className="flex flex-col gap-2 md:flex-row">
          <input className="flex-1 rounded-lg border border-line px-3 py-2 text-sm" placeholder="Cole o token do convite" value={token} onChange={(e) => setToken(e.target.value)} />
          <button disabled={!token || acceptMut.isPending} onClick={() => acceptMut.mutate()} className="rounded-lg border border-line px-4 py-2 text-sm disabled:opacity-60">Aceitar</button>
        </div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
        <h2 className="font-medium">Convites</h2>
        <ul className="mt-4 space-y-2 text-sm">
          {data?.invitations.map((inv) => (
            <li key={inv.id} className="flex flex-col gap-2 rounded-lg bg-canvas px-3 py-2 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-medium">{inv.invitedEmail} <span className="text-muted">({inv.role})</span></p>
                <p className="text-xs text-muted">Expira: {new Date(inv.expiresAt).toLocaleString()} {inv.acceptedAt ? '· Aceito' : inv.revokedAt ? '· Revogado' : '· Pendente'}</p>
              </div>
              {canManage && !inv.acceptedAt && !inv.revokedAt && <button onClick={() => revokeMut.mutate(inv.id)} className="rounded-lg border border-line px-3 py-2 text-xs">Revogar</button>}
            </li>
          ))}
          {(data?.invitations.length ?? 0) === 0 && <li className="text-muted">Sem convites.</li>}
        </ul>
      </section>
    </div>
  )
}
