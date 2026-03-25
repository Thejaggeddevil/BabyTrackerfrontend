package com.example.babyparenting.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.ui.theme.LocalAppColors

// ── Fixed readable colors (not dependent on dark gradient background) ─────────
private val LoginBg1     = Color(0xFFFFF0E6)
private val LoginBg2     = Color(0xFFFFE4D0)
private val LoginBg3     = Color(0xFFF5F0EB)
private val LoginText    = Color(0xFF2D1B0E)   // Dark brown — always readable
private val LoginMuted   = Color(0xFF7A5C4A)
private val LoginSurface = Color(0xFFFFFFFF)
private val LoginCoral   = Color(0xFFFF8B94)
private val LoginPeach   = Color(0xFFFFB06A)
private val LoginLavender= Color(0xFF9B8FD4)
private val LoginBorder  = Color(0xFFE0C8BC)
private val LoginRed     = Color(0xFFE53935)
private val LoginSubtle  = Color(0xFF5C3D2E)

@Composable
fun LoginScreen(
    onParentLogin: () -> Unit,
    onAdminLogin: (password: String) -> Boolean
) {
    var showAdminForm by remember { mutableStateOf(false) }
    var password      by remember { mutableStateOf("") }
    var showPwd       by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var attempts      by remember { mutableStateOf(0) }

    val logoScale = remember { Animatable(0f) }
    val pageAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        pageAlpha.animateTo(1f, tween(400))
    }

    // Light warm gradient — text is always visible on this
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(LoginBg1, LoginBg2, LoginBg3))
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp)
                .alpha(pageAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .scale(logoScale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(LoginCoral, LoginPeach))
                    )
            ) {
                Text("👶", fontSize = 38.sp)
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Baby Parenting Companion",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = LoginText,        // ← dark brown, always visible
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Track every milestone with love",
                fontSize  = 13.sp,
                color     = LoginMuted,        // ← muted brown, always visible
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(44.dp))

            if (!showAdminForm) {
                // ── Role selection ────────────────────────────────────────────
                Text(
                    "Who is using the app?",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = LoginMuted
                )
                Spacer(Modifier.height(20.dp))

                RoleCard(
                    emoji       = "👨‍👩‍👧",
                    title       = "I'm a Parent",
                    subtitle    = "Track my child's milestones",
                    accentColor = LoginCoral,
                    onClick     = onParentLogin
                )
                Spacer(Modifier.height(14.dp))
                RoleCard(
                    emoji       = "🛡️",
                    title       = "Admin Login",
                    subtitle    = "Manage & add milestones",
                    accentColor = LoginLavender,
                    onClick     = { showAdminForm = true }
                )

                Spacer(Modifier.height(32.dp))

                // Free trial badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LoginCoral.copy(alpha = 0.10f))
                        .border(1.dp, LoginCoral.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎁 ", fontSize = 16.sp)
                        Text(
                            "14 days FREE — No payment needed to start!",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = LoginSubtle,
                            textAlign  = TextAlign.Center
                        )
                    }
                }

            } else {
                // ── Admin password form ───────────────────────────────────────
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(LoginLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Admin Login",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = LoginText
                )
                Text(
                    "Enter your admin password",
                    fontSize  = 13.sp,
                    color     = LoginMuted,
                    textAlign = TextAlign.Center
                )

                if (attempts >= 3) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hint: default password is admin123",
                        fontSize = 11.sp,
                        color    = LoginRed
                    )
                }
                if (passwordError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Incorrect password. Try again.",
                        fontSize = 11.sp,
                        color    = LoginRed
                    )
                }

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it; passwordError = false },
                    label         = { Text("Password", color = LoginMuted) },
                    singleLine    = true,
                    visualTransformation = if (showPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = LoginMuted
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = LoginLavender,
                        unfocusedBorderColor    = LoginBorder,
                        focusedLabelColor       = LoginLavender,
                        unfocusedLabelColor     = LoginMuted,
                        cursorColor             = LoginCoral,
                        focusedTextColor        = LoginText,
                        unfocusedTextColor      = LoginText,
                        focusedContainerColor   = LoginSurface,
                        unfocusedContainerColor = LoginSurface
                    )
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val ok = onAdminLogin(password)
                        if (!ok) { passwordError = true; attempts++; password = "" }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = LoginLavender),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Login as Admin",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showAdminForm = false; password = ""; passwordError = false }) {
                    Text("← Back", color = LoginMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoginSurface)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
        ) {
            Text(emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = LoginText      // always dark readable text
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = LoginMuted
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor)
        ) {
            Text("→", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}