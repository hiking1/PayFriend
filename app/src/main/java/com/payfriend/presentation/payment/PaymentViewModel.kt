package com.payfriend.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfriend.core.network.PaymentRequestDto
import com.payfriend.core.network.TransactionDto
import com.payfriend.core.security.BiometricAuthManager
import com.payfriend.data.repository.ApiResult
import com.payfriend.data.repository.PaymentRepository
import com.payfriend.domain.model.Money
import com.payfriend.domain.model.QrPaymentData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private fun newIdempotencyKey() = "pf-" + UUID.randomUUID()

data class PaymentUiState(
    val scanned: QrPaymentData? = null,
    val scanError: String? = null,
    val amountInput: String = "",
    /** Reused for retries of the same payment so a repeated tap can never double-charge. */
    val idempotencyKey: String = newIdempotencyKey(),
    val submitting: Boolean = false,
    val error: String? = null,
    val receipt: TransactionDto? = null
)

/** Shared by the scan, confirm and receipt screens (scoped to the activity, see NavGraph). */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: PaymentRepository,
    val biometric: BiometricAuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    /** Called for every camera frame that contains a QR code, so it must be cheap and idempotent. */
    fun onQrText(raw: String) {
        if (_state.value.scanned != null) return
        val data = QrPaymentData.parse(raw)
        if (data == null) {
            _state.update { it.copy(scanError = "That is not a valid PayFriend merchant QR code.") }
            return
        }
        _state.update {
            it.copy(
                scanned = data,
                scanError = null,
                amountInput = data.amountCents?.let(::centsToInput).orEmpty(),
                idempotencyKey = newIdempotencyKey(),
                error = null,
                receipt = null
            )
        }
    }

    fun onAmountChanged(text: String) {
        // A different amount is a different payment, so it gets a different idempotency key.
        _state.update { it.copy(amountInput = text, idempotencyKey = newIdempotencyKey(), error = null) }
    }

    /** The amount that will be charged: fixed by the QR, or typed by the customer. Null if invalid. */
    fun amountCents(): Long? {
        val s = _state.value
        return s.scanned?.amountCents ?: Money.parseToCents(s.amountInput)
    }

    fun onError(message: String) {
        _state.update { it.copy(error = message) }
    }

    /** Call only after biometric confirmation succeeded. */
    fun pay() {
        val s = _state.value
        val qr = s.scanned ?: return
        if (s.submitting) return
        val cents = amountCents()
        if (cents == null) {
            onError("Enter a valid amount, for example 12.50")
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val request = PaymentRequestDto(
                idempotencyKey = s.idempotencyKey,
                merchantId = qr.merchantId,
                merchantName = qr.merchantName,
                amountCents = cents
            )
            when (val result = repository.pay(request)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, receipt = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.message) }
            }
        }
    }

    fun clearScan() {
        _state.update { it.copy(scanned = null, scanError = null, amountInput = "", error = null) }
    }

    fun reset() {
        _state.value = PaymentUiState()
    }

    private fun centsToInput(cents: Long) = String.format(Locale.US, "%d.%02d", cents / 100, cents % 100)
}
