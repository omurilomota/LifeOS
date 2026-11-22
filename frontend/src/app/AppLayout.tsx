import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '@/features/auth/useAuth'

const navItems = [
  { to: '/', label: 'Hoje' },
  { to: '/inbox', label: 'Inbox' },
  { to: '/records', label: 'Registros' },
  { to: '/documents', label: 'Documentos' },
  { to: '/obligations', label: 'Obrigações' },
  { to: '/search', label: 'Busca' },
  { to: '/household', label: 'Espaço' },
  { to: '/settings', label: 'Configurações' },
]

export function AppLayout() {
  const { auth, logout } = useAuth()
  const [q, setQ] = useState('')
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-canvas text-ink md:grid md:grid-cols-[240px_1fr]">
      <aside className="border-b border-line bg-white md:min-h-screen md:border-b-0 md:border-r">
        <div className="px-5 py-5">
          <p className="text-lg font-semibold tracking-tight">LifeOS</p>
          <p className="mt-1 text-xs text-muted">{auth?.currentHousehold.name}</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:block md:space-y-1 md:px-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-lg px-3 py-2 text-sm ${isActive ? 'bg-accentSoft text-accent' : 'text-muted hover:bg-canvas hover:text-ink'}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0">
        <header className="flex flex-col gap-3 border-b border-line bg-white/80 px-4 py-4 backdrop-blur md:flex-row md:items-center md:justify-between md:px-8">
          <div>
            <p className="text-sm text-muted">Espaço atual</p>
            <p className="font-medium">{auth?.currentHousehold.name}</p>
          </div>
          <form className="flex w-full max-w-md gap-2" onSubmit={(e) => { e.preventDefault(); if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`) }}>
            <input className="w-full rounded-lg border border-line px-3 py-2 text-sm" placeholder="Buscar..." value={q} onChange={(e) => setQ(e.target.value)} />
          </form>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-medium">{auth?.user.displayName}</p>
              <p className="text-xs text-muted">{auth?.user.email}</p>
            </div>
            <button onClick={() => void logout()} className="rounded-lg border border-line px-3 py-2 text-sm text-muted hover:text-ink">
              Sair
            </button>
          </div>
        </header>
        <main className="px-4 py-6 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
