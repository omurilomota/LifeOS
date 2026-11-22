export type ApiError = {
  code: string
  message: string
  details?: { field: string; message: string }[]
  traceId?: string
  timestamp?: string
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
let csrfToken: string | undefined

function getCookie(name: string): string | undefined {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) return parts.pop()?.split(';').shift()
  return undefined
}

function captureCsrf(res: Response) {
  const token = res.headers.get('X-XSRF-TOKEN')
  if (token) csrfToken = token
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    let csrf = csrfToken ?? getCookie('XSRF-TOKEN')
    if (!csrf) {
      const pre = await fetch(`${API_BASE}/auth/me`, { credentials: 'include' }).catch(() => undefined)
      if (pre) captureCsrf(pre)
      csrf = csrfToken ?? getCookie('XSRF-TOKEN')
    }
    if (csrf) headers.set('X-XSRF-TOKEN', decodeURIComponent(csrf))
  }

  const res = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    ...options,
    headers,
  })
  captureCsrf(res)

  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    throw (data ?? { code: 'UNKNOWN_ERROR', message: res.statusText }) as ApiError
  }
  return data as T
}
