package com.payfriend

import com.payfriend.models.PaymentRequest
import com.payfriend.repository.InMemoryTransactionRepository
import com.payfriend.services.AuditService
import com.payfriend.services.LedgerService
import com.payfriend.services.PayOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LedgerServiceTest {

    private val wallet = LedgerService.walletOf("user-1")
    private val audit = AuditService()
    private val repo = InMemoryTransactionRepository()
    private val ledger = LedgerService(repo, audit).also { it.ensureWallet(wallet) }

    private fun request(key: String = "key-0001-abcd", cents: Long = 1_250, merchant: String = "MERCH-001") =
        PaymentRequest(idempotencyKey = key, merchantId = merchant, merchantName = "Demo Cafe", amountCents = cents)

    @Test
    fun `new wallet gets the demo opening balance`() {
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS, ledger.balanceCents(wallet))
    }

    @Test
    fun `payment moves money between wallet and merchant and conserves value`() {
        val outcome = ledger.pay(wallet, request())
        assertIs<PayOutcome.Created>(outcome)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS - 1_250, ledger.balanceCents(wallet))
        assertEquals(1_250, repo.balanceCents(LedgerService.merchantAccount("MERCH-001")))
        // Across all accounts, credits minus debits must be zero.
        val total = listOf(wallet, LedgerService.merchantAccount("MERCH-001"), LedgerService.SYSTEM_FUNDING_ACCOUNT)
            .sumOf { repo.balanceCents(it) }
        assertEquals(0, total)
    }

    @Test
    fun `replaying the same idempotency key does not charge twice`() {
        val first = ledger.pay(wallet, request()) as PayOutcome.Created
        val second = ledger.pay(wallet, request())
        assertIs<PayOutcome.Replayed>(second)
        assertEquals(first.transaction.id, second.transaction.id)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS - 1_250, ledger.balanceCents(wallet))
    }

    @Test
    fun `reusing a key for a different amount is a conflict`() {
        ledger.pay(wallet, request(cents = 1_000))
        val outcome = ledger.pay(wallet, request(cents = 2_000))
        assertIs<PayOutcome.Rejected>(outcome)
        assertEquals("IDEMPOTENCY_CONFLICT", outcome.code)
    }

    @Test
    fun `payment above balance is rejected and changes nothing`() {
        val outcome = ledger.pay(wallet, request(cents = LedgerService.DEMO_OPENING_BALANCE_CENTS + 1))
        assertIs<PayOutcome.Rejected>(outcome)
        assertEquals("INSUFFICIENT_FUNDS", outcome.code)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS, ledger.balanceCents(wallet))
    }

    @Test
    fun `invalid amounts and keys are rejected`() {
        assertEquals("INVALID_AMOUNT", (ledger.pay(wallet, request(cents = 0)) as PayOutcome.Rejected).code)
        assertEquals("INVALID_AMOUNT", (ledger.pay(wallet, request(cents = -5)) as PayOutcome.Rejected).code)
        assertEquals("INVALID_REQUEST", (ledger.pay(wallet, request(key = "short")) as PayOutcome.Rejected).code)
    }

    @Test
    fun `audit chain stays valid after many events`() {
        repeat(25) { i -> ledger.pay(wallet, request(key = "key-loop-%04d".format(i), cents = 100)) }
        assertTrue(audit.verifyChain())
        assertTrue(audit.snapshot().size >= 26) // 1 funding + 25 payments
    }
}
