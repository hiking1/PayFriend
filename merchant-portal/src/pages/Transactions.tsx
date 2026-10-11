import { useEffect, useMemo, useState } from 'react'
import TransactionTable from '../components/TransactionTable'
import { fetchTransactions } from '../services/api'
import type { MerchantTransaction } from '../types'

export default function Transactions() {
  const [transactions, setTransactions] = useState<MerchantTransaction[]>([])
  const [query, setQuery] = useState('')

  useEffect(() => {
    let cancelled = false
    fetchTransactions().then((t) => {
      if (!cancelled) setTransactions(t)
    })
    return () => {
      cancelled = true
    }
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (q === '') return transactions
    return transactions.filter(
      (t) => t.reference.toLowerCase().includes(q) || t.customerRef.toLowerCase().includes(q),
    )
  }, [transactions, query])

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Transactions</h1>
      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search by reference or customer"
        className="w-full max-w-sm rounded-md border bg-white px-3 py-2"
      />
      <TransactionTable transactions={filtered} />
    </div>
  )
}
