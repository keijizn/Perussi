import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../services/api'

type Produto = {
  id: string
  destaque: boolean
  desconto: number
}

type Pedido = {
  id: string
  total: number
  status: string
}

export default function AdminDashboardPage() {
  const [produtos, setProdutos] = useState<Produto[]>([])
  const [pedidos, setPedidos] = useState<Pedido[]>([])
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    async function carregarDados() {
      try {
        const [produtosResponse, pedidosResponse] = await Promise.all([
          api.get('/produtos'),
          api.get('/pedidos'),
        ])

        setProdutos(produtosResponse.data)
        setPedidos(pedidosResponse.data)
      } finally {
        setCarregando(false)
      }
    }

    carregarDados()
  }, [])

  const pedidosPendentes = pedidos.filter(
    (pedido) => pedido.status === 'PENDENTE',
  ).length

  const pedidosPagos = pedidos.filter((pedido) => pedido.status === 'PAGO').length
  const pedidosEnviados = pedidos.filter((pedido) => pedido.status === 'ENVIADO').length
  const pedidosEntregues = pedidos.filter((pedido) => pedido.status === 'ENTREGUE').length

  const produtosDestaque = produtos.filter((produto) => produto.destaque).length
  const produtosPromocao = produtos.filter(
    (produto) => produto.desconto > 0,
  ).length

  const pedidosFaturados = pedidos.filter((pedido) =>
    ['PAGO', 'ENVIADO', 'ENTREGUE'].includes(pedido.status),
  )

  const faturamentoTotal = pedidosFaturados.reduce(
    (total, pedido) => total + pedido.total,
    0,
  )

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-10">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Administração
        </p>

        <h1 className="font-display text-5xl font-semibold">Dashboard</h1>

        <p className="mt-3 text-zinc-600 dark:text-zinc-300">
          Visão geral da operação da loja Perussi.
        </p>
      </div>

      {carregando ? (
        <p className="text-zinc-500">Carregando dashboard...</p>
      ) : (
        <>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Produtos cadastrados
              </p>
              <strong className="mt-3 block text-4xl">
                {produtos.length}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Produtos em destaque
              </p>
              <strong className="mt-3 block text-4xl">
                {produtosDestaque}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Produtos em promoção
              </p>
              <strong className="mt-3 block text-4xl">
                {produtosPromocao}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Pedidos realizados
              </p>
              <strong className="mt-3 block text-4xl">{pedidos.length}</strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Pedidos pendentes
              </p>
              <strong className="mt-3 block text-4xl">
                {pedidosPendentes}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Pedidos pagos
              </p>
              <strong className="mt-3 block text-4xl">
                {pedidosPagos}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Pedidos enviados
              </p>
              <strong className="mt-3 block text-4xl">
                {pedidosEnviados}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Pedidos entregues
              </p>
              <strong className="mt-3 block text-4xl">
                {pedidosEntregues}
              </strong>
            </div>

            <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">
                Faturamento confirmado
              </p>
              <strong className="mt-3 block text-3xl">
                R$ {faturamentoTotal.toFixed(2).replace('.', ',')}
              </strong>
            </div>
          </div>

          <div className="mt-10 grid gap-8 lg:grid-cols-2">
            <Link
              to="/admin/produtos"
              className="rounded-[2rem] border border-black/10 bg-white/70 p-8 shadow-xl shadow-black/5 transition hover:-translate-y-1 hover:border-[#b98b7d] dark:border-white/10 dark:bg-white/5"
            >
              <p className="mb-3 text-sm uppercase tracking-[0.25em] text-[#b98b7d]">
                Catálogo
              </p>

              <h2 className="font-display text-3xl font-semibold">
                Gerenciar Produtos
              </h2>

              <p className="mt-3 text-zinc-600 dark:text-zinc-300">
                Cadastre produtos, edite preços e controle variações de estoque.
              </p>
            </Link>

            <Link
              to="/admin/pedidos"
              className="rounded-[2rem] border border-black/10 bg-white/70 p-8 shadow-xl shadow-black/5 transition hover:-translate-y-1 hover:border-[#b98b7d] dark:border-white/10 dark:bg-white/5"
            >
              <p className="mb-3 text-sm uppercase tracking-[0.25em] text-[#b98b7d]">
                Operação
              </p>

              <h2 className="font-display text-3xl font-semibold">
                Gerenciar Pedidos
              </h2>

              <p className="mt-3 text-zinc-600 dark:text-zinc-300">
                Acompanhe pedidos e atualize o status das compras realizadas.
              </p>
            </Link>
          </div>
        </>
      )}
    </section>
  )
}