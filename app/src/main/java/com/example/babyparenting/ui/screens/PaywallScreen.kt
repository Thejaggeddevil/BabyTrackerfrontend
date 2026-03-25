package com.example.babyparenting.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.viewmodel.JourneyViewModel
import com.example.babyparenting.viewmodel.PaymentState

private val PayBg1     = Color(0xFFFFF0E6)
private val PayBg2     = Color(0xFFFFE4D0)
private val PayBg3     = Color(0xFFF5F0EB)
private val PayText    = Color(0xFF2D1B0E)
private val PayMuted   = Color(0xFFAA8877)
private val PayCoral   = Color(0xFFFF8B94)
private val PayPeach   = Color(0xFFFFB06A)
private val PayGreen   = Color(0xFF4CAF82)
private val PayRed     = Color(0xFFE53935)
private val PayGray    = Color(0xFF9E9E9E)

@Composable
fun PaywallScreen(
    viewModel: JourneyViewModel,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val paymentState by viewModel.paymentState.collectAsState()

    val pageAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        pageAlpha.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PayBg1, PayBg2, PayBg3)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .alpha(pageAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF5C3D2E))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .scale(logoScale.value)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PayCoral, PayPeach)))
            ) {
                Text("👶", fontSize = 38.sp)
            }

            Spacer(Modifier.height(20.dp))

            // Trial expired notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFEBEE))
                    .border(1.dp, PayRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⏰ ", fontSize = 16.sp)
                    Text(
                        "Aapka 14-din ka free trial khatam ho gaya",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = PayRed,
                        textAlign  = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Continue Your Child's Journey",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = PayText,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Subscribe for just ₹1 to keep tracking\nyour child's milestones",
                fontSize   = 14.sp,
                color      = PayMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Pricing card ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(2.dp, PayCoral.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PayCoral.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "30 DAYS ACCESS",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = PayCoral,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹1", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = PayText)
                        Text(" / 30 days", fontSize = 16.sp, color = PayMuted,
                            modifier = Modifier.padding(bottom = 10.dp))
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Renews every 30 days • Cancel anytime",
                        fontSize  = 11.sp,
                        color     = Color(0xFFBBBBBB),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Features list
                    listOf(
                        "Unlimited AI-powered milestone advice",
                        "76,000+ child development activities",
                        "Personalized parenting guidance",
                        "Progress tracking across 13 age groups",
                        "Safety & academics content (ages 0–12)"
                    ).forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(PayGreen.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Check, null, tint = PayGreen,
                                    modifier = Modifier.size(12.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(feature, fontSize = 13.sp, color = Color(0xFF3D2314))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Pay button ────────────────────────────────────────────────────
            when (paymentState) {
                is PaymentState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(54.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PayCoral, modifier = Modifier.size(32.dp))
                    }
                }
                is PaymentState.Success -> {
                    LaunchedEffect(Unit) { onPaymentSuccess() }
                    Box(modifier = Modifier.fillMaxWidth().height(54.dp),
                        contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = PayGreen, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Payment Successful! ✨", fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, color = PayGreen)
                        }
                    }
                }
                is PaymentState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFEBEE)).padding(12.dp)
                        ) {
                            Text(
                                (paymentState as PaymentState.Error).message,
                                fontSize = 13.sp, color = PayRed, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        PayButton(onClick = { viewModel.startPayment() })
                    }
                }
                else -> PayButton(onClick = { viewModel.startPayment() })
            }

            Spacer(Modifier.height(16.dp))

            // ── Trust badges ──────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TrustBadge("🔒 Secure")
                TrustBadge("💳 Razorpay")
                TrustBadge("↩️ Cancel Anytime")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Powered by Razorpay. Renews every 30 days for ₹1.\nCancel anytime from Settings.",
                fontSize  = 10.sp,
                color     = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PayButton(onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = PayCoral)
    ) {
        Text("Subscribe — ₹1 / 30 Days", fontSize = 16.sp,
            fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TrustBadge(text: String) {
    Text(text = text, fontSize = 10.sp, color = Color(0xFF9E9E9E))
}