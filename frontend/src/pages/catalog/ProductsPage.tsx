import { useNotification } from '../../contexts/NotificationContext'
import { useEffect, useState } from 'react'
import { api } from '../../services/api'
import { useAuth } from '../../contexts/AuthContext'
import { useCart } from '../../contexts/CartContext'
import { Link } from 'react-router-dom'

type Produto = {
  id: string
  nome: string
  descricao: string
  preco: number
  desconto: number
  categoria: string
  imagemUrl: string
  destaque: boolean
  variacoes: {
    id: string
    tamanho: string
    cor: string
    estoque: number
  }[]
}

type Favorito = {
  id: string
  produtoId: string
  nomeProduto: string
  descricao: string
  preco: number
  categoria: string
  imagemUrl: string
  criadoEm: string
}

export default function ProductsPage() {
  const notificar = useNotification()
  const { isAuthenticated } = useAuth()
  const { carregarCarrinho } = useCart()

  const [produtos, setProdutos] = useState<Produto[]>([])
  const [favoritos, setFavoritos] = useState<string[]>([])
  const [carregando, setCarregando] = useState(true)
  const [mensagem, setMensagem] = useState('')
  const [busca, setBusca] = useState('')
  const [categoriaSelecionada, setCategoriaSelecionada] = useState('Todos')

  useEffect(() => {
    async function carregarDados() {
      try {
        const produtosResponse = await api.get('/produtos')
        setProdutos(produtosResponse.data)

        if (isAuthenticated) {
          const favoritosResponse = await api.get('/favoritos')
          const idsFavoritos = favoritosResponse.data.map(
            (favorito: Favorito) => favorito.produtoId,
          )

          setFavoritos(idsFavoritos)
        }
      } finally {
        setCarregando(false)
      }
    }

    carregarDados()
  }, [isAuthenticated])

  async function alternarFavorito(produtoId: string) {
    if (!isAuthenticated) {
      setMensagem('Faça login para favoritar produtos.')
      return
    }

    const jaFavoritado = favoritos.includes(produtoId)

    try {
      if (jaFavoritado) {
        await api.delete(`/favoritos/${produtoId}`)
        setFavoritos((atuais) => atuais.filter((id) => id !== produtoId))
      } else {
        await api.post(`/favoritos/${produtoId}`)
        setFavoritos((atuais) => [...atuais, produtoId])
      }
      notificar(jaFavoritado ? 'Produto removido dos favoritos.' : 'Produto salvo nos favoritos.')
    } catch {
      notificar('Não foi possível atualizar os favoritos. Tente novamente.', 'erro')
    }
  }

  async function adicionarAoCarrinho(produto: Produto) {
    const variacaoDisponivel = produto.variacoes.find(
      (variacao) => variacao.estoque > 0,
    )

    if (!variacaoDisponivel) {
      setMensagem('Produto sem variação disponível no momento.')
      return
    }

    try {
      await api.post('/carrinho', {
        produtoId: produto.id,
        variacaoId: variacaoDisponivel.id,
        quantidade: 1,

      })

      await carregarCarrinho()

      setMensagem('')
      notificar(`${produto.nome} adicionado ao carrinho.`)
    } catch {
      notificar(isAuthenticated ? 'Não foi possível atualizar o carrinho. Confira o estoque e tente novamente.' : 'Faça login para adicionar produtos ao carrinho.', 'erro')
    }
  }

  const categorias = [
    'Todos',
    ...Array.from(new Set(produtos.map((produto) => produto.categoria))),
  ]

  const produtosFiltrados = produtos.filter((produto) => {
    const buscaNormalizada = busca.toLowerCase()

    const correspondeBusca =
      produto.nome.toLowerCase().includes(buscaNormalizada) ||
      produto.descricao.toLowerCase().includes(buscaNormalizada) ||
      produto.categoria.toLowerCase().includes(buscaNormalizada)

    const correspondeCategoria =
      categoriaSelecionada === 'Todos' ||
      produto.categoria === categoriaSelecionada

    return correspondeBusca && correspondeCategoria
  })

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-12 flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
            Coleção
          </p>

          <h1 className="font-display text-5xl font-semibold">
            Produtos Perussi
          </h1>

          <p className="mt-4 max-w-2xl text-zinc-600 dark:text-zinc-300">
            Peças selecionadas com acabamento sofisticado e estética
            minimalista.
          </p>
        </div>

        {mensagem && (
          <div className="rounded-full border border-[#b98b7d]/30 bg-[#f2ddd5] px-5 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
            {mensagem}
          </div>
        )}
      </div>

      <div className="mb-10 rounded-[2rem] border border-black/10 bg-white/70 p-5 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
        <div className="grid gap-4 md:grid-cols-[1fr_260px]">
          <input
            value={busca}
            onChange={(event) => setBusca(event.target.value)}
            placeholder="Buscar por nome, descrição ou categoria..."
            className="w-full rounded-full border border-black/10 bg-white px-5 py-3 text-sm outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
          />

          <select
            value={categoriaSelecionada}
            onChange={(event) => setCategoriaSelecionada(event.target.value)}
            className="w-full rounded-full border border-black/10 bg-white px-5 py-3 text-sm outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
          >
            {categorias.map((categoria) => (
              <option key={categoria} value={categoria}>
                {categoria}
              </option>
            ))}
          </select>
        </div>
      </div>

      {carregando ? (
        <p className="text-zinc-500">Carregando produtos...</p>
      ) : produtosFiltrados.length === 0 ? (
        <div className="rounded-[2rem] border border-black/10 bg-white/70 p-10 text-center shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display text-3xl font-semibold">
            Nenhum produto encontrado
          </h2>

          <p className="mt-3 text-zinc-600 dark:text-zinc-300">
            Tente buscar por outro nome ou selecionar outra categoria.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
          {produtosFiltrados.map((produto) => {
            const favorito = favoritos.includes(produto.id)
            const primeiraVariacao = produto.variacoes[0]
            const temEstoque = produto.variacoes.some(
              (variacao) => variacao.estoque > 0,
            )
            const precoComDesconto =
              produto.preco *
              (1 - (produto.desconto || 0) / 100)

            return (
              <article
                key={produto.id}
                className="group overflow-hidden rounded-[2rem] border border-black/10 bg-white/70 shadow-xl shadow-black/5 transition hover:-translate-y-1 hover:shadow-2xl dark:border-white/10 dark:bg-white/5"
              >
                <div className="relative aspect-[4/5] overflow-hidden bg-[#efe6df] dark:bg-zinc-900">
                  <Link to={`/produtos/${produto.id}`} className="block h-full">
                    {produto.imagemUrl ? (
                      <img
                        src={produto.imagemUrl}
                        alt={produto.nome}
                        className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                      />
                    ) : (
                      <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-[#b98b7d]/40 p-8 text-center dark:from-zinc-900 dark:via-zinc-800 dark:to-[#b98b7d]/30">
                        <div>
                          <p className="text-xs uppercase tracking-[0.35em] text-[#b98b7d]">
                            Perussi
                          </p>

                          <h2 className="font-display mt-4 text-4xl font-semibold text-zinc-950 dark:text-zinc-50">
                            {produto.nome}
                          </h2>
                        </div>
                      </div>
                    )}
                  </Link>

                  <button
                    type="button"
                    onClick={() => alternarFavorito(produto.id)}
                    className="absolute right-4 top-4 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/80 text-xl backdrop-blur transition hover:text-[#b98b7d] dark:bg-zinc-950/70"
                  >
                    {favorito ? '♥' : '♡'}
                  </button>
                </div>

                <div className="p-6">
                  <Link to={`/produtos/${produto.id}`} className="block">
                    <p className="mb-2 text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                      {produto.categoria}
                    </p>

                    <h2 className="font-display text-2xl font-semibold">
                      {produto.nome}
                    </h2>

                    <p className="mt-2 line-clamp-2 text-sm text-zinc-600 dark:text-zinc-300">
                      {produto.descricao}
                    </p>

                    {primeiraVariacao && (
                      <p className="mt-4 text-xs text-zinc-500 dark:text-zinc-400">
                        {primeiraVariacao.tamanho} · {primeiraVariacao.cor}
                      </p>
                    )}
                  </Link>

                  <div className="mt-6 flex items-center justify-between gap-4">
                    <div>
                      {produto.desconto > 0 && (
                        <p className="text-sm text-zinc-500 line-through">
                          R$ {produto.preco.toFixed(2).replace('.', ',')}
                        </p>
                      )}

                      <strong className="text-lg">
                        R$ {precoComDesconto.toFixed(2).replace('.', ',')}
                      </strong>

                      {produto.desconto > 0 && (
                        <span className="ml-2 rounded-full bg-red-500 px-2 py-1 text-xs text-white">
                          -{produto.desconto}%
                        </span>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => adicionarAoCarrinho(produto)}
                      disabled={!temEstoque}
                      className="rounded-full bg-zinc-950 px-5 py-2 text-sm text-white transition hover:bg-[#b98b7d] disabled:cursor-not-allowed disabled:opacity-50 dark:bg-white dark:text-zinc-950"
                    >
                      {temEstoque ? 'Adicionar' : 'Sem estoque'}
                    </button>
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}