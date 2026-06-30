import { FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useCart } from '../contexts/CartContext'
import { api } from '../services/api'

type ViaCepResponse = {
  cep: string
  logradouro: string
  bairro: string
  localidade: string
  uf: string
  erro?: boolean
}

type EnderecoUsuario = {
  cep: string
  logradouro: string
  numero: string
  bairro: string
  cidade: string
  estado: string
  complemento: string
}

export default function CheckoutPage() {
  const navigate = useNavigate()
  const { itens, totalCarrinho, carregarCarrinho } = useCart()

  const [cep, setCep] = useState('')
  const [logradouro, setLogradouro] = useState('')
  const [numero, setNumero] = useState('')
  const [bairro, setBairro] = useState('')
  const [cidade, setCidade] = useState('')
  const [estado, setEstado] = useState('')
  const [complemento, setComplemento] = useState('')

  const [buscandoCep, setBuscandoCep] = useState(false)
  const [finalizando, setFinalizando] = useState(false)
  const [erro, setErro] = useState('')
  

  async function carregarEnderecoPadrao() {
    try {
      const response = await api.get('/usuarios/me/endereco')
      const endereco: EnderecoUsuario = response.data

      setCep(endereco.cep || '')
      setLogradouro(endereco.logradouro || '')
      setNumero(endereco.numero || '')
      setBairro(endereco.bairro || '')
      setCidade(endereco.cidade || '')
      setEstado(endereco.estado || '')
      setComplemento(endereco.complemento || '')
    } catch {
      // Se o usuário não tiver endereço padrão, mantém os campos vazios.
    }
  }

  useEffect(() => {
    carregarEnderecoPadrao()
  }, [])

  async function buscarCep(valor: string) {
    const cepLimpo = valor.replace(/\D/g, '')

    setCep(valor)

    if (cepLimpo.length !== 8) {
      return
    }

    setBuscandoCep(true)
    setErro('')

    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`)
      const data: ViaCepResponse = await response.json()

      if (data.erro) {
        setErro('CEP não encontrado.')
        return
      }

      setLogradouro(data.logradouro || '')
      setBairro(data.bairro || '')
      setCidade(data.localidade || '')
      setEstado(data.uf || '')
    } catch {
      setErro('Não foi possível buscar o CEP.')
    } finally {
      setBuscandoCep(false)
    }
  }

  async function finalizarPedido(event: FormEvent) {
    event.preventDefault()
    setErro('')

    if (itens.length === 0) {
      setErro('Seu carrinho está vazio.')
      return
    }

    if (!cep || !logradouro || !numero || !bairro || !cidade || !estado) {
      setErro('Preencha todos os campos obrigatórios.')
      return
    }

    try {
      setFinalizando(true)

      const response = await api.post('/pedidos/finalizar', {
        cep,
        logradouro,
        numero,
        bairro,
        cidade,
        estado,
        complemento,
      })

      await carregarCarrinho()

      navigate(`/pagamento/${response.data.id}`)
    } catch {
      setErro('Não foi possível finalizar o pedido.')
    } finally {
      setFinalizando(false)
    }
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-10">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Finalização
        </p>

        <h1 className="font-display text-5xl font-semibold">Checkout</h1>

        <p className="mt-3 text-zinc-600 dark:text-zinc-300">
          Informe o endereço de entrega.
        </p>
      </div>

      <form onSubmit={finalizarPedido} className="grid gap-8 lg:grid-cols-2">
        <div className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display mb-6 text-3xl font-semibold">
            Endereço
          </h2>

          <div className="space-y-4">
            <input
              value={cep}
              onChange={(event) => buscarCep(event.target.value)}
              placeholder="CEP"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            {buscandoCep && (
              <p className="text-sm text-zinc-500">Buscando CEP...</p>
            )}

            <input
              value={logradouro}
              onChange={(event) => setLogradouro(event.target.value)}
              placeholder="Logradouro"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <div className="grid gap-4 md:grid-cols-2">
              <input
                value={numero}
                onChange={(event) => setNumero(event.target.value)}
                placeholder="Número"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={complemento}
                onChange={(event) => setComplemento(event.target.value)}
                placeholder="Complemento"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />
            </div>

            <input
              value={bairro}
              onChange={(event) => setBairro(event.target.value)}
              placeholder="Bairro"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <div className="grid gap-4 md:grid-cols-[1fr_120px]">
              <input
                value={cidade}
                onChange={(event) => setCidade(event.target.value)}
                placeholder="Cidade"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={estado}
                onChange={(event) => setEstado(event.target.value)}
                placeholder="UF"
                maxLength={2}
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 uppercase outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />
            </div>

            {erro && (
              <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">
                {erro}
              </p>
            )}
          </div>
        </div>

        <aside className="h-fit rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
          <h2 className="font-display text-3xl font-semibold">
            Resumo do Pedido
          </h2>

          <div className="mt-6 space-y-4">
            {itens.map((item) => (
              <div
                key={item.id}
                className="flex justify-between gap-4 border-b border-black/10 pb-4 text-sm dark:border-white/10"
              >
                <div>
                  <p className="font-medium">{item.nomeProduto}</p>
                  <p className="mt-1 text-zinc-500 dark:text-zinc-400">
                    {item.quantidade}x · {item.tamanho} · {item.cor}
                  </p>
                </div>

                <span>
                  R${' '}
                  {(item.precoComDesconto * item.quantidade)
                    .toFixed(2)
                    .replace('.', ',')}
                </span>
              </div>
            ))}

            <div className="flex justify-between text-sm text-zinc-600 dark:text-zinc-300">
              <span>Subtotal</span>
              <span>R$ {totalCarrinho.toFixed(2).replace('.', ',')}</span>
            </div>

            <div className="flex justify-between text-sm text-zinc-600 dark:text-zinc-300">
              <span>Entrega</span>
              <span>Calculada posteriormente</span>
            </div>

            <div className="border-t border-black/10 pt-4 dark:border-white/10">
              <div className="flex justify-between text-lg font-semibold">
                <span>Total</span>
                <span>R$ {totalCarrinho.toFixed(2).replace('.', ',')}</span>
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={finalizando || itens.length === 0}
            className="mt-8 flex w-full justify-center rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] disabled:cursor-not-allowed disabled:opacity-50 dark:bg-white dark:text-zinc-950"
          >
            {finalizando ? 'Finalizando...' : 'Finalizar pedido'}
          </button>
        </aside>
      </form>
    </section>
  )
}