import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { formatSgd } from '../services/api'
import type { DailySales, MerchantTransaction } from '../types'

interface DashboardProps {
  sales: DailySales[]
  transactions: MerchantTransaction[]
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-white p-5 shadow-sm">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-gray-900">{value}</p>
    </div>
  )
}

export default function Dashboard({ sales, transactions }: DashboardProps) {
  const settled = transactions.filter((t) => t.status === 'SETTLED')
  const weekTotal = sales.reduce((sum, d) => sum + d.amountCents, 0)
  const today = sales.length > 0 ? sales[sales.length - 1] : undefined
  const average = settled.length > 0 ? Math.round(weekTotal / settled.length) : 0
  const chartData = sales.map((d) => ({ day: d.day, amount: d.amountCents / 100 }))

  return (
    <section className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Today" value={formatSgd(today?.amountCents ?? 0)} />
        <StatCard label="Last 7 days" value={formatSgd(weekTotal)} />
        <StatCard label="Settled payments" value={String(settled.length)} />
        <StatCard label="Average payment" value={formatSgd(average)} />
      </div>

      <div className="rounded-xl bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-gray-900">Daily sales (SGD)</h2>
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="day" />
            <YAxis />
            <Tooltip formatter={(value) => `S$${Number(value).toFixed(2)}`} />
            <Bar dataKey="amount" fill="#00897B" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  )
}
