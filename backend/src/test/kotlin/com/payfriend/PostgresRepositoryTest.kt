package com.payfriend

import com.payfriend.models.LedgerEntry
import com.payfriend.models.PaymentRequest
import com.payfriend.models.Transaction
import com.payfriend.repository.ExposedTransactionRepository
import com.payfriend.services.AuditService
import com.payfriend.services.LedgerService
import com.payfriend.services.PayOutcome
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.junit.Assume.assumeTrue
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Integration tests that run the ledger against a REAL PostgreSQL.
 *
 * They are skipped unless TEST_DB_URL is set, so `gradle test` still works on a machine without a
 * database. CI sets TEST_DB_URL (see .github/workflows/ci.yml). Every test uses fresh random ids,
 * so the tests can be re-run against the same database without cleanup.
 */
class PostgresRepositoryTest {

    private fun withRepository(block: (ExposedTransactionRepository) -> Unit) {
        val jdbcUrl = System.getenv("TEST_DB_URL")?.takeIf { it.isNotBlank() }
        assumeTrue("TEST_DB_URL is not set: skipping PostgreSQL tests", jdbcUrl != null)

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = System.getenv("TEST_DB_USER") ?: "payfriend"
            password = System.getenv("TEST_DB_PASSWORD") ?: "payfriend_dev"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
        }
        HikariDataSource(config).use { dataSource ->
            val database = Database.connect(dataSource)
            block(ExposedTransactionRepository(database))
        }
    }

    private fun uniqueId(prefix: String) = prefix + UUID.randomUUID().toString().replace("-", "").take(16)

    private fun request(key: String, merchantId: String, cents: Long) = PaymentRequest(
        idempotencyKey = key,
        merchantId = merchantId,
        merchantName = "DB Test Merchant",
        amountCents = cents
    )

    @Test
    fun paymentIsPostedAsBalancedEntriesAndBalancesAreDerived() = withRepository { repo ->
        val ledger = LedgerService(repo, AuditService())
        val wallet = LedgerService.walletOf(uniqueId("user"))
        val merchantId = uniqueId("M")

        ledger.ensureWallet(wallet)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS, ledger.balanceCents(wallet))

        val outcome = ledger.pay(wallet, request(uniqueId("key"), merchantId, 1_250))
        assertIs<PayOutcome.Created>(outcome)

        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS - 1_250, ledger.balanceCents(wallet))
        assertEquals(1_250, repo.balanceCents(LedgerService.merchantAccount(merchantId)))
        // The stored transaction can be read back and is visible in the wallet history.
        assertEquals(outcome.transaction, repo.findById(outcome.transaction.id))
        assertEquals(true, ledger.transactionsFor(wallet).any { it.id == outcome.transaction.id })
    }

    @Test
    fun idempotencyRecordSurvivesANewRepositoryInstance() = withRepository { repo ->
        val wallet = LedgerService.walletOf(uniqueId("user"))
        val merchantId = uniqueId("M")
        val key = uniqueId("key")

        val firstLedger = LedgerService(repo, AuditService())
        firstLedger.ensureWallet(wallet)
        val first = firstLedger.pay(wallet, request(key, merchantId, 2_000))
        assertIs<PayOutcome.Created>(first)

        // A brand new service and repository over the same database behaves like a server restart.
        val restartedLedger = LedgerService(ExposedTransactionRepository(repo.database), AuditService())
        val replay = restartedLedger.pay(wallet, request(key, merchantId, 2_000))
        assertIs<PayOutcome.Replayed>(replay)
        assertEquals(first.transaction.id, replay.transaction.id)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS - 2_000, restartedLedger.balanceCents(wallet))
    }

    @Test
    fun reusingAKeyForADifferentAmountIsRejected() = withRepository { repo ->
        val ledger = LedgerService(repo, AuditService())
        val wallet = LedgerService.walletOf(uniqueId("user"))
        val merchantId = uniqueId("M")
        val key = uniqueId("key")
        ledger.ensureWallet(wallet)

        assertIs<PayOutcome.Created>(ledger.pay(wallet, request(key, merchantId, 1_000)))
        val conflict = ledger.pay(wallet, request(key, merchantId, 3_000))
        assertIs<PayOutcome.Rejected>(conflict)
        assertEquals("IDEMPOTENCY_CONFLICT", conflict.code)
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS - 1_000, ledger.balanceCents(wallet))
    }

    @Test
    fun paymentAboveBalanceIsRejectedAndNothingIsWritten() = withRepository { repo ->
        val ledger = LedgerService(repo, AuditService())
        val wallet = LedgerService.walletOf(uniqueId("user"))
        val merchantId = uniqueId("M")
        ledger.ensureWallet(wallet)

        val outcome = ledger.pay(wallet, request(uniqueId("key"), merchantId, LedgerService.MAX_AMOUNT_CENTS))
        assertIs<PayOutcome.Rejected>(outcome)
        assertEquals("INSUFFICIENT_FUNDS", outcome.code)
        assertEquals(0, repo.balanceCents(LedgerService.merchantAccount(merchantId)))
        assertEquals(LedgerService.DEMO_OPENING_BALANCE_CENTS, ledger.balanceCents(wallet))
    }

    @Test
    fun databaseEnforcesUniqueKeysAndRollsBackAFailedAppend() = withRepository { repo ->
        val key = uniqueId("key")
        val now = Instant.now().toString()

        fun tx(id: String, payer: String) = Transaction(
            id = id,
            idempotencyKey = key,
            payerAccountId = payer,
            merchantId = "M-DBTEST",
            merchantName = "DB Test Merchant",
            amountCents = 500,
            createdAt = now
        )

        fun entries(txId: String, account: String) = listOf(
            LedgerEntry(UUID.randomUUID().toString(), txId, account, LedgerEntry.Direction.DEBIT, 500, createdAt = now)
        )

        val firstId = UUID.randomUUID().toString()
        val firstAccount = uniqueId("wallet:first")
        repo.append(tx(firstId, firstAccount), entries(firstId, firstAccount))

        // Same idempotency key again: the database must refuse it, and no entries may be left behind.
        val secondId = UUID.randomUUID().toString()
        val secondAccount = uniqueId("wallet:second")
        assertFailsWith<Exception> { repo.append(tx(secondId, secondAccount), entries(secondId, secondAccount)) }

        assertNull(repo.findById(secondId))
        assertEquals(0, repo.balanceCents(secondAccount))
        assertEquals(-500, repo.balanceCents(firstAccount))
    }
}
