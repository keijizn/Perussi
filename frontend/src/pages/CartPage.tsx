import { Link } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'

export default function CartPage() {
  const {
    itens,
    totalCarrinho,
    carregando,
    removerItem,
    atualizarQuantidade,
  } = useCart()

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-12">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Sua seleção
        </p>

        <h1 className="font-display text-5xl font-semibold">Carrinho</h1>

        <p className="mt-4 max-w-2xl text-zinc-600 dark:text-zinc-300">
          Revise suas peças antes de finalizar a compra.
        </p>
      </div>

      {carregando ? (
        <p className="text-zinc-500">Carregando carrinho...</p>
      ) : itens.length === 0 ? (
        <div className="rounded-[2rem] border border-black/10 bg-white/70 p-10 text-center shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display text-3xl font-semibold">
            Seu carrinho está vazio
          </h2>

          <p className="mt-3 text-zinc-600 dark:text-zinc-300">
            Explore a coleção e adicione suas peças favoritas.
          </p>

          <Link
            to="/produtos"
            className="mt-8 inline-flex rounded-full bg-zinc-950 px-7 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
          >
            Ver produtos
          </Link>
        </div>
      ) : (
        <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
          <div className="space-y-5">
            {itens.map((item) => {
              const subtotal = item.precoComDesconto * item.quantidade

              return (
                <article
                  key={item.id}
                  className="grid gap-5 rounded-[2rem] border border-black/10 bg-white/70 p-5 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5 md:grid-cols-[120px_1fr_auto]"
                >
                  <div className="h-32 overflow-hidden rounded-3xl bg-gradient-to-br from-[#f2ddd5] via-[#efe6df] to-[#b98b7d]/40 text-center dark:from-zinc-900 dark:via-zinc-800 dark:to-[#b98b7d]/30">
                    {item.imagemUrl ? (
                      <img
                        src={item.imagemUrl}
                        alt={item.nomeProduto}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center p-3">
                        <span className="font-display text-xl font-semibold">
                          {item.nomeProduto}
                        </span>
                      </div>
                    )}
                  </div>

                  <div>
                    <p className="text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                      {item.tamanho} · {item.cor}
                    </p>

                    <h2 className="font-display mt-2 text-2xl font-semibold">
                      {item.nomeProduto}
                    </h2>

                    <div className="mt-2">
                      {item.desconto > 0 && (
                        <p className="text-sm text-zinc-500 line-through dark:text-zinc-400">
                          R$ {item.preco.toFixed(2).replace('.', ',')}
                        </p>
                      )}

                      <p className="text-sm font-medium">
                        R$ {item.precoComDesconto.toFixed(2).replace('.', ',')}
                        {item.desconto > 0 && (
                          <span className="ml-2 rounded-full bg-red-500 px-2 py-1 text-xs text-white">
                            -{item.desconto}%
                          </span>
                        )}
                      </p>
                    </div>

                    <div className="mt-3 flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => {
                          if (item.quantidade > 1) {
                            atualizarQuantidade(item.id, item.quantidade - 1)
                          } else {
                            removerItem(item.id)
                          }
                        }}
                        className="flex h-8 w-8 items-center justify-center rounded-full border border-black/10 dark:border-white/10"
                      >
                        −
                      </button>

                      <span className="min-w-[24px] text-center">
                        {item.quantidade}
                      </span>

                      <button
                        type="button"
                        disabled={item.quantidade >= item.estoqueDisponivel}
                        onClick={() =>
                          atualizarQuantidade(item.id, item.quantidade + 1)
                        }
                        className="flex h-8 w-8 items-center justify-center rounded-full border border-black/10 disabled:opacity-40 dark:border-white/10"
                      >
                        +
                      </button>
                    </div>

                    <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                      Estoque disponível: {item.estoqueDisponivel}
                    </p>
                  </div>

                  <div className="flex flex-col items-start justify-between gap-4 md:items-end">
                    <div className="text-left md:text-right">
                      <p className="text-sm text-zinc-500 dark:text-zinc-400">
                        Subtotal
                      </p>

                      <strong className="text-xl">
                        R$ {subtotal.toFixed(2).replace('.', ',')}
                      </strong>
                    </div>

                    <button
                      type="button"
                      onClick={() => removerItem(item.id)}
                      className="rounded-full border border-black/10 px-5 py-2 text-sm transition hover:border-red-400 hover:text-red-500 dark:border-white/10"
                    >
                      Remover
                    </button>
                  </div>
                </article>
              )
            })}
          </div>

          <aside className="h-fit rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
            <h2 className="font-display text-3xl font-semibold">Resumo</h2>

            <div className="mt-6 space-y-4 text-sm">
              <div className="flex justify-between text-zinc-600 dark:text-zinc-300">
                <span>Subtotal</span>
                <span>R$ {totalCarrinho.toFixed(2).replace('.', ',')}</span>
              </div>

              <div className="flex justify-between text-zinc-600 dark:text-zinc-300">
                <span>Entrega</span>
                <span>Calculada no checkout</span>
              </div>

              <div className="border-t border-black/10 pt-4 dark:border-white/10">
                <div className="flex justify-between text-lg font-semibold">
                  <span>Total</span>
                  <span>R$ {totalCarrinho.toFixed(2).replace('.', ',')}</span>
                </div>
              </div>
            </div>

            <Link
              to="/checkout"
              className="mt-8 flex w-full justify-center rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
            >
              Finalizar compra
            </Link>
          </aside>
        </div>
      )}
    </section>
  )
}