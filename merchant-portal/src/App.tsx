import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './hooks/useAuth'
import Login from './pages/Login'
import Overview from './pages/Overview'
import Transactions from './pages/Transactions'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-brand text-white' : 'text-gray-700 hover:bg-gray-200'
  }`

function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
          <div className="flex items-center gap-4">
            <span className="text-lg font-bold text-brand">PayFriend</span>
            <nav className="flex gap-2">
              <NavLink to="/" end className={linkClass}>
                Overview
              </NavLink>
              <NavLink to="/transactions" className={linkClass}>
                Transactions
              </NavLink>
            </nav>
          </div>
          <div className="flex items-center gap-3 text-sm text-gray-600">
            <span>{user?.merchant.name}</span>
            <button onClick={logout} className="rounded-md border px-3 py-1 hover:bg-gray-100">
              Sign out
            </button>
          </div>
        </div>
      </header>
      <div className="bg-amber-50 px-6 py-2 text-center text-xs text-amber-800">
        Technical demo running on mock data. No real payments.
      </div>
      <main className="mx-auto max-w-6xl p-6">
        <Outlet />
      </main>
    </div>
  )
}

function RequireAuth() {
  const { user } = useAuth()
  return user ? <Layout /> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<RequireAuth />}>
            <Route index element={<Overview />} />
            <Route path="transactions" element={<Transactions />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
