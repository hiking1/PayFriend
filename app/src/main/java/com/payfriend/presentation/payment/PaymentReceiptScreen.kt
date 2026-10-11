package com.payfriend.presentation.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfriend.domain.model.Money

@Composable
fun PaymentReceiptScreen(
    viewModel: PaymentViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val receipt = state.receipt

    // The system back button should land on a refreshed dashboard, not on a stale one.
    BackHandler { onDone() }

    LaunchedEffect(receipt) {
        if (receipt == null) onDone()
    }
    if (receipt == null) return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Payment successful", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(Money.format(receipt.amountCents), style = MaterialTheme.typography.headlineLarge)
        Text("paid to ${receipt.merchantName}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("Reference: ${receipt.id.take(8).uppercase()}", style = MaterialTheme.typography.bodyLarge)
        Text(receipt.createdAt.take(19).replace('T', ' ') + " UTC", style = MaterialTheme.typography.bodyLarge)
        Text("Demo transaction. No real money moved.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}
