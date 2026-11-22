import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { ApiError } from '@/shared/api/client'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  return (
    <div className="min-h-screen bg-canvas px-4 py-10 text-ink">
      <div className="mx-auto max-w-md rounded-2xl border border-line bg-white p-6 shadow-panel">
        <p className="text-sm font-medium text-accent">LifeOS</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">Entrar</h1>
        <p className="mt-2 text-sm text-muted">Acesse seu espaço pessoal/familiar.</p>
        <form
          className="mt-6 space-y-4"
          onSubmit={async (e) => {
            e.preventDefault()
            setError(null)
            setSubmitting(true)
            try {
              await login(email, password)
              navigate('/')
            } catch (err: unknown) {
              const apiErr = err as ApiError
              setError(apiErr?.message ?? 'Falha ao entrar')
            } finally {
              setSubmitting(false)
            }
          }}
        >
          <label className="block text-sm">
            <span className="mb-1 block text-muted">Email</span>
            <input className="w-full rounded-lg border border-line px-3 py-2 outline-none focus:border-accent" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-muted">Senha</span>
            <input className="w-full rounded-lg border border-line px-3 py-2 outline-none focus:border-accent" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <p className="text-sm text-danger" role="alert">{error}</p>}
          <button disabled={submitting} className="w-full rounded-lg bg-accent px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60">
            {submitting ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
        <p className="mt-4 text-sm text-muted">
          Não tem conta? <Link className="text-accent underline-offset-2 hover:underline" to="/register">Criar conta</Link>
        </p>
      </div>
    </div>
  )
}
