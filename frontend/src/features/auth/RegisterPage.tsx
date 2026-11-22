import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { ApiError } from '@/shared/api/client'

export function RegisterPage() {
  const { register, demo } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  return (
    <div className="min-h-screen bg-canvas px-4 py-10 text-ink">
      <div className="mx-auto max-w-md rounded-2xl border border-line bg-white p-6 shadow-panel">
        <p className="text-sm font-medium text-accent">LifeOS</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">Criar conta</h1>
        <p className="mt-2 text-sm text-muted">Crie sua conta e seu espaço pessoal inicial.</p>
        <form
          className="mt-6 space-y-4"
          onSubmit={async (e) => {
            e.preventDefault()
            setError(null)
            setSubmitting(true)
            try {
              await register(displayName, email, password)
              navigate('/')
            } catch (err: unknown) {
              const apiErr = err as ApiError
              setError(apiErr?.message ?? 'Falha ao criar conta')
            } finally {
              setSubmitting(false)
            }
          }}
        >
          <label className="block text-sm">
            <span className="mb-1 block text-muted">Nome</span>
            <input className="w-full rounded-lg border border-line px-3 py-2 outline-none focus:border-accent" value={displayName} onChange={(e) => setDisplayName(e.target.value)} required />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-muted">Email</span>
            <input className="w-full rounded-lg border border-line px-3 py-2 outline-none focus:border-accent" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-muted">Senha</span>
            <input className="w-full rounded-lg border border-line px-3 py-2 outline-none focus:border-accent" type="password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <p className="text-sm text-danger" role="alert">{error}</p>}
          <button disabled={submitting} className="w-full rounded-lg bg-accent px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60">
            {submitting ? 'Criando...' : 'Criar conta'}
          </button>
          <button type="button" disabled={submitting} className="w-full rounded-lg border border-line px-4 py-2.5 text-sm font-medium text-ink disabled:opacity-60" onClick={async () => {
            setError(null); setSubmitting(true)
            try { await demo(); navigate('/') }
            catch (err: unknown) { const apiErr = err as ApiError; setError(apiErr?.message ?? 'Falha ao entrar no modo demo') }
            finally { setSubmitting(false) }
          }}>
            Entrar sem conta (demo)
          </button>
        </form>
        <p className="mt-4 text-sm text-muted">
          Já tem conta? <Link className="text-accent underline-offset-2 hover:underline" to="/login">Entrar</Link>
        </p>
      </div>
    </div>
  )
}
