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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun LoginScreen(
    onParentLogin: () -> Unit,   // → AuthScreen (Login/Register)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1B30), Color(0xFF221F34), LocalAppColors.current.bgMain)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
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
                        Brush.linearGradient(
                            listOf(LocalAppColors.current.coral, LocalAppColors.current.peach)
                        )
                    )
            ) {
                Text("👶", fontSize = 38.sp)
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Baby Parenting Companion",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = LocalAppColors.current.textPrimary,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Track every milestone with love",
                fontSize  = 13.sp,
                color     = LocalAppColors.current.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(44.dp))

            if (!showAdminForm) {
                // ── Role selection ────────────────────────────────────────────
                Text(
                    "Who is using the app?",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = LocalAppColors.current.textSecondary
                )
                Spacer(Modifier.height(20.dp))

                // "I'm a Parent" → AuthScreen
                RoleCard(
                    emoji       = "👨‍👩‍👧",
                    title       = "I'm a Parent",
                    subtitle    = "Track my child's milestones",
                    accentColor = LocalAppColors.current.coral,
                    onClick     = onParentLogin   // ← goes to AuthScreen
                )
                Spacer(Modifier.height(14.dp))
                RoleCard(
                    emoji       = "🛡️",
                    title       = "Admin Login",
                    subtitle    = "Manage & add milestones",
                    accentColor = LocalAppColors.current.lavender,
                    onClick     = { showAdminForm = true }
                )

            } else {
                // ── Admin password form ───────────────────────────────────────
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(LocalAppColors.current.lavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Admin Login",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = LocalAppColors.current.textPrimary
                )
                Text(
                    "Enter your admin password",
                    fontSize  = 13.sp,
                    color     = LocalAppColors.current.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (attempts >= 3) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hint: default password is admin123",
                        fontSize = 11.sp,
                        color    = LocalAppColors.current.red
                    )
                }
                if (passwordError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Incorrect password. Try again.",
                        fontSize = 11.sp,
                        color    = LocalAppColors.current.red
                    )
                }

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it; passwordError = false },
                    label         = { Text("Password", color = LocalAppColors.current.textSecondary) },
                    singleLine    = true,
                    visualTransformation = if (showPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = LocalAppColors.current.textMuted
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = darkFieldColors()
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val ok = onAdminLogin(password)
                        if (!ok) { passwordError = true; attempts++; password = "" }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.lavender),
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
                    Text("← Back", color = LocalAppColors.current.textMuted, fontSize = 13.sp)
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
            .background(LocalAppColors.current.bgSurface)
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
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
                color      = LocalAppColors.current.textPrimary
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = LocalAppColors.current.textSecondary
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

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = LocalAppColors.current.lavender,
    unfocusedBorderColor    = LocalAppColors.current.border,
    focusedLabelColor       = LocalAppColors.current.lavender,
    unfocusedLabelColor     = LocalAppColors.current.textMuted,
    cursorColor             = LocalAppColors.current.coral,
    focusedTextColor        = LocalAppColors.current.textPrimary,
    unfocusedTextColor      = LocalAppColors.current.textPrimary,
    focusedContainerColor   = LocalAppColors.current.bgSurface,
    unfocusedContainerColor = LocalAppColors.current.bgSurface
)