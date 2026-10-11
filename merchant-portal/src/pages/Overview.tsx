import { useEffect, useState } from 'react'
import Dashboard from '../components/Dashboard'
import QrGenerator from '../components/QrGenerator'
import { fetchDailySales, fetchTransactions } from '../services/api'
import type { DailySales, MerchantTransaction } from '../types'

export default function Overview() {
  const [sales, setSales] = useState<DailySales[]>([])
  const [transactions, setTransactions] = useState<MerchantTransaction[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchDailySales(), fetchTransactions()]).then(([s, t]) => {
      if (cancelled) return
      setSales(s)
      setTransactions(t)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Overview</h1>
      {loading ? <p className="text-gray-500">Loading...</p> : <Dashboard sales={sales} transactions={transactions} />}
      <QrGenerator />
    </div>
  )
}
