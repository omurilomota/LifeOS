import { useState } from 'react'
import { useAuth } from '@/features/auth/useAuth'
import { deleteAccount, exportPrivacyData } from '@/features/privacy/privacyApi'

export function SettingsPage() {
  const { auth, logout } = useAuth()
  const [exportData, setExportData] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <p className="text-sm font-medium text-accent">Configurações</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">Privacidade e conta</h1>
      </div>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-3">
        <h2 className="font-medium">Perfil</h2>
        <div className="text-sm text-muted"><div>{auth?.user.displayName}</div><div>{auth?.user.email}</div><div>Locale: {auth?.user.locale} · TZ: {auth?.user.timezone}</div></div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel space-y-3">
        <h2 className="font-medium">Exportar dados (LGPD)</h2>
        <p className="text-sm text-muted">Exporta dados do seu espaço em JSON.</p>
        <button disabled={busy} className="rounded-lg border border-line px-4 py-2 text-sm disabled:opacity-60" onClick={async () => {
          setBusy(true); setError(null)
          try { setExportData(JSON.stringify(await exportPrivacyData(), null, 2)) }
          catch (e: unknown) { setError((e as { message?: string })?.message ?? 'Falha ao exportar') }
          finally { setBusy(false) }
        }}>Exportar</button>
        {exportData && <pre className="max-h-80 overflow-auto rounded-lg bg-canvas p-3 text-xs">{exportData}</pre>}
      </section>

      <section className="rounded-2xl border border-danger/30 bg-white p-5 shadow-panel space-y-3">
        <h2 className="font-medium text-danger">Excluir conta</h2>
        <p className="text-sm text-muted">Ação destrutiva. Remove dados do usuário e, se for o último membro, os dados do espaço.</p>
        <button disabled={busy} className="rounded-lg border border-danger px-4 py-2 text-sm text-danger disabled:opacity-60" onClick={async () => {
          if (!confirm('Excluir conta e dados? Esta ação não pode ser desfeita.')) return
          setBusy(true); setError(null)
          try { await deleteAccount(); await logout() }
          catch (e: unknown) { setError((e as { message?: string })?.message ?? 'Falha ao excluir conta') }
          finally { setBusy(false) }
        }}>Excluir minha conta</button>
      </section>

      {error && <p className="text-sm text-danger">{error}</p>}
    </div>
  )
}
