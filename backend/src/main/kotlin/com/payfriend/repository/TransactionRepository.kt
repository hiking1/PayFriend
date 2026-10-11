package com.payfriend.repository

import com.payfriend.models.LedgerEntry
import com.payfriend.models.Transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Storage port for the ledger. Implementations MUST be append-only:
 * there is deliberately no update or delete operation.
 */
interface TransactionRepository {
    fun findByIdempotencyKey(key: String): Transaction?
    fun findById(id: String): Transaction?

    /** Transactions that have at least one ledger entry on [accountId], newest first. */
    fun listForAccount(accountId: String, limit: Int): List<Transaction>

    fun hasEntries(accountId: String): Boolean

    /** Credits minus debits for the account. */
    fun balanceCents(accountId: String): Long

    /** Persists the header and all entries atomically. */
    fun append(tx: Transaction, entries: List<LedgerEntry>)
}

/** Default repository: no external dependencies, used for tests and quick local runs. */
class InMemoryTransactionRepository : TransactionRepository {
    private val lock = Any()
    private val transactions = mutableListOf<Transaction>()
    private val entries = mutableListOf<LedgerEntry>()

    override fun findByIdempotencyKey(key: String): Transaction? =
        synchronized(lock) { transactions.firstOrNull { it.idempotencyKey == key } }

    override fun findById(id: String): Transaction? =
        synchronized(lock) { transactions.firstOrNull { it.id == id } }

    override fun listForAccount(accountId: String, limit: Int): List<Transaction> = synchronized(lock) {
        val ids = entries.filter { it.accountId == accountId }.map { it.transactionId }.toSet()
        transactions.filter { it.id in ids }.asReversed().take(limit)
    }

    override fun hasEntries(accountId: String): Boolean =
        synchronized(lock) { entries.any { it.accountId == accountId } }

    override fun balanceCents(accountId: String): Long = synchronized(lock) {
        entries.filter { it.accountId == accountId }.sumOf { signed(it) }
    }

    override fun append(tx: Transaction, entries: List<LedgerEntry>) {
        synchronized(lock) {
            transactions += tx
            this.entries += entries
        }
    }
}

private fun signed(e: LedgerEntry): Long =
    if (e.direction == LedgerEntry.Direction.CREDIT) e.amountCents else -e.amountCents

// ---------------------------------------------------------------------------
// PostgreSQL (Exposed)
// ---------------------------------------------------------------------------

object TransactionsTable : Table("transactions") {
    val id = varchar("id", 36)
    val idempotencyKey = varchar("idempotency_key", 100).uniqueIndex()
    val payerAccountId = varchar("payer_account_id", 100)
    val merchantId = varchar("merchant_id", 64)
    val merchantName = varchar("merchant_name", 120)
    val amountCents = long("amount_cents")
    val currency = varchar("currency", 3)
    val description = varchar("description", 255).nullable()
    val status = varchar("status", 16)
    val createdAt = varchar("created_at", 40) // ISO-8601 UTC, lexicographically sortable
    override val primaryKey = PrimaryKey(id)
}

object LedgerEntriesTable : Table("ledger_entries") {
    val id = varchar("id", 36)
    val transactionId = varchar("transaction_id", 36).index()
    val accountId = varchar("account_id", 100).index()
    val direction = varchar("direction", 6)
    val amountCents = long("amount_cents")
    val currency = varchar("currency", 3)
    val createdAt = varchar("created_at", 40)
    override val primaryKey = PrimaryKey(id)
}

class ExposedTransactionRepository(private val database: Database) : TransactionRepository {

    init {
        transaction(database) { SchemaUtils.create(TransactionsTable, LedgerEntriesTable) }
    }

    override fun findByIdempotencyKey(key: String): Transaction? = transaction(database) {
        TransactionsTable.selectAll()
            .where { TransactionsTable.idempotencyKey eq key }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    override fun findById(id: String): Transaction? = transaction(database) {
        TransactionsTable.selectAll()
            .where { TransactionsTable.id eq id }
            .map { it.toTransaction() }
            .singleOrNull()
    }

    override fun listForAccount(accountId: String, limit: Int): List<Transaction> = transaction(database) {
        val ids = LedgerEntriesTable.selectAll()
            .where { LedgerEntriesTable.accountId eq accountId }
            .map { it[LedgerEntriesTable.transactionId] }
            .distinct()
        if (ids.isEmpty()) {
            emptyList()
        } else {
            TransactionsTable.selectAll()
                .where { TransactionsTable.id inList ids }
                .orderBy(TransactionsTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toTransaction() }
        }
    }

    override fun hasEntries(accountId: String): Boolean = transaction(database) {
        !LedgerEntriesTable.selectAll()
            .where { LedgerEntriesTable.accountId eq accountId }
            .limit(1)
            .empty()
    }

    override fun balanceCents(accountId: String): Long = transaction(database) {
        LedgerEntriesTable.selectAll()
            .where { LedgerEntriesTable.accountId eq accountId }
            .fold(0L) { acc, row ->
                val cents = row[LedgerEntriesTable.amountCents]
                if (row[LedgerEntriesTable.direction] == LedgerEntry.Direction.CREDIT.name) acc + cents else acc - cents
            }
    }

    override fun append(tx: Transaction, entries: List<LedgerEntry>) {
        transaction(database) {
            TransactionsTable.insert {
                it[TransactionsTable.id] = tx.id
                it[TransactionsTable.idempotencyKey] = tx.idempotencyKey
                it[TransactionsTable.payerAccountId] = tx.payerAccountId
                it[TransactionsTable.merchantId] = tx.merchantId
                it[TransactionsTable.merchantName] = tx.merchantName
                it[TransactionsTable.amountCents] = tx.amountCents
                it[TransactionsTable.currency] = tx.currency
                it[TransactionsTable.description] = tx.description
                it[TransactionsTable.status] = tx.status
                it[TransactionsTable.createdAt] = tx.createdAt
            }
            entries.forEach { e ->
                LedgerEntriesTable.insert {
                    it[LedgerEntriesTable.id] = e.id
                    it[LedgerEntriesTable.transactionId] = e.transactionId
                    it[LedgerEntriesTable.accountId] = e.accountId
                    it[LedgerEntriesTable.direction] = e.direction.name
                    it[LedgerEntriesTable.amountCents] = e.amountCents
                    it[LedgerEntriesTable.currency] = e.currency
                    it[LedgerEntriesTable.createdAt] = e.createdAt
                }
            }
        }
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        idempotencyKey = this[TransactionsTable.idempotencyKey],
        payerAccountId = this[TransactionsTable.payerAccountId],
        merchantId = this[TransactionsTable.merchantId],
        merchantName = this[TransactionsTable.merchantName],
        amountCents = this[TransactionsTable.amountCents],
        currency = this[TransactionsTable.currency],
        description = this[TransactionsTable.description],
        status = this[TransactionsTable.status],
        createdAt = this[TransactionsTable.createdAt]
    )
}
