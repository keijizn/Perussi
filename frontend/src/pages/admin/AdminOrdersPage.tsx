import { useEffect, useState } from 'react'
import { api } from '../../services/api'

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
  subtotal: number
  frete: number | null
  status: string
  metodoPagamento: string
  criadoEm: string
  cep: string
  logradouro: string
  numero: string
  bairro: string
  cidade: string
  estado: string
  complemento: string
  itens: PedidoItem[]
}

const proximosStatus: Record<string, string[]> = {
  PENDENTE: ['PENDENTE', 'CANCELADO'],
  PAGO: ['PAGO', 'ENVIADO'],
  ENVIADO: ['ENVIADO', 'ENTREGUE'],
  ENTREGUE: ['ENTREGUE'],
  CANCELADO: ['CANCELADO'],
}

function formatarMetodoPagamento(metodo?: string) {
  if (metodo === 'PIX') return 'PIX'
  if (metodo === 'CARTAO') return 'Cartão de Crédito'
  return '-'
}

export default function AdminOrdersPage() {
  const [pedidos, setPedidos] = useState<Pedido[]>([])
  const [carregando, setCarregando] = useState(true)
  const [mensagem, setMensagem] = useState('')

  async function carregarPedidos() {
    try {
      const response = await api.get('/pedidos')
      setPedidos(response.data)
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregarPedidos()
  }, [])

  async function atualizarStatus(pedidoId: string, status: string) {
    setMensagem('')

    try {
      await api.put(`/pedidos/${pedidoId}/status`, {
        status,
      })

      await carregarPedidos()

      setMensagem('Pedido atualizado. Confira o status: pagamentos já aprovados são preservados.')
    } catch (error: any) {
      setMensagem(error.response?.data?.erro || 'Erro ao atualizar status do pedido.')
    }
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-10">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Administração
        </p>

        <h1 className="font-display text-5xl font-semibold">Pedidos</h1>

        <p className="mt-3 text-zinc-600 dark:text-zinc-300">
          Acompanhe pedidos realizados e atualize o status das compras.
        </p>
      </div>

      {mensagem && (
        <div className="mb-8 rounded-2xl bg-[#f2ddd5] px-5 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
          {mensagem}
        </div>
      )}

      {carregando ? (
        <p className="text-zinc-500">Carregando pedidos...</p>
      ) : pedidos.length === 0 ? (
        <div className="rounded-[2rem] border border-black/10 bg-white/70 p-10 text-center shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display text-3xl font-semibold">
            Nenhum pedido encontrado
          </h2>

          <p className="mt-3 text-zinc-600 dark:text-zinc-300">
            Quando clientes finalizarem compras, os pedidos aparecerão aqui.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {pedidos.map((pedido) => (
            <article
              key={pedido.id}
              className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5"
            >
              <div className="flex flex-col gap-5 border-b border-black/10 pb-5 dark:border-white/10 md:flex-row md:items-start md:justify-between">
                <div>
                  <p className="text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                    Pedido
                  </p>

                  <h2 className="font-display mt-2 text-2xl font-semibold">
                    #{pedido.id.slice(0, 8)}
                  </h2>

                  <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
                    {new Date(pedido.criadoEm).toLocaleString('pt-BR')}
                  </p>
                </div>

                <div className="flex flex-col gap-3 md:items-end">
                  <span className="rounded-full bg-[#f2ddd5] px-4 py-2 text-sm font-medium text-zinc-950 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
                    {pedido.status}
                  </span>

                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    Pagamento: {formatarMetodoPagamento(pedido.metodoPagamento)}
                  </p>

                  <p className="text-sm">Produtos: R$ {pedido.subtotal.toFixed(2).replace('.', ',')}</p>
                  <p className="text-sm">Frete: {pedido.frete == null ? 'Não registrado (pedido anterior)' : `R$ ${pedido.frete.toFixed(2).replace('.', ',')}`}</p>
                  <strong className="text-xl">
                    R$ {pedido.total.toFixed(2).replace('.', ',')}
                  </strong>
                </div>
              </div>

              <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_320px]">
                <div className="space-y-4">
                  {pedido.itens.map((item) => (
                    <div
                      key={`${pedido.id}-${item.produtoId}-${item.tamanho}-${item.cor}`}
                      className="flex justify-between gap-4 rounded-2xl bg-black/5 p-4 dark:bg-white/5"
                    >
                      <div>
                        <h3 className="font-display text-xl font-semibold">
                          {item.nomeProduto}
                        </h3>

                        <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">
                          {item.quantidade}x · {item.tamanho} · {item.cor}
                        </p>
                      </div>

                      <span className="text-sm font-medium">
                        R${' '}
                        {(item.precoUnitario * item.quantidade)
                          .toFixed(2)
                          .replace('.', ',')}
                      </span>
                    </div>
                  ))}
                </div>

                <div className="space-y-4">
                  <div className="rounded-2xl border border-black/10 p-4 text-sm dark:border-white/10">
                    <p className="mb-2 text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                      Entrega
                    </p>

                    <p>
                      {pedido.logradouro}, {pedido.numero}
                    </p>

                    {pedido.complemento && <p>{pedido.complemento}</p>}

                    <p>{pedido.bairro}</p>

                    <p>
                      {pedido.cidade} - {pedido.estado}
                    </p>

                    <p>CEP: {pedido.cep}</p>
                  </div>

                  <div className="rounded-2xl border border-black/10 p-4 dark:border-white/10">
                    <p className="mb-3 text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                      Alterar status
                    </p>

                    <select
                      value={pedido.status}
                      onChange={(event) =>
                        atualizarStatus(pedido.id, event.target.value)
                      }
                      className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
                    >
                      {(proximosStatus[pedido.status] || [pedido.status]).map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
