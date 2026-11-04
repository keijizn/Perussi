import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../services/api'

function formatarNumeroCartao(valor: string) {
  return valor
    .replace(/\D/g, '')
    .slice(0, 16)
    .replace(/(.{4})/g, '$1 ')
    .trim()
}

function formatarValidade(valor: string) {
  const numeros = valor.replace(/\D/g, '').slice(0, 4)

  if (numeros.length <= 2) {
    return numeros
  }

  return `${numeros.slice(0, 2)}/${numeros.slice(2)}`
}

function formatarCvv(valor: string) {
  return valor.replace(/\D/g, '').slice(0, 4)
}

type PagamentoPix = {
  paymentId: string
  status: string
  qrCode: string
  qrCodeBase64: string
}

export default function PaymentPage() {
  const { pedidoId } = useParams()
  const navigate = useNavigate()

  const [resumo, setResumo] = useState<{ subtotal: number; frete: number | null; total: number } | null>(null)
  const [metodoPagamento, setMetodoPagamento] = useState('PIX')
  const [carregando, setCarregando] = useState(false)
  const [numeroCartao, setNumeroCartao] = useState('')
  const [nomeTitular, setNomeTitular] = useState('')
  const [validade, setValidade] = useState('')
  const [cvv, setCvv] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [verificando, setVerificando] = useState(true)
  const [pagamentoPix, setPagamentoPix] = useState<PagamentoPix | null>(null)
  const [reservaEncerrada, setReservaEncerrada] = useState(false)
  const consultandoPix = useRef(false)

  useEffect(() => {
    let cancelado = false
    setVerificando(true)
    setResumo(null)
    setPagamentoPix(null)
    setReservaEncerrada(false)
    async function verificarPedido() {
      try {
        const response = await api.get(`/pedidos/${pedidoId}/meu`)

        if (cancelado) return
        if (response.data.status !== 'PENDENTE') {
          navigate('/meus-pedidos')
          return
        }
        setResumo(response.data)
      } catch {
        if (!cancelado) navigate('/meus-pedidos')
      } finally {
        if (!cancelado) setVerificando(false)
      }
    }

    verificarPedido()
    return () => { cancelado = true }
  }, [pedidoId, navigate])

  useEffect(() => {
    if (!verificando && resumo && metodoPagamento === 'PIX' && !pagamentoPix) {
      gerarPix()
    }
  }, [verificando, metodoPagamento, resumo])

  async function gerarPix() {
    if (consultandoPix.current || reservaEncerrada || !resumo || verificando) return
    setMensagem('')
    setPagamentoPix(null)

    if (!pedidoId) {
      setMensagem('Pedido inválido.')
      return
    }

    try {
      consultandoPix.current = true
      setCarregando(true)

      const response = await api.post('/pagamentos/criar', {
        pedidoId,
        metodoPagamento: 'PIX',
      })

      setPagamentoPix(response.data)
      if (['approved', 'cancelled', 'rejected'].includes(response.data.status)) {
        navigate('/meus-pedidos')
        return
      }
      setMensagem('PIX disponível. Novas consultas recuperam o mesmo pagamento.')
    } catch (error: any) {
      if (error.response?.status === 409) setReservaEncerrada(true)
      setMensagem(error.response?.data?.erro || 'Erro ao consultar pagamento PIX.')
    } finally {
      consultandoPix.current = false
      setCarregando(false)
    }
  }

  useEffect(() => {
    if (!pagamentoPix || !pedidoId) return
    const timer = window.setInterval(async () => {
      try {
        const response = await api.get(`/pedidos/${pedidoId}/meu`)
        if (response.data.status !== 'PENDENTE') navigate('/meus-pedidos')
      } catch {
        setMensagem('Não foi possível atualizar o status. Consulte seus pedidos novamente.')
      }
    }, 5000)
    return () => window.clearInterval(timer)
  }, [pagamentoPix, pedidoId, navigate])

  async function copiarCodigoPix() {
    if (!pagamentoPix?.qrCode) return

    await navigator.clipboard.writeText(pagamentoPix.qrCode)
    setMensagem('Código PIX copiado.')
  }

  async function confirmarPagamento() {
    setMensagem('')

    if (metodoPagamento === 'PIX') {
      await gerarPix()
      return
    }

    setMensagem(
      'Pagamento por cartão ainda será integrado com o Mercado Pago. Use PIX por enquanto.',
    )
  }

  if (verificando) {
    return (
      <section className="mx-auto max-w-4xl px-6 py-14">
        <p className="text-zinc-500">Verificando pedido...</p>
      </section>
    )
  }

  return (
    <section className="mx-auto max-w-4xl px-6 py-14">
      <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
        Pagamento
      </p>

      <h1 className="font-display text-5xl font-semibold">
        Finalizar pagamento
      </h1>

      <p className="mt-3 text-zinc-600 dark:text-zinc-300">
        Escolha uma forma de pagamento para concluir seu pedido.
      </p>

      {resumo && <div className="mt-6 space-y-2 rounded-2xl border border-black/10 p-4 dark:border-white/10">
        <p>Produtos: R$ {resumo.subtotal.toFixed(2).replace('.', ',')}</p>
        <p>Frete: {resumo.frete == null ? 'Não registrado (pedido anterior)' : `R$ ${resumo.frete.toFixed(2).replace('.', ',')}`}</p>
        <p className="font-semibold">Total: R$ {resumo.total.toFixed(2).replace('.', ',')}</p>
      </div>}
      <div className="mt-8 rounded-[2rem] border border-black/10 bg-white/70 p-8 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
        <label className="mb-4 block text-sm font-medium">
          Método de pagamento
        </label>

        <div className="grid gap-3 md:grid-cols-2">
          <button
            type="button"
            onClick={() => {
              setMetodoPagamento('PIX')
              setMensagem('')
            }}
            className={`rounded-2xl border p-4 text-left transition ${metodoPagamento === 'PIX'
              ? 'border-[#b98b7d] bg-[#f2ddd5] dark:bg-[#b98b7d]/20'
              : 'border-black/10 dark:border-white/10'
              }`}
          >
            <p className="font-medium">PIX</p>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
              QR Code gerado pelo Mercado Pago
            </p>
          </button>

          <button
            type="button"
            onClick={() => {
              setMetodoPagamento('CARTAO')
              setMensagem('')
              setPagamentoPix(null)
            }}
            className={`rounded-2xl border p-4 text-left transition ${metodoPagamento === 'CARTAO'
              ? 'border-[#b98b7d] bg-[#f2ddd5] dark:bg-[#b98b7d]/20'
              : 'border-black/10 dark:border-white/10'
              }`}
          >
            <p className="font-medium">Cartão de Crédito</p>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
              Integração Mercado Pago em breve
            </p>
          </button>
        </div>

        {metodoPagamento === 'PIX' && (
          <div className="mt-8 rounded-2xl border border-black/10 p-6 dark:border-white/10">
            {pagamentoPix?.qrCode ? (
              <>
                {pagamentoPix.qrCodeBase64 && (
                <div className="mx-auto flex h-64 w-64 items-center justify-center rounded-xl border border-black/10 bg-white p-4 text-center dark:border-white/10">
                  <img
                    src={`data:image/png;base64,${pagamentoPix.qrCodeBase64}`}
                    alt="QR Code PIX"
                    className="h-full w-full object-contain"
                  />
                </div>
                )}

                <p className="mt-4 text-center text-sm text-zinc-500 dark:text-zinc-400">
                  {pagamentoPix.qrCodeBase64 ? 'Escaneie o QR Code ou copie o código PIX abaixo.' : 'Copie o código PIX abaixo.'}
                </p>

                <div className="mt-4 rounded-2xl bg-black/5 p-4 dark:bg-white/5">
                  <p className="break-all font-mono text-xs text-zinc-700 dark:text-zinc-300">
                    {pagamentoPix.qrCode}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={copiarCodigoPix}
                  className="mt-4 w-full rounded-full border border-black/10 px-6 py-3 text-sm font-medium transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                >
                  Copiar código PIX
                </button>

                <p className="mt-4 text-center text-xs text-zinc-500 dark:text-zinc-400">
                  Status Mercado Pago: {pagamentoPix.status}
                </p>
              </>
            ) : (
              <div className="text-center">
                <div className="mx-auto flex h-56 w-56 items-center justify-center rounded-xl border border-black/10 bg-white text-center dark:border-white/10">
                  <div>
                    <p className="text-xs uppercase tracking-[0.25em] text-[#b98b7d]">
                      PIX
                    </p>
                    <p className="mt-2 text-sm text-zinc-500">
                      Clique em gerar pagamento para criar o QR Code.
                    </p>
                  </div>
                </div>

                <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
                  O QR Code será gerado com segurança pelo Mercado Pago.
                </p>
              </div>
            )}
          </div>
        )}

        {metodoPagamento === 'CARTAO' && (
          <div className="mt-8 mb-6">
            <div className="mx-auto max-w-md rounded-[2rem] bg-gradient-to-br from-zinc-900 via-zinc-800 to-[#b98b7d] p-6 text-white shadow-2xl">
              <div className="flex justify-between">
                <span className="text-sm opacity-70">Perussi</span>
                <span className="font-semibold">CARTÃO</span>
              </div>

              <div className="mt-10 text-2xl tracking-widest">
                {numeroCartao || '0000 0000 0000 0000'}
              </div>

              <div className="mt-8 flex justify-between">
                <div>
                  <p className="text-xs opacity-70">TITULAR</p>
                  <p>{nomeTitular.toUpperCase() || 'SEU NOME'}</p>
                </div>

                <div>
                  <p className="text-xs opacity-70">VALIDADE</p>
                  <p>{validade || 'MM/AA'}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {metodoPagamento === 'CARTAO' && (
          <div className="mt-8 space-y-4">
            <input
              value={numeroCartao}
              onChange={(event) =>
                setNumeroCartao(formatarNumeroCartao(event.target.value))
              }
              placeholder="Número do cartão"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <input
              value={nomeTitular}
              onChange={(event) => setNomeTitular(event.target.value)}
              placeholder="Nome do titular"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <div className="grid gap-4 md:grid-cols-2">
              <input
                value={validade}
                onChange={(event) =>
                  setValidade(formatarValidade(event.target.value))
                }
                placeholder="MM/AA"
                className="rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={cvv}
                onChange={(event) => setCvv(formatarCvv(event.target.value))}
                placeholder="CVV"
                className="rounded-xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />
            </div>

            <p className="text-sm text-zinc-500 dark:text-zinc-400">
              Por segurança, o pagamento por cartão será processado diretamente
              pelo Mercado Pago na próxima etapa da integração.
            </p>
          </div>
        )}

        {mensagem && (
          <p className="mt-6 rounded-2xl bg-[#f2ddd5] px-4 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
            {mensagem}
          </p>
        )}

        <button
          type="button"
          onClick={gerarPix}
          disabled={carregando || reservaEncerrada || !resumo || metodoPagamento !== 'PIX'}
          className="mt-6 w-full rounded-full bg-zinc-950 px-6 py-4 text-white disabled:opacity-50 dark:bg-white dark:text-zinc-950"
        >
          {carregando ? 'Consultando PIX...' : 'Gerar ou recuperar PIX'}
        </button>

        <Link to="/meus-pedidos" className="mt-4 block text-center text-sm underline">
          Voltar para Meus Pedidos
        </Link>

        {pagamentoPix && (
          <button
            type="button"
            onClick={() => navigate('/meus-pedidos')}
            className="mt-4 w-full rounded-full border border-black/10 px-6 py-4 text-sm font-medium transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
          >
            Já paguei, acompanhar pedido
          </button>
        )}
      </div>
    </section>
  )
}
