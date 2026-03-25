package com.example.babyparenting.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Warm colors — same palette as AuthScreen + Razorpay
private val WarmBg1    = Color(0xFFFFF0E6)
private val WarmBg2    = Color(0xFFFFE4D0)
private val WarmBg3    = Color(0xFFF5F0EB)
private val WarmCoral  = Color(0xFFFF8B94)
private val WarmPeach  = Color(0xFFFFB06A)
private val WarmText   = Color(0xFF2D1B0E)
private val WarmMuted  = Color(0xFFAA8877)
private val WarmBorder = Color(0xFFDDDDDD)
private val WarmCard   = Color.White
private val WarmRed    = Color(0xFFE53935)
private val WarmBlue   = Color(0xFF1565C0)

@Composable
fun OnboardingScreen(onComplete: (name: String, ageMonths: Int) -> Unit) {
    var childName by remember { mutableStateOf("") }
    var sliderVal by remember { mutableStateOf(0f) }
    var nameError by remember { mutableStateOf(false) }
    val months = sliderVal.roundToInt()

    val logoScale = remember { Animatable(0f) }
    val pageAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        pageAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WarmBg1, WarmBg2, WarmBg3)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .alpha(pageAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .scale(logoScale.value)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WarmCoral, WarmPeach)))
            ) {
                Icon(Icons.Default.ChildCare, null, tint = Color.White, modifier = Modifier.size(50.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("Welcome!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WarmText)
            Text("Baby Parenting Companion", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmCoral)
            Spacer(Modifier.height(4.dp))
            Text(
                "Powered by 76,000+ child development activities",
                fontSize  = 12.sp,
                color     = WarmMuted,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Form card ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmCard)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Set up your child's profile",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = WarmText
                )

                // Child name field
                OutlinedTextField(
                    value         = childName,
                    onValueChange = { childName = it; nameError = false },
                    label         = { Text("Child's name *") },
                    placeholder   = { Text("e.g. Arjun", color = WarmMuted) },
                    leadingIcon   = {
                        Icon(Icons.Default.Person, null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                    },
                    singleLine    = true,
                    isError       = nameError,
                    supportingText = if (nameError) {{
                        Text("Please enter your child's name", color = WarmRed, fontSize = 11.sp)
                    }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = warmFieldColors()
                )

                // Age slider
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("Child's current age", fontSize = 13.sp, color = WarmMuted)
                        Text(
                            formatAge(months),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = WarmCoral
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value         = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange    = 0f..144f,
                        steps         = 143,
                        colors        = SliderDefaults.colors(
                            thumbColor         = WarmCoral,
                            activeTrackColor   = WarmCoral,
                            inactiveTrackColor = Color(0xFFFFD6C2)
                        )
                    )
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Newborn", fontSize = 10.sp, color = WarmMuted)
                        Text("12 years", fontSize = 10.sp, color = WarmMuted)
                    }
                    if (months > 0) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE3F2FD))
                                .padding(10.dp)
                        ) {
                            Text(
                                "✓ All milestones up to ${formatAge(months)} will be auto-completed",
                                fontSize = 11.sp,
                                color    = WarmBlue
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Privacy note ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE3F2FD))
                    .padding(12.dp)
            ) {
                Text("🔒 ", fontSize = 13.sp)
                Text(
                    "Your data stays on this device. Firebase sync coming soon.",
                    fontSize   = 11.sp,
                    color      = WarmBlue,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── What's included ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarmCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("What's included:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmText)
                listOf(
                    "🍼  0–24 months: Baby activities & parent guides",
                    "🎠  2–5 years: Toddler activities & pre-academics",
                    "🛡️  Safety & body awareness (ages 4–12)",
                    "📚  School: Maths, Science, Social Studies",
                    "🏛️  Civics, Computer Science & Foreign Languages",
                    "🗣️  Language & Communication skills"
                ).forEach {
                    Text(it, fontSize = 11.sp, color = WarmMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── CTA button ────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (childName.isBlank()) nameError = true
                    else onComplete(childName.trim(), months)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = WarmCoral)
            ) {
                Text("Start the Journey ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Your progress is saved locally.",
                fontSize  = 10.sp,
                color     = WarmMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun warmFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = WarmCoral,
    unfocusedBorderColor    = WarmBorder,
    focusedLabelColor       = WarmCoral,
    unfocusedLabelColor     = WarmMuted,
    cursorColor             = WarmCoral,
    focusedTextColor        = WarmText,
    unfocusedTextColor      = WarmText,
    focusedContainerColor   = WarmCard,
    unfocusedContainerColor = WarmCard
)

private fun formatAge(months: Int): String = when {
    months == 0      -> "Newborn"
    months < 12      -> "$months month${if (months == 1) "" else "s"}"
    months == 12     -> "1 year"
    months % 12 == 0 -> "${months / 12} years"
    else             -> "${months / 12} yr ${months % 12} mo"
}