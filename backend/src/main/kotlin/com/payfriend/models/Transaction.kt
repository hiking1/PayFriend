package com.payfriend.models

import kotlinx.serialization.Serializable

/** Header record for a posting. The money movement itself lives in [LedgerEntry] rows. */
@Serializable
data class Transaction(
    val id: String,
    val idempotencyKey: String,
    val payerAccountId: String,
    val merchantId: String,
    val merchantName: String,
    val amountCents: Long,
    val currency: String = "SGD",
    val description: String? = null,
    val status: String = "SETTLED",
    val createdAt: String
)

@Serializable
data class PaymentRequest(
    val idempotencyKey: String,
    val merchantId: String,
    val merchantName: String,
    val amountCents: Long,
    val description: String? = null
)

@Serializable
data class LoginResponse(val token: String, val userId: String, val walletId: String)

@Serializable
data class WalletResponse(val walletId: String, val balanceCents: Long, val currency: String = "SGD")

@Serializable
data class HealthResponse(val status: String, val auditChainValid: Boolean)

@Serializable
data class ErrorResponse(val error: String, val message: String)
