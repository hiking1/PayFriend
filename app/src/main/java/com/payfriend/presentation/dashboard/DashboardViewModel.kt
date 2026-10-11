package com.payfriend.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payfriend.core.network.TransactionDto
import com.payfriend.data.repository.ApiResult
import com.payfriend.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val walletId: String? = null,
    val balanceCents: Long? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val wallet = repository.wallet()
            val transactions = repository.transactions()
            _state.update { current ->
                var next = current.copy(loading = false)
                when (wallet) {
                    is ApiResult.Success -> next = next.copy(walletId = wallet.value.walletId, balanceCents = wallet.value.balanceCents)
                    is ApiResult.Failure -> next = next.copy(error = wallet.message)
                }
                when (transactions) {
                    is ApiResult.Success -> next = next.copy(transactions = transactions.value)
                    is ApiResult.Failure -> if (next.error == null) next = next.copy(error = transactions.message)
                }
                next
            }
        }
    }

    fun signOut() = repository.logout()
}
