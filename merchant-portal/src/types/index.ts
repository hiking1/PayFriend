export type TransactionStatus = 'SETTLED' | 'REFUNDED' | 'FAILED'

export interface MerchantProfile {
  merchantId: string
  name: string
  uen: string
}

export interface MerchantTransaction {
  id: string
  reference: string
  customerRef: string
  amountCents: number
  status: TransactionStatus
  createdAt: string // ISO-8601
}

export interface DailySales {
  day: string // e.g. "Mon"
  amountCents: number
  count: number
}

export interface AuthUser {
  email: string
  merchant: MerchantProfile
}
