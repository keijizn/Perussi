import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { api } from '../services/api'
import { useCart } from '../contexts/CartContext'

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

export default function ProductDetailsPage() {
  const { id } = useParams()
  const { carregarCarrinho } = useCart()

  const [produto, setProduto] = useState<Produto | null>(null)
  const [variacaoId, setVariacaoId] = useState('')
  const [quantidade, setQuantidade] = useState(1)
  const [mensagem, setMensagem] = useState('')
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    async function carregarProduto() {
      try {
        const response = await api.get(`/produtos/${id}`)
        setProduto(response.data)

        const primeiraDisponivel = response.data.variacoes.find(
          (variacao: Produto['variacoes'][number]) => variacao.estoque > 0,
        )

        if (primeiraDisponivel) {
          setVariacaoId(primeiraDisponivel.id)
        }
      } finally {
        setCarregando(false)
      }
    }

    carregarProduto()
  }, [id])

  async function adicionarAoCarrinho() {
    if (!produto || !variacaoId) {
      setMensagem('Selecione uma variação disponível.')
      return
    }

    try {
      await api.post('/carrinho', {
        produtoId: produto.id,
        variacaoId,
        quantidade,
      })

      await carregarCarrinho()
      setMensagem('Produto adicionado ao carrinho.')
    } catch {
      setMensagem('Faça login para adicionar ao carrinho.')
    }
  }

  if (carregando) {
    return (
      <section className="mx-auto max-w-7xl px-6 py-14">
        <p className="text-zinc-500">Carregando produto...</p>
      </section>
    )
  }

  if (!produto) {
    return (
      <section className="mx-auto max-w-7xl px-6 py-14">
        <p className="text-zinc-500">Produto não encontrado.</p>
      </section>
    )
  }

  const variacaoSelecionada = produto.variacoes.find(
    (variacao) => variacao.id === variacaoId,
  )
  const precoComDesconto =
    produto.preco * (1 - (produto.desconto || 0) / 100)

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <Link
        to="/produtos"
        className="mb-8 inline-flex text-sm text-zinc-500 transition hover:text-[#b98b7d]"
      >
        ← Voltar para produtos
      </Link>

      <div className="grid gap-10 lg:grid-cols-2">
        <div className="overflow-hidden rounded-[2rem] border border-black/10 bg-white/70 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          {produto.imagemUrl ? (
            <img
              src={produto.imagemUrl}
              alt={produto.nome}
              className="aspect-[4/5] w-full object-cover"
            />
          ) : (
            <div className="flex aspect-[4/5] items-center justify-center bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-[#b98b7d]/40 p-10 text-center dark:from-zinc-900 dark:via-zinc-800 dark:to-[#b98b7d]/30">
              <div>
                <p className="text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
                  Perussi
                </p>

                <h1 className="font-display mt-5 text-5xl font-semibold">
                  {produto.nome}
                </h1>
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col justify-center">
          <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
            {produto.categoria}
          </p>

          <h1 className="font-display text-5xl font-semibold">
            {produto.nome}
          </h1>

          <p className="mt-5 text-lg leading-8 text-zinc-600 dark:text-zinc-300">
            {produto.descricao}
          </p>

          <div className="mt-8">
            {produto.desconto > 0 && (
              <p className="text-lg text-zinc-500 line-through dark:text-zinc-400">
                R$ {produto.preco.toFixed(2).replace('.', ',')}
              </p>
            )}

            <div className="mt-1 flex items-center gap-3">
              <strong className="text-3xl">
                R$ {precoComDesconto.toFixed(2).replace('.', ',')}
              </strong>

              {produto.desconto > 0 && (
                <span className="rounded-full bg-red-500 px-3 py-1 text-sm font-medium text-white">
                  -{produto.desconto}%
                </span>
              )}
            </div>
          </div>

          <div className="mt-8">
            <label className="mb-3 block text-sm font-medium">
              Tamanho e cor
            </label>

            <select
              value={variacaoId}
              onChange={(event) => setVariacaoId(event.target.value)}
              className="w-full rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            >
              {produto.variacoes.map((variacao) => (
                <option
                  key={variacao.id}
                  value={variacao.id}
                  disabled={variacao.estoque <= 0}
                >
                  {variacao.tamanho} · {variacao.cor} · Estoque:{' '}
                  {variacao.estoque}
                </option>
              ))}
            </select>
          </div>

          <div className="mt-5">
            <label className="mb-3 block text-sm font-medium">
              Quantidade
            </label>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() =>
                  setQuantidade((atual) => Math.max(1, atual - 1))
                }
                className="flex h-10 w-10 items-center justify-center rounded-full border border-black/10 dark:border-white/10"
              >
                −
              </button>

              <span className="min-w-8 text-center">{quantidade}</span>

              <button
                type="button"
                disabled={
                  !!variacaoSelecionada &&
                  quantidade >= variacaoSelecionada.estoque
                }
                onClick={() => setQuantidade((atual) => atual + 1)}
                className="flex h-10 w-10 items-center justify-center rounded-full border border-black/10 disabled:opacity-40 dark:border-white/10"
              >
                +
              </button>
            </div>
          </div>

          {mensagem && (
            <p className="mt-6 rounded-2xl bg-[#f2ddd5] px-4 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
              {mensagem}
            </p>
          )}

          <button
            type="button"
            onClick={adicionarAoCarrinho}
            disabled={!variacaoSelecionada || variacaoSelecionada.estoque <= 0}
            className="mt-8 rounded-full bg-zinc-950 px-8 py-4 text-sm font-medium text-white transition hover:bg-[#b98b7d] disabled:cursor-not-allowed disabled:opacity-50 dark:bg-white dark:text-zinc-950"
          >
            Adicionar ao carrinho
          </button>
        </div>
      </div>
    </section>
  )
}