package com.example.babyparenting.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var childName  by remember { mutableStateOf("") }
    var yearsInput by remember { mutableStateOf("") }
    var monthsInput by remember { mutableStateOf("") }
    var nameError  by remember { mutableStateOf(false) }
    var ageError   by remember { mutableStateOf("") }

    // Compute total months from years + months fields
    val totalMonths by remember {
        derivedStateOf {
            val y = yearsInput.trim().toIntOrNull() ?: 0
            val m = monthsInput.trim().toIntOrNull() ?: 0
            y * 12 + m
        }
    }

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

                // ── Child name ────────────────────────────────────────────────
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

                // ── Age input — Years + Months ────────────────────────────────
                Column {
                    Text(
                        "Child's current age *",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = WarmText
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter years and months separately",
                        fontSize = 11.sp,
                        color    = WarmMuted
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Years field
                        OutlinedTextField(
                            value         = yearsInput,
                            onValueChange = {
                                // Max 12 years
                                val n = it.filter { c -> c.isDigit() }
                                if (n.isEmpty() || (n.toIntOrNull() ?: 0) <= 12) {
                                    yearsInput = n
                                    ageError = ""
                                }
                            },
                            label         = { Text("Years") },
                            placeholder   = { Text("0", color = WarmMuted) },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError       = ageError.isNotEmpty(),
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = warmFieldColors()
                        )

                        // Months field
                        OutlinedTextField(
                            value         = monthsInput,
                            onValueChange = {
                                // Max 11 months (0-11)
                                val n = it.filter { c -> c.isDigit() }
                                if (n.isEmpty() || (n.toIntOrNull() ?: 0) <= 11) {
                                    monthsInput = n
                                    ageError = ""
                                }
                            },
                            label         = { Text("Months") },
                            placeholder   = { Text("0", color = WarmMuted) },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError       = ageError.isNotEmpty(),
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = warmFieldColors()
                        )
                    }

                    if (ageError.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ageError, fontSize = 11.sp, color = WarmRed)
                    }

                    // Age preview
                    if (totalMonths > 0) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(WarmCoral.copy(alpha = 0.10f))
                                .border(1.dp, WarmCoral.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👶 ", fontSize = 14.sp)
                                Column {
                                    Text(
                                        "Age: ${formatAge(totalMonths)}",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = WarmCoral
                                    )
                                    Text(
                                        "Past milestones will be auto-completed",
                                        fontSize = 11.sp,
                                        color    = WarmMuted
                                    )
                                }
                            }
                        }
                    } else if (yearsInput.isEmpty() && monthsInput.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE3F2FD))
                                .padding(10.dp)
                        ) {
                            Text(
                                "💡 Leave both as 0 for a newborn",
                                fontSize = 11.sp,
                                color    = WarmBlue
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Free trial notice ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmCoral.copy(alpha = 0.10f))
                    .border(1.dp, WarmCoral.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("🎁 ", fontSize = 14.sp)
                Text(
                    "14 days FREE! Explore all features at no cost.",
                    fontSize   = 12.sp,
                    color      = Color(0xFF5C3D2E),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(8.dp))

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
                    var hasError = false
                    if (childName.isBlank()) {
                        nameError = true
                        hasError = true
                    }
                    val years = yearsInput.trim().toIntOrNull() ?: 0
                    val months = monthsInput.trim().toIntOrNull() ?: 0
                    if (years > 12 || (years == 12 && months > 0)) {
                        ageError = "Maximum age is 12 years"
                        hasError = true
                    }
                    if (!hasError) {
                        onComplete(childName.trim(), totalMonths)
                    }
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