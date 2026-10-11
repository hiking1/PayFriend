import type { AuthUser, DailySales, MerchantProfile, MerchantTransaction, TransactionStatus } from '../types'

/**
 * MOCK DATA LAYER.
 * Everything here is generated locally so the portal runs without a backend.
 * Replace these functions with calls to the real merchant API when it exists.
 */

export const DEMO_MERCHANT: MerchantProfile = {
  merchantId: 'MERCH-001',
  name: 'PayFriend Demo Cafe',
  uen: '201912345A',
}

export const DEMO_EMAIL = 'merchant@payfriend.demo'

export function formatSgd(cents: number): string {
  return `S$${(cents / 100).toFixed(2)}`
}

/** Small deterministic PRNG so the demo data does not change on every reload. */
function mulberry32(seed: number): () => number {
  let a = seed
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

function buildTransactions(): MerchantTransaction[] {
  const rand = mulberry32(42)
  const now = Date.now()
  const result: MerchantTransaction[] = []
  for (let i = 0; i < 40; i++) {
    const ageHours = Math.floor(rand() * 24 * 7)
    const roll = rand()
    const status: TransactionStatus = roll > 0.95 ? 'FAILED' : roll > 0.9 ? 'REFUNDED' : 'SETTLED'
    result.push({
      id: `tx-${1000 + i}`,
      reference: `PF${(100000 + Math.floor(rand() * 899999)).toString()}`,
      customerRef: `cust-${Math.floor(rand() * 9000 + 1000)}`,
      amountCents: Math.floor(rand() * 4500 + 350),
      status,
      createdAt: new Date(now - ageHours * 3_600_000).toISOString(),
    })
  }
  return result.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

const TRANSACTIONS = buildTransactions()

export async function demoLogin(email: string, password: string): Promise<AuthUser> {
  await new Promise((resolve) => setTimeout(resolve, 200))
  if (email.trim().toLowerCase() !== DEMO_EMAIL || password.length === 0) {
    throw new Error(`Use ${DEMO_EMAIL} with any non-empty password (demo only).`)
  }
  return { email: DEMO_EMAIL, merchant: DEMO_MERCHANT }
}

export async function fetchTransactions(): Promise<MerchantTransaction[]> {
  return TRANSACTIONS
}

export async function fetchDailySales(): Promise<DailySales[]> {
  const buckets = new Map<string, DailySales>()
  const today = new Date()
  for (let offset = 6; offset >= 0; offset--) {
    const d = new Date(today)
    d.setDate(today.getDate() - offset)
    buckets.set(d.toDateString(), { day: DAY_NAMES[d.getDay()], amountCents: 0, count: 0 })
  }
  for (const tx of TRANSACTIONS) {
    if (tx.status !== 'SETTLED') continue
    const bucket = buckets.get(new Date(tx.createdAt).toDateString())
    if (bucket) {
      bucket.amountCents += tx.amountCents
      bucket.count += 1
    }
  }
  return Array.from(buckets.values())
}
