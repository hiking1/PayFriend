package com.payfriend.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.payfriend.presentation.auth.LoginScreen
import com.payfriend.presentation.dashboard.DashboardScreen
import com.payfriend.presentation.payment.PaymentConfirmationScreen
import com.payfriend.presentation.payment.PaymentReceiptScreen
import com.payfriend.presentation.payment.PaymentViewModel
import com.payfriend.presentation.payment.QrScannerScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SCAN = "scan"
    const val CONFIRM = "confirm"
    const val RECEIPT = "receipt"
}

@Composable
fun PayFriendNavGraph(
    navController: NavHostController = rememberNavController(),
    // Resolved here (outside NavHost) so it is scoped to the activity and shared by scan/confirm/receipt.
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onScan = {
                    paymentViewModel.reset()
                    navController.navigate(Routes.SCAN) { launchSingleTop = true }
                },
                onSignedOut = {
                    paymentViewModel.reset()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SCAN) {
            QrScannerScreen(
                viewModel = paymentViewModel,
                onClose = { navController.popBackStack() },
                onScanned = { navController.navigate(Routes.CONFIRM) { launchSingleTop = true } }
            )
        }

        composable(Routes.CONFIRM) {
            PaymentConfirmationScreen(
                viewModel = paymentViewModel,
                onBack = { navController.popBackStack() },
                onPaid = {
                    navController.navigate(Routes.RECEIPT) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RECEIPT) {
            PaymentReceiptScreen(
                viewModel = paymentViewModel,
                onDone = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
