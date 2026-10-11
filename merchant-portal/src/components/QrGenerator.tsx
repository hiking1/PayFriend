import { useMemo, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { useAuth } from '../hooks/useAuth'
import type { MerchantProfile } from '../types'

/**
 * Builds the payload the Android app parses (see QrPaymentData.parse):
 *   PAYFRIEND|<merchantId>|<merchantName with "_" for spaces>|<UEN>|<amount or empty>
 * Returns null when the amount text is not a valid positive amount with at most 2 decimals.
 */
export function buildQrPayload(merchant: MerchantProfile, amountText: string): string | null {
  const trimmed = amountText.trim()
  let amount = ''
  if (trimmed !== '') {
    if (!/^\d+(\.\d{1,2})?$/.test(trimmed) || Number(trimmed) <= 0) return null
    amount = Number(trimmed).toFixed(2)
  }
  const name = merchant.name.trim().replace(/[|\s]+/g, '_')
  return `PAYFRIEND|${merchant.merchantId}|${name}|${merchant.uen}|${amount}`
}

export default function QrGenerator() {
  const { user } = useAuth()
  const [amount, setAmount] = useState('')

  const payload = useMemo(() => (user ? buildQrPayload(user.merchant, amount) : null), [user, amount])

  return (
    <section className="rounded-xl bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-gray-900">Payment QR</h2>
      <label className="block text-sm font-medium text-gray-700">
        Amount (SGD), leave empty to let the customer type it
        <input
          inputMode="decimal"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="e.g. 12.50"
          className="mt-1 w-full max-w-xs rounded-md border px-3 py-2"
        />
      </label>

      {payload ? (
        <div className="mt-6 flex flex-col items-start gap-3">
          <QRCodeSVG value={payload} size={192} />
          <code className="break-all rounded bg-gray-100 p-2 text-xs">{payload}</code>
        </div>
      ) : (
        <p className="mt-4 text-sm text-red-600">Enter a valid amount such as 12.50, or leave it empty.</p>
      )}
    </section>
  )
}
