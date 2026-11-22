import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '@/features/auth/useAuth'
import { getDashboard } from './dashboardApi'

export function TodayPage() {
  const { auth } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-accent">Hoje</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">Bom ver você, {auth?.user.displayName?.split(' ')[0]}.</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted">Visão operacional: o que está atrasado, o que vence em breve e o que mudou recentemente.</p>
      </div>

      {isLoading && <div className="text-sm text-muted">Carregando...</div>}

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Atrasadas</p><p className="mt-2 text-2xl font-semibold">{data?.overdue.length ?? 0}</p></div>
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Vencem em 7 dias</p><p className="mt-2 text-2xl font-semibold">{data?.dueSoon.length ?? 0}</p></div>
        <div className="rounded-2xl border border-line bg-white p-5 shadow-panel"><p className="text-sm text-muted">Notificações</p><p className="mt-2 text-2xl font-semibold">{data?.notifications.length ?? 0}</p></div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
          <div className="flex items-center justify-between"><h2 className="font-medium">Atrasadas / Próximas</h2><Link to="/obligations" className="text-sm text-accent">Ver todas</Link></div>
          <ul className="mt-4 space-y-2 text-sm">
            {[...(data?.overdue ?? []), ...(data?.dueSoon ?? [])].slice(0, 6).map((o) => (
              <li key={o.id} className="flex items-center justify-between rounded-lg bg-canvas px-3 py-2">
                <span>{o.title}</span><span className="text-xs text-muted">{new Date(o.dueAt).toLocaleDateString()}</span>
              </li>
            ))}
            {(data?.overdue.length ?? 0) === 0 && (data?.dueSoon.length ?? 0) === 0 && <li className="text-muted">Nada urgente.</li>}
          </ul>
        </section>

        <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
          <div className="flex items-center justify-between"><h2 className="font-medium">Registros recentes</h2><Link to="/records" className="text-sm text-accent">Ver todos</Link></div>
          <ul className="mt-4 space-y-2 text-sm">
            {data?.recentRecords.map((r) => <li key={r.id} className="rounded-lg bg-canvas px-3 py-2"><Link to={`/records/${r.id}`} className="hover:text-accent">{r.title}</Link></li>)}
            {(data?.recentRecords.length ?? 0) === 0 && <li className="text-muted">Sem registros.</li>}
          </ul>
        </section>
      </div>

      <section className="rounded-2xl border border-line bg-white p-5 shadow-panel">
        <h2 className="font-medium">Notificações</h2>
        <ul className="mt-4 space-y-2 text-sm">
          {data?.notifications.slice(0, 8).map((n) => (
            <li key={n.id} className="flex items-center justify-between border-b border-line pb-2">
              <div><p className="font-medium">{n.title}</p><p className="text-muted">{n.body}</p></div>
              <span className="text-xs text-muted">{new Date(n.createdAt).toLocaleString()}</span>
            </li>
          ))}
          {(data?.notifications.length ?? 0) === 0 && <li className="text-muted">Sem notificações.</li>}
        </ul>
      </section>
    </div>
  )
}
