import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { register } = useAuth()

  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')

    if (!nome || !email || !senha || !confirmarSenha) {
      setErro('Preencha todos os campos.')
      return
    }

    if (senha !== confirmarSenha) {
      setErro('As senhas não coincidem.')
      return
    }

    try {
      setCarregando(true)

      await register({
        nome,
        email,
        senha,
      })

      navigate('/login')
    } catch {
      setErro('Não foi possível criar a conta.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <section className="mx-auto grid min-h-[calc(100vh-81px)] max-w-7xl grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-2">
      <div>
        <p className="mb-4 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Crie sua conta
        </p>

        <h1 className="font-display max-w-xl text-5xl font-semibold leading-tight md:text-6xl">
          Comece sua experiência Perussi.
        </h1>

        <p className="mt-6 max-w-lg text-lg leading-8 text-zinc-600 dark:text-zinc-300">
          Salve favoritos, acompanhe seus pedidos e finalize suas compras com
          mais praticidade.
        </p>
      </div>

      <div className="rounded-[2rem] border border-black/10 bg-white/70 p-8 shadow-2xl shadow-black/10 backdrop-blur-md dark:border-white/10 dark:bg-white/5">
        <h2 className="font-display text-3xl font-semibold">Cadastro</h2>

        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <div>
            <label className="mb-2 block text-sm font-medium">Nome</label>
            <input
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              placeholder="Seu nome"
              className="w-full rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium">Email</label>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="seuemail@exemplo.com"
              className="w-full rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium">Senha</label>
            <input
              type="password"
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
              placeholder="Digite sua senha"
              className="w-full rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium">
              Confirmar senha
            </label>
            <input
              type="password"
              value={confirmarSenha}
              onChange={(event) => setConfirmarSenha(event.target.value)}
              placeholder="Repita sua senha"
              className="w-full rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />
          </div>

          {erro && (
            <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={carregando}
            className="w-full rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-zinc-950 dark:hover:bg-[#e8c9bd]"
          >
            {carregando ? 'Criando conta...' : 'Criar conta'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-zinc-600 dark:text-zinc-300">
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-[#b98b7d]">
            Entrar
          </Link>
        </p>
      </div>
    </section>
  )
}