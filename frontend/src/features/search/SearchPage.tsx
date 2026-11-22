import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { search } from '@/features/dashboard/dashboardApi'

export function SearchPage() {
  const [params] = useSearchParams()
  const initial = params.get('q') ?? ''
  const [q, setQ] = useState(initial)
  const [term, setTerm] = useState(initial)
  const { data, isFetching } = useQuery({ queryKey: ['search', term], queryFn: () => search(term), enabled: term.length > 0 })

  useEffect(() => { setQ(initial); setTerm(initial) }, [initial])

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-accent">Busca</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">Encontre tudo</h1>
      </div>
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); setTerm(q.trim()) }}>
        <input className="flex-1 rounded-lg border border-line px-3 py-2" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar registros, documentos, obrigações" />
        <button className="rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white">Buscar</button>
      </form>
      {isFetching && <div className="text-sm text-muted">Buscando...</div>}
      {data && (
        <div className="grid gap-4 md:grid-cols-3">
          {[{ title: 'Registros', items: data.records, path: '/records/' }, { title: 'Documentos', items: data.documents, path: '/documents' }, { title: 'Obrigações', items: data.obligations, path: '/obligations' }].map((group) => (
            <section key={group.title} className="rounded-2xl border border-line bg-white p-5 shadow-panel">
              <h2 className="font-medium">{group.title}</h2>
              <ul className="mt-3 space-y-2 text-sm">
                {group.items.length === 0 && <li className="text-muted">Nenhum resultado.</li>}
                {group.items.map((item) => (
                  <li key={item.id} className="rounded-lg bg-canvas px-3 py-2">
                    {group.title === 'Registros' ? <Link to={`${group.path}${item.id}`} className="hover:text-accent">{item.title}</Link> : <span>{item.title}</span>}
                    {item.subtitle && <div className="text-xs text-muted">{item.subtitle}</div>}
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </div>
  )
}
