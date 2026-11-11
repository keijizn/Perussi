import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { api } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useNotification } from '../contexts/NotificationContext'

type Situacao = { email: string; confirmado: boolean; envioDisponivel: boolean }
export default function ConfirmacaoEmail({ onChange }: { onChange?: (confirmado: boolean) => void }) {
  const { user } = useAuth()
  const notificar = useNotification()
  const [situacao, setSituacao] = useState<Situacao | null>(null)
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [aguardarAte, setAguardarAte] = useState(0)
  const [agora, setAgora] = useState(Date.now())
  const [consultando, setConsultando] = useState(false)
  const [tentativa, setTentativa] = useState(0)
  const consultar = useCallback(() => setTentativa(atual => atual + 1), [])

  useEffect(() => {
    let ativo = true
    setConsultando(true)
    setSituacao(null)
    onChange?.(false)
    api.get<Situacao>('/usuarios/me/confirmacao-email').then(({ data }) => {
      if (!ativo) return
      setSituacao(data)
      setErro('')
      onChange?.(data.confirmado)
    }).catch(() => {
      if (ativo) setErro('Não foi possível consultar a confirmação. Tente atualizar a situação.')
    }).finally(() => { if (ativo) setConsultando(false) })
    return () => { ativo = false }
  }, [user?.email, tentativa, onChange])

  useEffect(() => {
    window.addEventListener('focus', consultar)
    return () => window.removeEventListener('focus', consultar)
  }, [consultar])
  useEffect(() => {
    if (!aguardarAte) return
    const timer = window.setInterval(() => {
      const instante = Date.now()
      setAgora(instante)
      if (instante >= aguardarAte) setAguardarAte(0)
    }, 1000)
    return () => window.clearInterval(timer)
  }, [aguardarAte])

  async function reenviar() {
    if (enviando || aguardarAte > Date.now()) return
    setEnviando(true)
    setErro('')
    try {
      const { data } = await api.post('/usuarios/me/confirmacao-email')
      notificar(data.message)
      setAgora(Date.now())
      setAguardarAte(Date.now() + 60000)
    } catch (error) {
      setErro(isAxiosError(error) ? error.response?.data?.erro || 'Não foi possível enviar. Tente novamente mais tarde.' : 'Não foi possível enviar o link.')
    } finally { setEnviando(false) }
  }
  const segundos = Math.max(0, Math.ceil((aguardarAte - agora) / 1000))
  return <aside className="mb-8 rounded-2xl border border-black/10 bg-white/70 p-5 dark:border-white/10 dark:bg-white/5" aria-label="Confirmação do e-mail">
    <p className="font-medium">{situacao?.confirmado ? 'E-mail confirmado' : 'Confirme seu e-mail'}</p>
    {situacao?.confirmado ? <p className="mt-2 break-all text-sm">{situacao.email}</p> : <>
      <p className="mt-2 text-sm">Para finalizar a compra, confirme seu e-mail pelo link enviado após o cadastro. Seus itens permanecem no carrinho.</p>
      {situacao && <p className="mt-2 break-all text-sm">{situacao.email}</p>}
      {situacao && !situacao.envioDisponivel && <p className="mt-2 text-sm">O envio está temporariamente indisponível. Tente novamente mais tarde.</p>}
      <div className="mt-4 flex flex-wrap items-center gap-4 text-sm">
        <button type="button" onClick={reenviar} disabled={enviando || segundos > 0 || !situacao?.envioDisponivel} className="rounded-full bg-zinc-950 px-5 py-3 text-white disabled:opacity-50 dark:bg-white dark:text-zinc-950">{enviando ? 'Enviando...' : segundos ? `Reenviar em ${segundos}s` : 'Reenviar confirmação'}</button>
        <button type="button" onClick={consultar} disabled={consultando} className="underline disabled:opacity-50">{consultando ? 'Verificando...' : 'Já confirmei / atualizar situação'}</button>
        <Link to="/perfil" className="underline">Corrigir e-mail no perfil</Link>
      </div>
    </>}
    {erro && <p role="alert" className="mt-3 text-sm text-red-700 dark:text-red-300">{erro}</p>}
  </aside>
}
