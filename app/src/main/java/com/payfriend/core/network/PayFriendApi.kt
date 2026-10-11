package com.payfriend.core.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class LoginResponseDto(val token: String, val userId: String, val walletId: String)

@Serializable
data class WalletDto(val walletId: String, val balanceCents: Long, val currency: String = "SGD")

@Serializable
data class TransactionDto(
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
data class PaymentRequestDto(
    val idempotencyKey: String,
    val merchantId: String,
    val merchantName: String,
    val amountCents: Long,
    val description: String? = null
)

@Serializable
data class ErrorDto(val error: String = "", val message: String = "")

/** Mirrors docs/API_SPEC.md. The Authorization header is added by an OkHttp interceptor. */
interface PayFriendApi {

    /** DEMO ONLY: stands in for real KYC / Singpass login. */
    @POST("api/v1/auth/demo-login")
    suspend fun demoLogin(): LoginResponseDto

    @GET("api/v1/wallets/me")
    suspend fun wallet(): WalletDto

    @GET("api/v1/transactions")
    suspend fun transactions(): List<TransactionDto>

    /** Returns the raw Response so 4xx bodies (INSUFFICIENT_FUNDS, ...) can be read. */
    @POST("api/v1/transactions")
    suspend fun pay(@Body body: PaymentRequestDto): Response<TransactionDto>
}
