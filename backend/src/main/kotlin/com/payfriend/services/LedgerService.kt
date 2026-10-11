package com.payfriend.services

import com.payfriend.models.LedgerEntry
import com.payfriend.models.PaymentRequest
import com.payfriend.models.Transaction
import com.payfriend.repository.TransactionRepository
import java.time.Instant
import java.util.UUID

/** Result of a payment attempt. Routes translate these into HTTP statuses. */
sealed interface PayOutcome {
    data class Created(val transaction: Transaction) : PayOutcome
    data class Replayed(val transaction: Transaction) : PayOutcome
    data class Rejected(val code: String, val message: String) : PayOutcome
}

/**
 * Double-entry payment engine with idempotency.
 *
 * Concurrency: payments are serialised with an in-process lock, which is only correct for a
 * single backend instance. Running several instances needs database-level locking
 * (see docs/ARCHITECTURE.md, "Known limitations").
 */
class LedgerService(
    private val repository: TransactionRepository,
    private val audit: AuditService
) {
    private val lock = Any()

    /** Creates a demo wallet with an opening balance the first time it is seen. */
    fun ensureWallet(walletId: String) {
        synchronized(lock) {
            if (repository.hasEntries(walletId)) return
            val now = Instant.now().toString()
            val txId = UUID.randomUUID().toString()
            val tx = Transaction(
                id = txId,
                idempotencyKey = "funding-$walletId",
                payerAccountId = SYSTEM_FUNDING_ACCOUNT,
                merchantId = "demo-topup",
                merchantName = "Demo top-up",
                amountCents = DEMO_OPENING_BALANCE_CENTS,
                description = "Demo opening balance (no real funds)",
                createdAt = now
            )
            repository.append(tx, postings(txId, SYSTEM_FUNDING_ACCOUNT, walletId, DEMO_OPENING_BALANCE_CENTS, now))
            audit.record("system", "WALLET_FUNDED", "wallet=$walletId amount=$DEMO_OPENING_BALANCE_CENTS")
        }
    }

    fun balanceCents(walletId: String): Long = repository.balanceCents(walletId)

    fun transactionsFor(walletId: String, limit: Int = 50): List<Transaction> =
        repository.listForAccount(walletId, limit)

    fun transactionForPayer(id: String, walletId: String): Transaction? =
        repository.findById(id)?.takeIf { it.payerAccountId == walletId }

    fun pay(payerWalletId: String, request: PaymentRequest): PayOutcome {
        validate(request)?.let { return it }

        synchronized(lock) {
            repository.findByIdempotencyKey(request.idempotencyKey)?.let { existing ->
                val same = existing.payerAccountId == payerWalletId &&
                    existing.merchantId == request.merchantId &&
                    existing.amountCents == request.amountCents
                return if (same) {
                    PayOutcome.Replayed(existing)
                } else {
                    PayOutcome.Rejected("IDEMPOTENCY_CONFLICT", "Idempotency key was already used for a different payment")
                }
            }

            if (repository.balanceCents(payerWalletId) < request.amountCents) {
                return PayOutcome.Rejected("INSUFFICIENT_FUNDS", "Insufficient wallet balance")
            }

            val now = Instant.now().toString()
            val txId = UUID.randomUUID().toString()
            val tx = Transaction(
                id = txId,
                idempotencyKey = request.idempotencyKey,
                payerAccountId = payerWalletId,
                merchantId = request.merchantId,
                merchantName = request.merchantName,
                amountCents = request.amountCents,
                description = request.description,
                createdAt = now
            )
            repository.append(tx, postings(txId, payerWalletId, merchantAccount(request.merchantId), request.amountCents, now))
            audit.record(payerWalletId, "PAYMENT_SETTLED", "tx=$txId merchant=${request.merchantId} amount=${request.amountCents}")
            return PayOutcome.Created(tx)
        }
    }

    private fun validate(r: PaymentRequest): PayOutcome.Rejected? = when {
        !KEY_REGEX.matches(r.idempotencyKey) ->
            PayOutcome.Rejected("INVALID_REQUEST", "idempotencyKey must be 8-100 characters of A-Z a-z 0-9 _ -")
        !MERCHANT_ID_REGEX.matches(r.merchantId) ->
            PayOutcome.Rejected("INVALID_REQUEST", "merchantId must be 1-64 characters of A-Z a-z 0-9 _ -")
        r.merchantName.isBlank() || r.merchantName.length > 120 ->
            PayOutcome.Rejected("INVALID_REQUEST", "merchantName must be 1-120 characters")
        (r.description?.length ?: 0) > 255 ->
            PayOutcome.Rejected("INVALID_REQUEST", "description must be at most 255 characters")
        r.amountCents <= 0 || r.amountCents > MAX_AMOUNT_CENTS ->
            PayOutcome.Rejected("INVALID_AMOUNT", "amountCents must be between 1 and $MAX_AMOUNT_CENTS")
        else -> null
    }

    private fun postings(txId: String, debitAccount: String, creditAccount: String, cents: Long, now: String) = listOf(
        LedgerEntry(UUID.randomUUID().toString(), txId, debitAccount, LedgerEntry.Direction.DEBIT, cents, createdAt = now),
        LedgerEntry(UUID.randomUUID().toString(), txId, creditAccount, LedgerEntry.Direction.CREDIT, cents, createdAt = now)
    )

    companion object {
        const val SYSTEM_FUNDING_ACCOUNT = "system:demo-funding"
        const val DEMO_OPENING_BALANCE_CENTS = 100_000L // SGD 1,000.00 of play money
        const val MAX_AMOUNT_CENTS = 1_000_000L // SGD 10,000.00 per payment
        private val KEY_REGEX = Regex("^[A-Za-z0-9_-]{8,100}$")
        private val MERCHANT_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")

        fun merchantAccount(merchantId: String) = "merchant:$merchantId"
        fun walletOf(userId: String) = "wallet:$userId"
    }
}
