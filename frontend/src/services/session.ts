import { CanceledError, type AxiosInstance } from 'axios'

export const SESSION_EXPIRED = 'perussi:session-expired'

// Used only for the UI clock. The server still verifies the JWT signature.
export function expirationTime(token: string): number {
  try {
    const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '=')))
    return typeof payload.exp === 'number' && Number.isFinite(payload.exp)
      ? payload.exp * 1000 : 0
  } catch { return 0 }
}

export function expireSession(token: string) {
  // A delayed response from an older session must not log out a new session.
  if (localStorage.getItem('token') !== token) return
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.dispatchEvent(new Event(SESSION_EXPIRED))
}

export function installSessionInterceptors(client: AxiosInstance) {
  client.interceptors.request.use((config) => {
    if (config.url?.startsWith('/auth/')) return config
    const token = localStorage.getItem('token')
    if (token) {
      if (expirationTime(token) <= Date.now()) {
        expireSession(token)
        throw new CanceledError('Sessão expirada')
      }
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })
  client.interceptors.response.use(response => response, error => {
    const token = localStorage.getItem('token')
    if (error.response?.status === 401 && token && !error.config?.url?.startsWith('/auth/')
        && error.config?.headers?.Authorization === `Bearer ${token}`) {
      expireSession(token)
    }
    return Promise.reject(error)
  })
}
