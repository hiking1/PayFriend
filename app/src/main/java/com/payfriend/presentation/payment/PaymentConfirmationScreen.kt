package com.payfriend.presentation.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfriend.domain.model.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmationScreen(
    viewModel: PaymentViewModel,
    onBack: () -> Unit,
    onPaid: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity
    val qr = state.scanned

    fun cancel() {
        viewModel.clearScan()
        onBack()
    }

    BackHandler { cancel() }

    LaunchedEffect(state.receipt) {
        if (state.receipt != null) onPaid()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm payment") },
                navigationIcon = {
                    IconButton(onClick = ::cancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (qr == null) {
                Text("There is nothing to pay. Scan a merchant QR code first.")
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = ::cancel) { Text("Back") }
                return@Column
            }

            Text("Paying", style = MaterialTheme.typography.bodyLarge)
            Text(qr.merchantName, style = MaterialTheme.typography.titleLarge)
            if (qr.merchantUen.isNotBlank()) {
                Text("UEN ${qr.merchantUen}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(24.dp))

            if (qr.amountCents != null) {
                Text("Amount", style = MaterialTheme.typography.bodyLarge)
                Text(Money.format(qr.amountCents), style = MaterialTheme.typography.headlineLarge)
            } else {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChanged,
                    label = { Text("Amount (SGD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(32.dp))
            if (state.submitting) {
                CircularProgressIndicator()
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val cents = viewModel.amountCents()
                        if (cents == null) {
                            viewModel.onError("Enter a valid amount, for example 12.50")
                        } else {
                            viewModel.biometric.authenticate(
                                activity = activity,
                                title = "Confirm payment",
                                subtitle = "Pay ${qr.merchantName} ${Money.format(cents)}",
                                onSuccess = viewModel::pay,
                                onError = viewModel::onError
                            )
                        }
                    }
                ) {
                    Text("Pay with biometrics")
                }
            }
        }
    }
}
