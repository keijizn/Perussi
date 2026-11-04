import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { api } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'

type Usuario = {
  id: string
  nome: string
  email: string
  role: 'ADMIN' | 'CUSTOMER'
}

type Endereco = {
  cep: string
  logradouro: string
  numero: string
  bairro: string
  cidade: string
  estado: string
  complemento: string
}

type PedidoItem = {
  produtoId: string
  nomeProduto: string
  tamanho: string
  cor: string
  quantidade: number
  precoUnitario: number
}

type Pedido = {
  id: string
  total: number
  status: string
  criadoEm: string
  itens: PedidoItem[]
}

type Favorito = {
  id: string
  produtoId: string
}

type ViaCepResponse = {
  cep: string
  logradouro: string
  bairro: string
  localidade: string
  uf: string
  erro?: boolean
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const { logout, atualizarUsuario } = useAuth()
  const { quantidadeTotal } = useCart()

  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [pedidos, setPedidos] = useState<Pedido[]>([])
  const [favoritos, setFavoritos] = useState<Favorito[]>([])

  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [salvandoPerfil, setSalvandoPerfil] = useState(false)

  const [cep, setCep] = useState('')
  const [logradouro, setLogradouro] = useState('')
  const [numero, setNumero] = useState('')
  const [bairro, setBairro] = useState('')
  const [cidade, setCidade] = useState('')
  const [estado, setEstado] = useState('')
  const [complemento, setComplemento] = useState('')

  const [mensagem, setMensagem] = useState('')
  const [editando, setEditando] = useState(false)
  const [editandoEndereco, setEditandoEndereco] = useState(false)
  const [buscandoCep, setBuscandoCep] = useState(false)
  const [carregando, setCarregando] = useState(true)

  async function carregarPerfil() {
    try {
      const [perfilResponse, enderecoResponse, pedidosResponse, favoritosResponse] =
        await Promise.all([
          api.get('/usuarios/me'),
          api.get('/usuarios/me/endereco'),
          api.get('/pedidos/meus'),
          api.get('/favoritos'),
        ])

      setUsuario(perfilResponse.data)
      setNome(perfilResponse.data.nome)
      setEmail(perfilResponse.data.email)

      const endereco: Endereco = enderecoResponse.data

      setCep(endereco.cep || '')
      setLogradouro(endereco.logradouro || '')
      setNumero(endereco.numero || '')
      setBairro(endereco.bairro || '')
      setCidade(endereco.cidade || '')
      setEstado(endereco.estado || '')
      setComplemento(endereco.complemento || '')

      setPedidos(pedidosResponse.data)
      setFavoritos(favoritosResponse.data)
    } catch {
      setMensagem('Não foi possível carregar o perfil. Atualize a página para tentar novamente.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregarPerfil()
  }, [])

  async function buscarCep(valor: string) {
    const cepLimpo = valor.replace(/\D/g, '')

    setCep(valor)

    if (cepLimpo.length !== 8) return

    setBuscandoCep(true)
    setMensagem('')

    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`)
      const data: ViaCepResponse = await response.json()

      if (data.erro) {
        setMensagem('CEP não encontrado.')
        return
      }

      setLogradouro(data.logradouro || '')
      setBairro(data.bairro || '')
      setCidade(data.localidade || '')
      setEstado(data.uf || '')
    } catch {
      setMensagem('Não foi possível buscar o CEP.')
    } finally {
      setBuscandoCep(false)
    }
  }

  async function salvarPerfil() {
    if (salvandoPerfil) return
    setMensagem('')

    try {
      setSalvandoPerfil(true)
      const alterarAcesso = email !== usuario?.email || senha.length > 0
      const response = await api.put('/usuarios/me', {
        nome,
        email,
        senha: senha || null,
        senhaAtual: senhaAtual || null,
      })

      if (alterarAcesso) {
        logout()
        navigate('/login', { replace: true, state: { perfilAtualizado: true } })
        return
      }
      setUsuario(response.data)
      atualizarUsuario(response.data)
      setSenha('')
      setSenhaAtual('')
      setEditando(false)
      setMensagem('Perfil atualizado com sucesso.')
    } catch (error: any) {
      setMensagem(error.response?.data?.mensagens?.join(' ') || error.response?.data?.erro || 'Erro ao atualizar perfil.')
    } finally {
      setSalvandoPerfil(false)
    }
  }

  async function salvarEndereco(event: FormEvent) {
    event.preventDefault()
    setMensagem('')

    try {
      await api.put('/usuarios/me/endereco', {
        cep,
        logradouro,
        numero,
        bairro,
        cidade,
        estado,
        complemento,
      })

      setEditandoEndereco(false)
      setMensagem('Endereço padrão atualizado com sucesso.')
    } catch (error: any) {
      setMensagem(error.response?.data?.mensagens?.join(' ') || error.response?.data?.erro || 'Erro ao atualizar endereço.')
    }
  }

  async function excluirPerfil() {
    const confirmar = window.confirm(
      'Deseja realmente excluir sua conta? Essa ação não poderá ser desfeita.',
    )

    if (!confirmar) return

    try {
      await api.delete('/usuarios/me')
      logout()
      navigate('/')
    } catch {
      setMensagem('Erro ao excluir perfil.')
    }
  }

  const pedidosRecentes = pedidos.slice(0, 3)

  if (carregando) {
    return (
      <section className="mx-auto max-w-7xl px-6 py-14">
        <p className="text-zinc-500">Carregando perfil...</p>
      </section>
    )
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-10">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Minha conta
        </p>

        <h1 className="font-display text-5xl font-semibold">Perfil</h1>

        <p className="mt-3 text-zinc-600 dark:text-zinc-300">
          Gerencie sua conta, acompanhe seus pedidos e salve seu endereço
          padrão.
        </p>
      </div>

      {mensagem && (
        <div className="mb-8 rounded-2xl bg-[#f2ddd5] px-5 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
          {mensagem}
        </div>
      )}

      <div className="grid items-start gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-8">
          <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
            <h2 className="font-display mb-6 text-3xl font-semibold">
              Dados pessoais
            </h2>

            <div className="space-y-4">
              <input
                value={nome}
                onChange={(event) => setNome(event.target.value)}
                disabled={!editando}
                placeholder="Nome"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                disabled={!editando}
                placeholder="Email"
                type="email"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
              />

              {!editando && (
                <input
                  value="••••••••"
                  disabled
                  type="text"
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
                />
              )}

              {editando && (
                <>
                <label className="block text-sm">
                  Senha atual (obrigatória para trocar email ou senha)
                  <input value={senhaAtual} onChange={(event) => setSenhaAtual(event.target.value)} type="password" autoComplete="current-password"
                    className="mt-2 w-full rounded-xl border border-black/10 bg-white px-4 py-3 dark:border-white/10 dark:bg-zinc-900" />
                </label>
                <input
                  value={senha}
                  onChange={(event) => setSenha(event.target.value)}
                  placeholder="Nova senha opcional"
                  type="password"
                  autoComplete="new-password"
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
                />
                </>
              )}

              <div className="flex flex-wrap gap-3 pt-2">
                {!editando ? (
                  <button
                    type="button"
                    onClick={() => setEditando(true)}
                    className="rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
                  >
                    Editar perfil
                  </button>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={salvarPerfil}
                      disabled={salvandoPerfil}
                      className="rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
                    >
                      Salvar alterações
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setEditando(false)
                        setNome(usuario?.nome || '')
                        setEmail(usuario?.email || '')
                        setSenha('')
                        setSenhaAtual('')
                      }}
                      className="rounded-full border border-black/10 px-6 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                    >
                      Cancelar
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>

          <form
            onSubmit={salvarEndereco}
            className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5"
          >
            <div className="mb-6 flex items-center justify-between gap-4">
              <div>
                <h2 className="font-display text-3xl font-semibold">
                  Endereço padrão
                </h2>

                <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
                  Usado para agilizar suas próximas compras.
                </p>
              </div>

              {!editandoEndereco && (
                <button
                  type="button"
                  onClick={() => setEditandoEndereco(true)}
                  className="rounded-full border border-black/10 px-5 py-2 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                >
                  Editar
                </button>
              )}
            </div>

            <div className="space-y-4">
              <input
                value={cep}
                onChange={(event) => buscarCep(event.target.value)}
                disabled={!editandoEndereco}
                placeholder="CEP"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
              />

              {buscandoCep && (
                <p className="text-sm text-zinc-500">Buscando CEP...</p>
              )}

              <input
                value={logradouro}
                onChange={(event) => setLogradouro(event.target.value)}
                disabled={!editandoEndereco}
                placeholder="Logradouro"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
              />

              <div className="grid gap-4 md:grid-cols-2">
                <input
                  value={numero}
                  onChange={(event) => setNumero(event.target.value)}
                  disabled={!editandoEndereco}
                  placeholder="Número"
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
                />

                <input
                  value={complemento}
                  onChange={(event) => setComplemento(event.target.value)}
                  disabled={!editandoEndereco}
                  placeholder="Complemento"
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
                />
              </div>

              <input
                value={bairro}
                onChange={(event) => setBairro(event.target.value)}
                disabled={!editandoEndereco}
                placeholder="Bairro"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
              />

              <div className="grid gap-4 md:grid-cols-[1fr_120px]">
                <input
                  value={cidade}
                  onChange={(event) => setCidade(event.target.value)}
                  disabled={!editandoEndereco}
                  placeholder="Cidade"
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
                />

                <input
                  value={estado}
                  onChange={(event) => setEstado(event.target.value)}
                  disabled={!editandoEndereco}
                  placeholder="UF"
                  maxLength={2}
                  className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 uppercase outline-none transition focus:border-[#b98b7d] disabled:opacity-70 dark:border-white/10 dark:bg-zinc-900"
                />
              </div>

              {editandoEndereco && (
                <div className="flex flex-wrap gap-3 pt-2">
                  <button
                    type="submit"
                    className="rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
                  >
                    Salvar endereço
                  </button>

                  <button
                    type="button"
                    onClick={() => setEditandoEndereco(false)}
                    className="rounded-full border border-black/10 px-6 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                  >
                    Cancelar
                  </button>
                </div>
              )}
            </div>
          </form>
        </div>

        <aside className="space-y-6">
          <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
            <h2 className="font-display text-3xl font-semibold">Resumo</h2>

            <div className="mt-6 grid gap-3">
              <div className="rounded-2xl bg-black/5 p-4 dark:bg-white/5">
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                  Pedidos realizados
                </p>
                <strong className="text-2xl">{pedidos.length}</strong>
              </div>

              <div className="rounded-2xl bg-black/5 p-4 dark:bg-white/5">
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                  Favoritos salvos
                </p>
                <strong className="text-2xl">{favoritos.length}</strong>
              </div>

              <div className="rounded-2xl bg-black/5 p-4 dark:bg-white/5">
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                  Itens no carrinho
                </p>
                <strong className="text-2xl">{quantidadeTotal}</strong>
              </div>
            </div>
          </div>

          <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
            <h2 className="font-display text-3xl font-semibold">Atalhos</h2>

            <div className="mt-6 grid gap-3">
              <Link
                to="/meus-pedidos"
                className="rounded-full border border-black/10 px-5 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
              >
                📦 Meus pedidos
              </Link>

              <Link
                to="/favoritos"
                className="rounded-full border border-black/10 px-5 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
              >
                ♡ Favoritos
              </Link>

              <Link
                to="/carrinho"
                className="rounded-full border border-black/10 px-5 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
              >
                🛒 Carrinho
              </Link>
            </div>
          </div>

          <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
            <h2 className="font-display text-3xl font-semibold">
              Pedidos recentes
            </h2>

            <div className="mt-6 space-y-3">
              {pedidosRecentes.length === 0 ? (
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                  Nenhum pedido recente.
                </p>
              ) : (
                pedidosRecentes.map((pedido) => (
                  <Link
                    key={pedido.id}
                    to="/meus-pedidos"
                    className="block rounded-2xl bg-black/5 p-4 transition hover:bg-black/10 dark:bg-white/5 dark:hover:bg-white/10"
                  >
                    <p className="font-medium">#{pedido.id.slice(0, 8)}</p>
                    <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                      {pedido.status} · R${' '}
                      {pedido.total.toFixed(2).replace('.', ',')}
                    </p>
                  </Link>
                ))
              )}
            </div>
          </div>

          <div className="rounded-[2rem] border border-red-200 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-red-900/50 dark:bg-white/5">
            <h2 className="font-display text-3xl font-semibold">
            </h2>

            <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-300">
              A exclusão da conta remove seu acesso à Perussi.
            </p>

            <button
              type="button"
              onClick={excluirPerfil}
              className="mt-8 w-full rounded-full border border-red-300 px-6 py-3 text-sm font-medium text-red-500 transition hover:bg-red-50 dark:hover:bg-red-950/20"
            >
              Excluir minha conta
            </button>
          </div>
        </aside>
      </div>
    </section>
  )
}
