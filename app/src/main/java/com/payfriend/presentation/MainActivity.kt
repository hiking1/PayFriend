package com.payfriend.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.payfriend.BuildConfig
import com.payfriend.presentation.navigation.PayFriendNavGraph
import com.payfriend.ui.theme.PayFriendTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (a ComponentActivity subclass) because BiometricPrompt requires it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            // Block screenshots and the recents thumbnail for a payments app.
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            PayFriendTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PayFriendNavGraph()
                }
            }
        }
    }
}
