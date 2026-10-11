package com.payfriend.data.repository

import com.payfriend.core.network.ErrorDto
import com.payfriend.core.network.LoginResponseDto
import com.payfriend.core.network.PayFriendApi
import com.payfriend.core.network.PaymentRequestDto
import com.payfriend.core.network.TransactionDto
import com.payfriend.core.network.WalletDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String) : ApiResult<Nothing>
}

/**
 * In-memory session. The JWT is deliberately NOT persisted: every app launch requires a fresh
 * biometric sign-in.
 */
@Singleton
class SessionStore @Inject constructor() {
    @Volatile
    var token: String? = null
        private set

    @Volatile
    var walletId: String? = null
        private set

    fun save(token: String, walletId: String) {
        this.token = token
        this.walletId = walletId
    }

    fun clear() {
        token = null
        walletId = null
    }
}

@Singleton
class PaymentRepository @Inject constructor(
    private val api: PayFriendApi,
    private val session: SessionStore,
    private val json: Json
) {
    suspend fun login(): ApiResult<LoginResponseDto> = call {
        api.demoLogin().also { session.save(it.token, it.walletId) }
    }

    suspend fun wallet(): ApiResult<WalletDto> = call { api.wallet() }

    suspend fun transactions(): ApiResult<List<TransactionDto>> = call { api.transactions() }

    suspend fun pay(request: PaymentRequestDto): ApiResult<TransactionDto> = try {
        val response = api.pay(request)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.Failure(parseError(response.errorBody()?.string(), response.code()))
        }
    } catch (e: IOException) {
        ApiResult.Failure(OFFLINE_MESSAGE)
    } catch (e: SerializationException) {
        ApiResult.Failure(BAD_RESPONSE_MESSAGE)
    }

    fun logout() = session.clear()

    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Failure(parseError(e.response()?.errorBody()?.string(), e.code()))
    } catch (e: IOException) {
        ApiResult.Failure(OFFLINE_MESSAGE)
    } catch (e: SerializationException) {
        ApiResult.Failure(BAD_RESPONSE_MESSAGE)
    }

    private fun parseError(body: String?, code: Int): String {
        val message = body?.let { runCatching { json.decodeFromString<ErrorDto>(it).message }.getOrNull() }
        return message?.takeIf { it.isNotBlank() } ?: "Request failed (HTTP $code)"
    }

    private companion object {
        const val OFFLINE_MESSAGE = "Can't reach the server. Check your connection and try again."
        const val BAD_RESPONSE_MESSAGE = "Unexpected response from the server."
    }
}
