import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { useTheme } from '../contexts/ThemeContext'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function MainLayout() {
  const { theme, toggleTheme } = useTheme()
  const { quantidadeTotal } = useCart()
  const navigate = useNavigate()
  const { user, isAuthenticated, isAdmin, logout } = useAuth()

  const [menuAberto, setMenuAberto] = useState(false)

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-[#f8f3ef] text-zinc-950 transition-colors dark:bg-zinc-950 dark:text-zinc-50">
      <header className="sticky top-0 z-50 border-b border-black/10 bg-[#f8f3ef]/80 backdrop-blur-xl transition-colors dark:border-white/10 dark:bg-zinc-950/80">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5">
          <Link
            to="/"
            className="font-display text-2xl font-semibold tracking-[0.45em] transition hover:text-[#b98b7d]"
          >
            PERUSSI
          </Link>

          <div className="hidden items-center gap-8 text-sm font-medium md:flex">
            <Link to="/produtos" className="transition hover:text-[#b98b7d]">
              Produtos
            </Link>

            <Link to="/favoritos" className="transition hover:text-[#b98b7d]">
              Favoritos
            </Link>

            {isAuthenticated && (
              <Link
                to="/meus-pedidos"
                className="transition hover:text-[#b98b7d]"
              >
                Meus Pedidos
              </Link>
            )}

            {isAdmin && (
              <>
                <Link to="/admin" className="transition hover:text-[#b98b7d]">
                  Dashboard
                </Link>

                <Link to="/admin/produtos" className="transition hover:text-[#b98b7d]">
                  Admin Produtos
                </Link>

                <Link to="/admin/pedidos" className="transition hover:text-[#b98b7d]">
                  Admin Pedidos
                </Link>
              </>
            )}

            <Link to="/sobre" className="transition hover:text-[#b98b7d]">
              Sobre
            </Link>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/favoritos"
              className="flex h-10 w-10 items-center justify-center rounded-full border border-black/10 bg-white/60 text-lg transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10 dark:bg-white/5"
              aria-label="Favoritos"
            >
              ♡
            </Link>

            <Link
              to="/carrinho"
              className="relative flex h-10 w-10 items-center justify-center rounded-full border border-black/10 bg-white/60 text-lg transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10 dark:bg-white/5"
              aria-label="Carrinho"
            >
              🛒

              {quantidadeTotal > 0 && (
                <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#b98b7d] px-1.5 text-xs font-semibold text-white">
                  {quantidadeTotal}
                </span>
              )}
            </Link>

            <button
              type="button"
              onClick={toggleTheme}
              className="flex h-10 min-w-10 items-center justify-center rounded-full border border-black/10 bg-white/60 px-3 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10 dark:bg-white/5"
              aria-label="Alternar tema"
            >
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>

            {isAuthenticated ? (
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setMenuAberto((aberto) => !aberto)}
                  className="flex items-center gap-2 rounded-full border border-black/10 bg-white/60 px-4 py-2.5 text-sm transition hover:border-[#b98b7d] hover:text-[#b98b7d] dark:border-white/10 dark:bg-white/5"
                >
                  Olá, {user?.nome}
                  <span className="text-xs">▼</span>
                </button>

                {menuAberto && (
                  <div className="absolute right-0 mt-2 w-52 overflow-hidden rounded-2xl border border-black/10 bg-white shadow-xl shadow-black/10 dark:border-white/10 dark:bg-zinc-900">
                    <Link
                      to="/perfil"
                      onClick={() => setMenuAberto(false)}
                      className="block px-4 py-3 text-sm transition hover:bg-black/5 dark:hover:bg-white/5"
                    >
                      Visualizar perfil
                    </Link>

                    <button
                      type="button"
                      onClick={() => {
                        handleLogout()
                        setMenuAberto(false)
                      }}
                      className="block w-full px-4 py-3 text-left text-sm text-red-500 transition hover:bg-black/5 dark:hover:bg-white/5"
                    >
                      Sair
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <Link
                to="/login"
                className="rounded-full bg-zinc-950 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-[#b98b7d] dark:bg-white dark:text-zinc-950 dark:hover:bg-[#e8c9bd]"
              >
                Entrar
              </Link>
            )}
          </div>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  )
}