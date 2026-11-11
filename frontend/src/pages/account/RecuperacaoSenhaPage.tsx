import { useNotification } from '../../contexts/NotificationContext'
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { api } from '../../services/api'
import { useAuth } from '../../contexts/AuthContext'

export default function RecuperacaoSenhaPage({ redefinir = false }: { redefinir?: boolean }) {
  const notificar = useNotification()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [token] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('token') ?? '')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState(false)
  useEffect(() => {
    if (redefinir && window.location.hash) window.history.replaceState(window.history.state, '', window.location.pathname)
  }, [redefinir])
  const tokenValido = /^[A-Za-z0-9_-]{43}$/.test(token)
  async function enviar(event: FormEvent) {
    event.preventDefault()
    if (ocupado) return
    setErro('')
    if (redefinir && senha !== confirmacao) { setErro('As senhas precisam ser iguais.'); return }
    if (redefinir && new TextEncoder().encode(senha).length > 72) { setErro('A senha é longa demais. Use até 72 bytes.'); return }
    setOcupado(true)
    try {
      if (redefinir) {
        await api.post('/auth/redefinir-senha', { token, senha })
        logout()
        notificar('Senha alterada com sucesso. Entre com sua nova senha.')
        navigate('/login', { replace: true, state: { senhaRedefinida: true } })
      } else {
        const result = await api.post('/auth/esqueci-senha', { email: email.trim() })
        setMensagem(result.data.message)
        notificar('Solicitação recebida. Se o e-mail estiver cadastrado, você receberá o link.')
      }
    } catch (error) {
      const status = axios.isAxiosError(error) ? error.response?.status : undefined
      setErro(status === 429 ? 'Muitas tentativas. Aguarde 15 minutos.' : status === 503 ? 'Recuperação de senha temporariamente indisponível. Tente novamente mais tarde.' : redefinir && status === 400 ? 'Link inválido, expirado ou já utilizado. Solicite um novo link.' : 'Não foi possível concluir. Tente novamente.')
    } finally { setOcupado(false) }
  }
  const input = 'mt-2 w-full rounded-2xl border border-black/10 bg-white px-4 py-3 dark:border-white/10 dark:bg-zinc-900'
  return <section className="mx-auto max-w-lg px-6 py-16">
    <h1 className="font-display text-4xl">{redefinir ? 'Criar nova senha' : 'Esqueci minha senha'}</h1>
    <p className="mt-4">{redefinir ? 'Escolha uma senha com pelo menos 6 caracteres.' : 'Informe o email da sua conta para receber um link válido por 15 minutos.'}</p>
    {redefinir && !tokenValido ? <p role="alert" className="mt-6">Link inválido. Solicite um novo link abaixo.</p> :
      <form onSubmit={enviar} className="mt-6 space-y-5">
        {redefinir ? <>
          <label className="block">Nova senha<input className={input} type="password" autoComplete="new-password" required minLength={6} maxLength={72} value={senha} onChange={e => setSenha(e.target.value)} /></label>
          <label className="block">Confirme a nova senha<input className={input} type="password" autoComplete="new-password" required minLength={6} maxLength={72} value={confirmacao} onChange={e => setConfirmacao(e.target.value)} /></label>
        </> : <label className="block">Email<input className={input} type="email" autoComplete="email" required maxLength={254} value={email} onChange={e => setEmail(e.target.value)} /></label>}
        {erro && <p role="alert">{erro}</p>}
        {mensagem && <p role="status">{mensagem}</p>}
        <button disabled={ocupado} className="w-full rounded-full bg-zinc-950 px-6 py-3 text-white disabled:opacity-50 dark:bg-white dark:text-zinc-950">{ocupado ? 'Aguarde...' : redefinir ? 'Salvar nova senha' : 'Enviar link'}</button>
      </form>}
    <div className="mt-6 flex flex-wrap gap-6 text-sm underline">
      <Link to="/login">Voltar ao login</Link>
      {redefinir && <Link to="/esqueci-senha">Solicitar novo link</Link>}
    </div>
  </section>
}
