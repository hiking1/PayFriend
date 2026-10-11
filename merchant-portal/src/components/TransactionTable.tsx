import { formatSgd } from '../services/api'
import type { MerchantTransaction, TransactionStatus } from '../types'

const badge: Record<TransactionStatus, string> = {
  SETTLED: 'bg-green-100 text-green-800',
  REFUNDED: 'bg-amber-100 text-amber-800',
  FAILED: 'bg-red-100 text-red-800',
}

export default function TransactionTable({ transactions }: { transactions: MerchantTransaction[] }) {
  if (transactions.length === 0) {
    return <p className="rounded-xl bg-white p-6 text-gray-500 shadow-sm">No transactions found.</p>
  }
  return (
    <div className="overflow-x-auto rounded-xl bg-white shadow-sm">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b bg-gray-50 text-gray-600">
          <tr>
            <th className="px-4 py-3 font-medium">Reference</th>
            <th className="px-4 py-3 font-medium">Time</th>
            <th className="px-4 py-3 font-medium">Customer</th>
            <th className="px-4 py-3 text-right font-medium">Amount</th>
            <th className="px-4 py-3 font-medium">Status</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => (
            <tr key={tx.id} className="border-b last:border-0">
              <td className="px-4 py-3 font-mono">{tx.reference}</td>
              <td className="px-4 py-3">{new Date(tx.createdAt).toLocaleString()}</td>
              <td className="px-4 py-3">{tx.customerRef}</td>
              <td className="px-4 py-3 text-right">{formatSgd(tx.amountCents)}</td>
              <td className="px-4 py-3">
                <span className={`rounded-full px-2 py-1 text-xs font-medium ${badge[tx.status]}`}>{tx.status}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
