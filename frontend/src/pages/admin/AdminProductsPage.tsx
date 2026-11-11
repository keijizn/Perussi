import { useNotification } from '../../contexts/NotificationContext'
import { ChangeEvent, FormEvent, useEffect, useState } from 'react'
import { api } from '../../services/api'

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

export default function AdminProductsPage() {
  const notificar = useNotification()
  const [produtos, setProdutos] = useState<Produto[]>([])
  const [produtoSelecionado, setProdutoSelecionado] = useState('')

  const [editandoId, setEditandoId] = useState('')
  const [editandoVariacaoId, setEditandoVariacaoId] = useState('')
  const [novoEstoque, setNovoEstoque] = useState('')

  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [preco, setPreco] = useState('')
  const [desconto, setDesconto] = useState('')
  const [categoria, setCategoria] = useState('')
  const [imagemUrl, setImagemUrl] = useState('')
  const [enviandoImagem, setEnviandoImagem] = useState(false)
  const [destaque, setDestaque] = useState(false)

  const [tamanho, setTamanho] = useState('')
  const [cor, setCor] = useState('')
  const [estoque, setEstoque] = useState('')

  const [mensagem, setMensagem] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [produtoAberto, setProdutoAberto] = useState<string | null>(null)

  async function carregarProdutos() {
    const response = await api.get('/produtos')
    setProdutos(response.data)
  }

  async function uploadImagem(
    event: ChangeEvent<HTMLInputElement>,
  ) {
    const arquivo = event.target.files?.[0]

    if (!arquivo) return

    try {
      setEnviandoImagem(true)

      const formData = new FormData()
      formData.append('file', arquivo)

      const response = await api.post(
        '/upload/imagem',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        },
      )

      setImagemUrl(response.data.url)
      setMensagem('')
      notificar('Imagem enviada com sucesso.')
    } catch {
      setMensagem('Erro ao enviar imagem.')
    } finally {
      setEnviandoImagem(false)
    }
  }

  useEffect(() => {
    carregarProdutos()
  }, [])

  async function salvarProduto(event: FormEvent) {
    event.preventDefault()
    setMensagem('')
    setCarregando(true)

    const body = {
      nome,
      descricao,
      preco: Number(preco),
      desconto: Number(desconto) || 0,
      categoria,
      imagemUrl,
      destaque,
      variacoes: [],
    }

    try {
      if (editandoId) {
        await api.put(`/produtos/${editandoId}`, body)
        setMensagem('')
        notificar('Produto atualizado com sucesso.')
      } else {
        await api.post('/produtos', body)
        setMensagem('')
        notificar('Produto cadastrado com sucesso.')
      }

      setEditandoId('')
      setNome('')
      setDescricao('')
      setPreco('')
      setDesconto('')
      setCategoria('')
      setImagemUrl('')
      setDestaque(false)

      await carregarProdutos()
    } catch {
      setMensagem('Erro ao salvar produto.')
    } finally {
      setCarregando(false)
    }
  }

  function editarProduto(produto: Produto) {
    setEditandoId(produto.id)
    setNome(produto.nome)
    setDescricao(produto.descricao)
    setPreco(String(produto.preco))
    setDesconto(String(produto.desconto || 0))
    setCategoria(produto.categoria)
    setImagemUrl(produto.imagemUrl || '')
    setDestaque(produto.destaque)
  }

  async function excluirProduto(id: string) {
    const confirmar = window.confirm('Deseja realmente excluir este produto?')

    if (!confirmar) return

    try {
      await api.delete(`/produtos/${id}`)
      await carregarProdutos()
      setMensagem('')
      notificar('Produto excluído com sucesso.')
    } catch {
      setMensagem('Erro ao excluir produto.')
    }
  }

  async function cadastrarVariacao(event: FormEvent) {
    event.preventDefault()
    setMensagem('')

    if (!produtoSelecionado) {
      setMensagem('Selecione um produto.')
      return
    }

    try {
      await api.post('/variacoes', {
        produtoId: produtoSelecionado,
        tamanho,
        cor,
        estoque: Number(estoque),
      })

      setTamanho('')
      setCor('')
      setEstoque('')

      await carregarProdutos()
      setMensagem('')
      notificar('Variação cadastrada com sucesso.')
    } catch {
      setMensagem('Erro ao cadastrar variação.')
    }
  }

  async function salvarEstoque(
    variacaoId: string,
    produtoId: string,
    tamanhoVariacao: string,
    corVariacao: string,
  ) {
    try {
      await api.put(`/variacoes/${variacaoId}`, {
        produtoId,
        tamanho: tamanhoVariacao,
        cor: corVariacao,
        estoque: Number(novoEstoque),
      })

      setEditandoVariacaoId('')
      setNovoEstoque('')

      await carregarProdutos()
      setMensagem('')
      notificar('Estoque atualizado com sucesso.')
    } catch {
      setMensagem('Erro ao atualizar estoque.')
    }
  }

  async function excluirVariacao(id: string) {
    const confirmar = window.confirm('Deseja realmente excluir esta variação?')

    if (!confirmar) return

    try {
      await api.delete(`/variacoes/${id}`)
      await carregarProdutos()
      setMensagem('')
      notificar('Variação excluída com sucesso.')
    } catch {
      setMensagem('Erro ao excluir variação.')
    }
  }

  return (
    <section className="mx-auto max-w-7xl px-6 py-14">
      <div className="mb-10">
        <p className="mb-3 text-sm uppercase tracking-[0.35em] text-[#b98b7d]">
          Administração
        </p>

        <h1 className="font-display text-5xl font-semibold">Produtos</h1>

        <p className="mt-3 text-zinc-600 dark:text-zinc-300">
          Cadastre produtos e gerencie variações do catálogo Perussi.
        </p>
      </div>

      {mensagem && (
        <div className="mb-8 rounded-2xl bg-[#f2ddd5] px-5 py-3 text-sm text-zinc-900 dark:bg-[#b98b7d]/20 dark:text-zinc-100">
          {mensagem}
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-2">
        <form
          onSubmit={salvarProduto}
          className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5"
        >
          <h2 className="font-display mb-6 text-3xl font-semibold">
            {editandoId ? 'Editar produto' : 'Novo produto'}
          </h2>

          <div className="space-y-4">
            <input
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              placeholder="Nome"
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <textarea
              value={descricao}
              onChange={(event) => setDescricao(event.target.value)}
              placeholder="Descrição"
              rows={4}
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            />

            <div className="grid gap-4 md:grid-cols-3">
              <input
                value={preco}
                onChange={(event) => setPreco(event.target.value)}
                placeholder="Preço"
                type="number"
                step="0.01"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={desconto}
                onChange={(event) => setDesconto(event.target.value)}
                placeholder="Desconto (%)"
                type="number"
                min="0"
                max="100"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={categoria}
                onChange={(event) => setCategoria(event.target.value)}
                placeholder="Categoria"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />
            </div>

            <div className="space-y-3">
              <input
                type="file"
                accept="image/*"
                onChange={uploadImagem}
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 dark:border-white/10 dark:bg-zinc-900"
              />

              {enviandoImagem && (
                <p className="text-sm text-zinc-500">
                  Enviando imagem...
                </p>
              )}

              {imagemUrl && (
                <img
                  src={imagemUrl}
                  alt="Preview"
                  className="h-40 rounded-xl border border-black/10 object-cover"
                />
              )}
            </div>

            <label className="flex items-center gap-3 text-sm">
              <input
                type="checkbox"
                checked={destaque}
                onChange={(event) => setDestaque(event.target.checked)}
              />
              Produto em destaque
            </label>

            <div className="flex gap-3">
              <button
                type="submit"
                disabled={carregando}
                className="flex-1 rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] disabled:opacity-50 dark:bg-white dark:text-zinc-950"
              >
                {carregando
                  ? 'Salvando...'
                  : editandoId
                    ? 'Atualizar produto'
                    : 'Cadastrar produto'}
              </button>

              {editandoId && (
                <button
                  type="button"
                  onClick={() => {
                    setEditandoId('')
                    setNome('')
                    setDescricao('')
                    setPreco('')
                    setDesconto('')
                    setCategoria('')
                    setImagemUrl('')
                    setDestaque(false)
                  }}
                  className="rounded-full border border-black/10 px-6 py-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                >
                  Cancelar
                </button>
              )}
            </div>
          </div>
        </form>

        <form
          onSubmit={cadastrarVariacao}
          className="rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5"
        >
          <h2 className="font-display mb-6 text-3xl font-semibold">
            Nova variação
          </h2>

          <div className="space-y-4">
            <select
              value={produtoSelecionado}
              onChange={(event) => setProdutoSelecionado(event.target.value)}
              className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
            >
              <option value="">Selecione um produto</option>
              {produtos.map((produto) => (
                <option key={produto.id} value={produto.id}>
                  {produto.nome}
                </option>
              ))}
            </select>

            <div className="grid gap-4 md:grid-cols-3">
              <input
                value={tamanho}
                onChange={(event) => setTamanho(event.target.value)}
                placeholder="Tamanho"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={cor}
                onChange={(event) => setCor(event.target.value)}
                placeholder="Cor"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />

              <input
                value={estoque}
                onChange={(event) => setEstoque(event.target.value)}
                placeholder="Estoque"
                type="number"
                className="w-full rounded-xl border border-black/10 bg-white px-4 py-3 outline-none focus:border-[#b98b7d] dark:border-white/10 dark:bg-zinc-900"
              />
            </div>

            <button
              type="submit"
              className="w-full rounded-full bg-zinc-950 px-6 py-3 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950"
            >
              Cadastrar variação
            </button>
          </div>
        </form>
      </div>

      <div className="mt-10 rounded-[2rem] border border-black/10 bg-white/70 p-6 shadow-xl shadow-black/5 dark:border-white/10 dark:bg-white/5">
        <h2 className="font-display mb-6 text-3xl font-semibold">
          Produtos cadastrados
        </h2>

        <div className="space-y-4">
          {produtos.map((produto) => (
            <div
              key={produto.id}
              className="rounded-2xl border border-black/10 p-4 dark:border-white/10"
            >
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <h3 className="font-display text-2xl font-semibold">
                    {produto.nome}
                  </h3>

                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    {produto.categoria} ·{' '}

                    {produto.desconto > 0 ? (
                      <>
                        <span className="line-through">
                          R$ {produto.preco.toFixed(2).replace('.', ',')}
                        </span>

                        <span className="ml-2 font-medium text-zinc-100">
                          R${' '}
                          {(
                            produto.preco *
                            (1 - (produto.desconto || 0) / 100)
                          )
                            .toFixed(2)
                            .replace('.', ',')}
                        </span>

                        <span className="ml-2 text-red-500">
                          (-{produto.desconto}%)
                        </span>
                      </>
                    ) : (
                      <span>
                        R$ {produto.preco.toFixed(2).replace('.', ',')}
                      </span>
                    )}
                  </p>
                </div>

                <div className="flex flex-wrap gap-2">
                  <span className="rounded-full bg-[#f2ddd5] px-4 py-2 text-sm dark:bg-[#b98b7d]/20">
                    {produto.variacoes.length} variação(ões)
                  </span>

                  <button
                    type="button"
                    onClick={() =>
                      setProdutoAberto(
                        produtoAberto === produto.id ? null : produto.id,
                      )
                    }
                    className="rounded-full border border-black/10 px-4 py-2 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                  >
                    {produtoAberto === produto.id
                      ? '▲ Ocultar'
                      : '▼ Ver variações'}
                  </button>

                  <button
                    type="button"
                    onClick={() => editarProduto(produto)}
                    className="rounded-full border border-blue-300 px-4 py-2 text-sm text-blue-500 transition hover:bg-blue-50 dark:hover:bg-blue-950/20"
                  >
                    Editar
                  </button>

                  <button
                    type="button"
                    onClick={() => excluirProduto(produto.id)}
                    className="rounded-full border border-red-300 px-4 py-2 text-sm text-red-500 transition hover:bg-red-50 dark:hover:bg-red-950/20"
                  >
                    Excluir
                  </button>
                </div>
              </div>

              {produto.variacoes.length > 0 && produtoAberto === produto.id && (
                <div className="mt-4 flex flex-col gap-2">
                  {produto.variacoes.map((variacao) => (
                    <div
                      key={variacao.id}
                      className="flex flex-wrap items-center gap-2 rounded-xl border border-black/10 p-2 dark:border-white/10"
                    >
                      <span className="text-sm font-medium">
                        {variacao.tamanho} · {variacao.cor}
                      </span>

                      {editandoVariacaoId === variacao.id ? (
                        <>
                          <input
                            type="number"
                            value={novoEstoque}
                            onChange={(event) =>
                              setNovoEstoque(event.target.value)
                            }
                            className="w-24 rounded border border-black/10 bg-white px-2 py-1 text-sm dark:border-white/10 dark:bg-zinc-900"
                          />

                          <button
                            type="button"
                            onClick={() =>
                              salvarEstoque(
                                variacao.id,
                                produto.id,
                                variacao.tamanho,
                                variacao.cor,
                              )
                            }
                            className="rounded-full bg-[#b98b7d] px-4 py-2 text-xs font-medium text-white transition hover:opacity-90"
                          >
                            Salvar
                          </button>

                          <button
                            type="button"
                            onClick={() => {
                              setEditandoVariacaoId('')
                              setNovoEstoque('')
                            }}
                            className="rounded-full border border-black/10 px-4 py-2 text-xs transition hover:border-zinc-400 dark:border-white/10"
                          >
                            Cancelar
                          </button>
                        </>
                      ) : (
                        <>
                          <span className="text-sm text-zinc-500 dark:text-zinc-400">
                            Estoque: {variacao.estoque}
                          </span>

                          <button
                            type="button"
                            onClick={() => {
                              setEditandoVariacaoId(variacao.id)
                              setNovoEstoque(String(variacao.estoque))
                            }}
                            className="rounded-full border border-black/10 px-4 py-2 text-xs transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10"
                          >
                            Editar Estoque
                          </button>
                        </>
                      )}

                      <button
                        type="button"
                        onClick={() => excluirVariacao(variacao.id)}
                        className="rounded-full border border-black/10 px-4 py-2 text-xs transition hover:border-red-400 hover:text-red-500 dark:border-white/10"
                      >
                        Excluir
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
