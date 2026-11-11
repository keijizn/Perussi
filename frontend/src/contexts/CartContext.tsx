import { createContext, useContext, useEffect, useState } from 'react'
import { api } from '../services/api'
import { useAuth } from './AuthContext'
import { useNotification } from './NotificationContext'

type CartItem = {
  id: string
  produtoId: string
  nomeProduto: string
  imagemUrl: string
  preco: number
  desconto: number
  precoComDesconto: number
  variacaoId: string
  tamanho: string
  cor: string
  quantidade: number
  estoqueDisponivel: number
}

type CartContextType = {
  itens: CartItem[]
  quantidadeTotal: number
  totalCarrinho: number
  carregando: boolean
  carregarCarrinho: () => Promise<void>
  removerItem: (itemId: string) => Promise<void>
  atualizarQuantidade: (
    itemId: string,
    quantidade: number,
  ) => Promise<void>
}

const CartContext = createContext<CartContextType | null>(null)

export function CartProvider({ children }: { children: React.ReactNode }) {
  const notificar = useNotification()
  const { isAuthenticated } = useAuth()
  const [itens, setItens] = useState<CartItem[]>([])
  const [carregando, setCarregando] = useState(false)

  async function carregarCarrinho() {
    if (!isAuthenticated) {
      setItens([])
      return
    }

    setCarregando(true)

    try {
      const response = await api.get('/carrinho')
      setItens(response.data)
    } finally {
      setCarregando(false)
    }
  }

  async function removerItem(itemId: string) {
    try {
      await api.delete(`/carrinho/${itemId}`)
      setItens((atuais) => atuais.filter((item) => item.id !== itemId))
      notificar('Produto removido do carrinho.')
    } catch {
      notificar('Não foi possível remover o produto. Tente novamente.', 'erro')
    }
  }

  async function atualizarQuantidade(itemId: string, quantidade: number) {
    try {
      await api.put(`/carrinho/${itemId}`, { quantidade })
      await carregarCarrinho()
      notificar('Quantidade atualizada.')
    } catch {
      notificar('Não foi possível atualizar a quantidade. Confira o estoque e tente novamente.', 'erro')
    }
  }

  useEffect(() => {
    carregarCarrinho()
  }, [isAuthenticated])

  const quantidadeTotal = itens.reduce(
    (total, item) => total + item.quantidade,
    0,
  )

  const totalCarrinho = itens.reduce(
    (total, item) =>
      total + item.precoComDesconto * item.quantidade,
    0,
  )

  return (
    <CartContext.Provider
      value={{
        itens,
        quantidadeTotal,
        totalCarrinho,
        carregando,
        carregarCarrinho,
        removerItem,
        atualizarQuantidade,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const context = useContext(CartContext)

  if (!context) {
    throw new Error('useCart deve ser usado dentro de CartProvider')
  }

  return context
}
