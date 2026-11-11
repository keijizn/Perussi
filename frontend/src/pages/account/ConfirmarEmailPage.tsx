import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { api } from '../../services/api'
import { useNotification } from '../../contexts/NotificationContext'
import { useAuth } from '../../contexts/AuthContext'

export default function ConfirmarEmailPage() {
  const { isAuthenticated } = useAuth()
  const notificar = useNotification()
  const [token] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('token') || '')
  const [confirmado, setConfirmado] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const valido = /^[A-Za-z0-9_-]{43}$/.test(token)
  useEffect(() => {
    if (window.location.hash) window.history.replaceState(window.history.state, '', window.location.pathname)
  }, [])
  async function confirmar() {
    if (enviando || confirmado || !valido) return
    setEnviando(true)
    setErro('')
    try {
      await api.post('/auth/confirmar-email', { token })
      setConfirmado(true)
      notificar('E-mail confirmado com sucesso!')
    } catch (error) {
      setErro(isAxiosError(error) ? error.response?.data?.erro || 'Não foi possível confirmar. Tente novamente.' : 'Não foi possível confirmar.')
    } finally { setEnviando(false) }
  }
  return <section className="mx-auto max-w-xl px-6 py-16">
    <h1 className="font-display text-4xl">{confirmado ? 'E-mail confirmado!' : 'Confirme seu e-mail'}</h1>
    <p className="mt-5">{confirmado ? 'A confirmação foi concluída. Entre na conta deste e-mail para continuar sua compra.' : 'Clique abaixo para confirmar o e-mail da sua conta Perussi. O link vale por 30 minutos e pode ser usado uma vez.'}</p>
    {!valido && <p role="alert" className="mt-5">Link inválido. Entre na sua conta e peça outro link pelo perfil.</p>}
    {erro && <p role="alert" className="mt-5 text-red-700 dark:text-red-300">{erro}</p>}
    {!confirmado && valido && <button disabled={enviando} onClick={confirmar} className="mt-6 rounded-full bg-zinc-950 px-6 py-3 text-white disabled:opacity-50 dark:bg-white dark:text-zinc-950">{enviando ? 'Confirmando...' : 'Confirmar meu e-mail'}</button>}
    <div className="mt-6 flex gap-5 text-sm underline">
      <Link to={isAuthenticated ? '/perfil' : '/login'}>{isAuthenticated ? 'Ir ao perfil' : 'Entrar na conta'}</Link>
      {confirmado && isAuthenticated && <Link to="/carrinho">Voltar ao carrinho</Link>}
    </div>
  </section>
}
