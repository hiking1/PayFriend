package com.payfriend.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.payfriend.core.network.TransactionDto
import com.payfriend.domain.model.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PayFriend") },
                actions = {
                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                    TextButton(onClick = {
                        viewModel.signOut()
                        onSignedOut()
                    }) { Text("Sign out") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onScan) { Text("Scan to Pay") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Demo wallet balance", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = state.balanceCents?.let { Money.format(it) } ?: "--",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text("Play money only. No real funds.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Text("Recent activity", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (state.loading && state.transactions.isEmpty()) {
                CircularProgressIndicator()
            } else if (state.transactions.isEmpty()) {
                Text("No transactions yet.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRow(tx, outgoing = tx.payerAccountId == state.walletId)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionDto, outgoing: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tx.merchantName, style = MaterialTheme.typography.bodyLarge)
                Text(tx.createdAt.take(16).replace('T', ' ') + " UTC", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = (if (outgoing) "-" else "+") + Money.format(tx.amountCents),
                style = MaterialTheme.typography.titleLarge,
                color = if (outgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
