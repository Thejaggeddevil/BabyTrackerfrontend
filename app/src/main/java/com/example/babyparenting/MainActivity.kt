package com.example.babyparenting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.babyparenting.data.local.ThemeManager
import com.example.babyparenting.navigation.AppNavigation
import com.example.babyparenting.ui.theme.BabyParentingTheme
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.viewmodel.JourneyViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    // Same ViewModel instance jo Compose use karta hai
    private val journeyViewModel: JourneyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Razorpay preload — checkout screen fast open hoti hai
        Checkout.preload(applicationContext)

        // Activity bind karo taaki ViewModel Razorpay open kar sake
        journeyViewModel.bindActivity(this)

        val themeManager = ThemeManager(this)

        setContent {
            var isDark by remember { mutableStateOf(themeManager.isDarkMode()) }

            BabyParentingTheme(isDark = isDark) {
                val colors = LocalAppColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = colors.bgMain
                ) {
                    AppNavigation(
                        onToggleTheme = {
                            isDark = !isDark
                            themeManager.setDarkMode(isDark)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        journeyViewModel.bindActivity(this)
        journeyViewModel.refreshSubscriptionStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        journeyViewModel.unbindActivity()
    }

    // ── Razorpay callbacks ────────────────────────────────────────────────────

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        razorpayPaymentID?.let {
            journeyViewModel.onPaymentSuccess(it)
        }
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?) {
        journeyViewModel.onPaymentError(
            errorCode,
            errorDescription ?: "Payment failed"
        )
    }
}