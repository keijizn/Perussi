import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../services/api'
import { useAuth } from '../../contexts/AuthContext'

type Produto = {
  id: string
  nome: string
  descricao: string
  preco: number
  desconto: number
  categoria: string
  imagemUrl: string
  destaque: boolean
}

export default function HomePage() {
  const [produtosDestaque, setProdutosDestaque] = useState<Produto[]>([])
  const [indiceAtual, setIndiceAtual] = useState(0)
  const { isAuthenticated } = useAuth()

  useEffect(() => {
    async function carregarDestaques() {
      const response = await api.get('/produtos')

      const destaques = response.data.filter(
        (produto: Produto) => produto.destaque,
      )

      setProdutosDestaque(destaques)
    }

    carregarDestaques()
  }, [])

  useEffect(() => {
    if (produtosDestaque.length <= 1) return

    const intervalo = setInterval(() => {
      setIndiceAtual((indice) =>
        indice === produtosDestaque.length - 1 ? 0 : indice + 1,
      )
    }, 4000)

    return () => clearInterval(intervalo)
  }, [produtosDestaque.length])

  const produtoAtual = produtosDestaque[indiceAtual]

  const precoComDesconto = produtoAtual
    ? produtoAtual.preco * (1 - (produtoAtual.desconto || 0) / 100)
    : 0

  return (
    <section className="mx-auto grid min-h-[calc(100vh-81px)] max-w-7xl grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-2">
      <div>
        <p className="mb-4 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Moda premium
        </p>

        <h1 className="font-display max-w-2xl text-5xl font-semibold leading-tight tracking-tight md:text-7xl">
          Elegância minimalista para todos os dias.
        </h1>

        <p className="mt-6 max-w-xl text-lg leading-8 text-zinc-600 dark:text-zinc-300">
          Peças selecionadas com estética sofisticada, tons neutros e uma
          experiência de compra moderna.
        </p>

        <div className="mt-10 flex gap-4">
          <Link
            to="/produtos"
            className="rounded-full bg-zinc-950 px-7 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
          >
            Ver coleção
          </Link>

          {!isAuthenticated && (
            <Link
              to="/login"
              className="rounded-full border border-zinc-300 px-7 py-3 text-sm font-medium transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-zinc-700"
            >
              Entrar
            </Link>
          )}
        </div>
      </div>

      <div className="rounded-[2rem] border border-black/10 bg-white/60 p-4 shadow-2xl shadow-black/10 dark:border-white/10 dark:bg-white/5">
        {produtoAtual ? (
          <div className="relative overflow-hidden rounded-[1.5rem]">
            <Link to={`/produtos/${produtoAtual.id}`} className="block">
              <div className="relative aspect-[4/5] overflow-hidden rounded-[1.5rem] bg-zinc-900">
                {produtoAtual.imagemUrl ? (
                  <img
                    src={produtoAtual.imagemUrl}
                    alt={produtoAtual.nome}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-zinc-950 p-8 text-center">
                    <h2 className="font-display text-4xl font-semibold">
                      {produtoAtual.nome}
                    </h2>
                  </div>
                )}

                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent p-6">
                  <p className="text-xs uppercase tracking-[0.25em] text-[#e8c9bd]">
                    {produtoAtual.categoria}
                  </p>

                  <h2 className="font-display mt-2 text-3xl font-semibold text-white">
                    {produtoAtual.nome}
                  </h2>

                  <p className="mt-2 line-clamp-2 text-sm text-white/90">
                    {produtoAtual.descricao}
                  </p>

                  <div className="mt-4 flex items-center justify-between gap-4">
                    <div>
                      {produtoAtual.desconto > 0 && (
                        <p className="text-sm text-zinc-300 line-through">
                          R$ {produtoAtual.preco.toFixed(2).replace('.', ',')}
                        </p>
                      )}

                      <div className="mt-1 flex items-center gap-3">
                        <strong className="text-xl text-white">
                          R$ {precoComDesconto.toFixed(2).replace('.', ',')}
                        </strong>

                        {produtoAtual.desconto > 0 && (
                          <span className="rounded-full bg-red-500 px-2 py-1 text-xs text-white">
                            -{produtoAtual.desconto}%
                          </span>
                        )}
                      </div>
                    </div>

                    <span className="rounded-full bg-white px-5 py-2 text-sm font-medium text-zinc-950">
                      Ver produto
                    </span>
                  </div>
                </div>
              </div>
            </Link>

            {produtosDestaque.length > 1 && (
              <div className="absolute top-6 right-6 flex gap-2">
                {produtosDestaque.map((produto, index) => (
                  <button
                    key={produto.id}
                    type="button"
                    onClick={() => setIndiceAtual(index)}
                    className={`h-2.5 rounded-full transition ${index === indiceAtual
                      ? 'w-8 bg-white'
                      : 'w-2.5 bg-white/50'
                      }`}
                    aria-label={`Ver destaque ${index + 1}`}
                  />
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="flex aspect-[4/5] items-end rounded-[1.5rem] bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-zinc-950 p-8">
            <div className="rounded-2xl bg-white/80 p-6 backdrop-blur-md dark:bg-zinc-950/70">
              <p className="text-sm uppercase tracking-[0.25em] text-[#b98b7d]">
                Nova coleção
              </p>

              <h2 className="font-display mt-2 text-3xl font-semibold">
                Perussi 2026
              </h2>
            </div>
          </div>
        )}
      </div>
    </section>
  )
}