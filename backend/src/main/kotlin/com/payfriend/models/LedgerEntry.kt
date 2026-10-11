package com.payfriend.models

import kotlinx.serialization.Serializable

/**
 * One side of a double-entry posting.
 *
 * Every payment produces exactly two entries with the same [transactionId]:
 * a DEBIT on the payer wallet and a CREDIT on the merchant account, for the same amount.
 * Entries are append-only. Corrections are made with compensating entries, never updates.
 */
@Serializable
data class LedgerEntry(
    val id: String,
    val transactionId: String,
    val accountId: String,
    val direction: Direction,
    val amountCents: Long,
    val currency: String = "SGD",
    val createdAt: String
) {
    @Serializable
    enum class Direction { DEBIT, CREDIT }
}
