import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

type Tipo = 'sucesso' | 'erro'
type Aviso = { id: number; texto: string; tipo: Tipo }
const NotificationContext = createContext<((texto: string, tipo?: Tipo) => void) | null>(null)

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const [aviso, setAviso] = useState<Aviso | null>(null)
  const [pausado, setPausado] = useState(false)
  const sequencia = useRef(0)
  const notificar = useCallback((texto: string, tipo: Tipo = 'sucesso') => {
    setPausado(false)
    setAviso({ id: ++sequencia.current, texto, tipo })
  }, [])

  useEffect(() => {
    if (!aviso || pausado) return
    const timer = window.setTimeout(() => setAviso(null), aviso.tipo === 'erro' ? 8000 : 5000)
    return () => window.clearTimeout(timer)
  }, [aviso, pausado])

  return (
    <NotificationContext.Provider value={notificar}>
      {children}
      <div style={{ top: 'var(--navbar-height, 0px)' }} className="pointer-events-none fixed inset-x-0 z-[100] flex justify-end p-4 sm:left-auto sm:w-[28rem]">
        <div role="status" aria-live="polite" aria-atomic="true" className="w-full max-w-md">
          {aviso && (
            <div key={aviso.id} className="perussi-notification pointer-events-auto flex items-start gap-3 rounded-2xl border border-black/10 bg-white p-4 text-zinc-950 shadow-xl dark:border-white/20 dark:bg-zinc-900 dark:text-white"
              onMouseEnter={() => setPausado(true)} onMouseLeave={() => setPausado(false)}
              onFocus={() => setPausado(true)} onBlur={() => setPausado(false)}>
              <span aria-hidden="true" className={aviso.tipo === 'erro' ? 'text-red-500' : 'text-emerald-600 dark:text-emerald-400'}>{aviso.tipo === 'erro' ? '!' : '✓'}</span>
              <p className="min-w-0 flex-1 break-words text-sm leading-6">{aviso.texto}</p>
              <button type="button" aria-label="Fechar notificação" className="-m-2 flex h-10 w-10 shrink-0 items-center justify-center rounded-full hover:bg-black/5 focus-visible:outline-2 dark:hover:bg-white/10" onClick={() => { setAviso(null); setPausado(false) }}>×</button>
            </div>
          )}
        </div>
      </div>
    </NotificationContext.Provider>
  )
}

export function useNotification() {
  const context = useContext(NotificationContext)
  if (!context) throw new Error('useNotification deve ser usado dentro de NotificationProvider')
  return context
}
