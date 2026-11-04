import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../services/api'

type Favorito = {
  id: string
  produtoId: string
  nomeProduto: string
  descricao: string
  preco: number
  desconto: number
  categoria: string
  imagemUrl: string
  criadoEm: string
}

export default function FavoritesPage() {
  const [favoritos, setFavoritos] = useState<Favorito[]>([])
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    async function carregarFavoritos() {
      try {
        const response = await api.get('/favoritos')
        setFavoritos(response.data)
      } finally {
        setCarregando(false)
      }
    }

    carregarFavoritos()
  }, [])

  async function removerFavorito(produtoId: string) {
    await api.delete(`/favoritos/${produtoId}`)

    setFavoritos((atuais) =>
      atuais.filter((favorito) => favorito.produtoId !== produtoId),
    )
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-12">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Seleção pessoal
        </p>

        <h1 className="font-display text-5xl font-semibold">
          Seus favoritos
        </h1>

        <p className="mt-4 max-w-2xl text-zinc-600 dark:text-zinc-300">
          Produtos que você salvou para ver novamente mais tarde.
        </p>
      </div>

      {carregando ? (
        <p className="text-zinc-500">Carregando favoritos...</p>
      ) : favoritos.length === 0 ? (
        <div className="rounded-[2rem] border border-black/10 bg-white/70 p-10 text-center shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display text-3xl font-semibold">
            Nenhum favorito ainda
          </h2>

          <p className="mt-3 text-zinc-600 dark:text-zinc-300">
            Explore os produtos e toque no coração para salvar suas peças
            favoritas.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
          {favoritos.map((favorito) => (
            <article
              key={favorito.id}
              className="overflow-hidden rounded-[2rem] border border-black/10 bg-white/70 shadow-xl shadow-black/5 transition hover:-translate-y-1 hover:shadow-2xl dark:border-white/10 dark:bg-white/5"
            >
              <div className="relative aspect-[4/5] overflow-hidden bg-[#efe6df] dark:bg-zinc-900">
                <Link to={`/produtos/${favorito.produtoId}`} className="block h-full">
                  {favorito.imagemUrl ? (
                    <img
                      src={favorito.imagemUrl}
                      alt={favorito.nomeProduto}
                      className="h-full w-full object-cover transition duration-300 hover:scale-105"
                    />
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-[#b98b7d]/40 p-8 text-center dark:from-zinc-900 dark:via-zinc-800 dark:to-[#b98b7d]/30">
                      <div>
                        <p className="text-xs uppercase tracking-[0.35em] text-[#b98b7d]">
                          Perussi
                        </p>

                        <h2 className="font-display mt-4 text-4xl font-semibold">
                          {favorito.nomeProduto}
                        </h2>
                      </div>
                    </div>
                  )}
                </Link>

                <button
                  type="button"
                  onClick={() => removerFavorito(favorito.produtoId)}
                  className="absolute right-4 top-4 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-white/80 text-xl text-[#b98b7d] backdrop-blur transition hover:bg-zinc-950 hover:text-white dark:bg-zinc-950/70"
                >
                  ♥
                </button>
              </div>

              <div className="p-6">
                <p className="mb-2 text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                  {favorito.categoria}
                </p>

                <h2 className="font-display text-2xl font-semibold">
                  {favorito.nomeProduto}
                </h2>

                <p className="mt-2 line-clamp-2 text-sm text-zinc-600 dark:text-zinc-300">
                  {favorito.descricao}
                </p>

                <div className="mt-6 flex items-center justify-between">
                  <div>
                    {favorito.desconto > 0 && (
                      <p className="text-sm text-zinc-500 line-through dark:text-zinc-400">
                        R$ {favorito.preco.toFixed(2).replace('.', ',')}
                      </p>
                    )}

                    <strong className="text-lg">
                      R${' '}
                      {(
                        favorito.preco *
                        (1 - (favorito.desconto || 0) / 100)
                      )
                        .toFixed(2)
                        .replace('.', ',')}
                    </strong>

                    {favorito.desconto > 0 && (
                      <span className="ml-2 rounded-full bg-red-500 px-2 py-1 text-xs text-white">
                        -{favorito.desconto}%
                      </span>
                    )}
                  </div>

                  <Link
                    to={`/produtos/${favorito.produtoId}`}
                    className="rounded-full bg-zinc-950 px-5 py-2 text-sm text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
                  >
                    Ver produto
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}