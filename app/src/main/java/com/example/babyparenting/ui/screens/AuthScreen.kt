package com.example.babyparenting.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.viewmodel.AuthState
import com.example.babyparenting.viewmodel.AuthStep
import com.example.babyparenting.viewmodel.AuthViewModel

// ── Warm fixed colors — dark/light mode se independent ───────────────────────
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

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val authStep  by viewModel.authStep.collectAsState()

    var isLoginMode by remember { mutableStateOf(true) }

    val logoScale = remember { Animatable(0f) }
    val pageAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        pageAlpha.animateTo(1f, tween(400))
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WarmBg1, WarmBg2, WarmBg3)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    .size(88.dp)
                    .scale(logoScale.value)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WarmCoral, WarmPeach)))
            ) {
                Text("👶", fontSize = 38.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Baby Parenting Companion",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = WarmText,
                textAlign  = TextAlign.Center
            )
            Text(
                "Track every milestone with love",
                fontSize  = 12.sp,
                color     = WarmMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Animated content — step switch ────────────────────────────────
            AnimatedContent(
                targetState = authStep,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "stepAnim"
            ) { step ->
                when (step) {
                    is AuthStep.EnterDetails -> DetailsForm(
                        isLoginMode = isLoginMode,
                        authState   = authState,
                        onToggleMode = {
                            isLoginMode = !isLoginMode
                            viewModel.clearError()
                        },
                        onSubmit = { name, email, password ->
                            if (isLoginMode) viewModel.sendOtpForLogin(email, password)
                            else viewModel.sendOtpForSignup(name, email, password)
                        },
                        onClearError = { viewModel.clearError() }
                    )
                    is AuthStep.EnterOtp -> OtpForm(
                        authState  = authState,
                        onVerify   = { otp -> viewModel.verifyOtp(otp) },
                        onResend   = { viewModel.resendOtp() },
                        onBack     = { viewModel.backToDetails() }
                    )
                    is AuthStep.Done -> {}
                }
            }
        }
    }
}

// ── Details form ──────────────────────────────────────────────────────────────

@Composable
private fun DetailsForm(
    isLoginMode: Boolean,
    authState: AuthState,
    onToggleMode: () -> Unit,
    onSubmit: (name: String, email: String, password: String) -> Unit,
    onClearError: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFD6C2).copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            WarmTabButton("Login",   isLoginMode,  { if (!isLoginMode) { onToggleMode(); onClearError() } }, Modifier.weight(1f))
            WarmTabButton("Sign Up", !isLoginMode, { if (isLoginMode)  { onToggleMode(); onClearError() } }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Form card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(WarmCard)
                .border(1.5.dp, WarmCoral.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isLoginMode) {
                    WarmField(name, { name = it; onClearError() }, "Your Name", Icons.Default.Person)
                }
                WarmField(email, { email = it; onClearError() }, "Email Address", Icons.Default.Email, KeyboardType.Email)
                WarmField(
                    value         = password,
                    onValueChange = { password = it; onClearError() },
                    label         = "Password",
                    icon          = Icons.Default.Lock,
                    keyboardType  = KeyboardType.Password,
                    isPassword    = true,
                    showPassword  = showPwd,
                    onTogglePwd   = { showPwd = !showPwd }
                )
            }
        }

        // Error
        if (authState is AuthState.Error) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmRed.copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
                Text(authState.message, fontSize = 13.sp, color = WarmRed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        // Submit button
        Button(
            onClick  = { onSubmit(name.trim(), email.trim(), password) },
            enabled  = authState !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = WarmCoral)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (isLoginMode) "Send OTP & Login" else "Send OTP & Register",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row {
            Text(
                if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                fontSize = 13.sp, color = WarmMuted
            )
            Text(
                if (isLoginMode) "Sign Up" else "Login",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = WarmCoral,
                modifier   = Modifier.clickable { onToggleMode(); onClearError() }
            )
        }
    }
}

// ── OTP form ──────────────────────────────────────────────────────────────────

@Composable
private fun OtpForm(
    authState: AuthState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Back button
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = WarmMuted)
            }
        }

        // Email icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(WarmCoral.copy(alpha = 0.12f))
        ) {
            Icon(Icons.Default.MarkEmailRead, null, tint = WarmCoral, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text("Check your email!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WarmText)
        Spacer(Modifier.height(6.dp))
        Text(
            "We've sent a 6-digit OTP to your email.\nEnter it below to continue.",
            fontSize  = 13.sp,
            color     = WarmMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        // OTP card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(WarmCard)
                .border(1.5.dp, WarmCoral.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter OTP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmMuted)

                OutlinedTextField(
                    value         = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    placeholder   = { Text("• • • • • •", fontSize = 20.sp, color = WarmBorder, letterSpacing = 8.sp) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = WarmText,
                        textAlign     = TextAlign.Center,
                        letterSpacing = 8.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = WarmCoral,
                        unfocusedBorderColor    = WarmBorder,
                        focusedContainerColor   = WarmCard,
                        unfocusedContainerColor = WarmCard,
                        cursorColor             = WarmCoral
                    )
                )

                // Resend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Resend OTP",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = WarmCoral,
                        modifier   = Modifier.clickable { onResend(); otp = "" }
                    )
                }
            }
        }

        // Error
        if (authState is AuthState.Error) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmRed.copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
                Text(authState.message, fontSize = 13.sp, color = WarmRed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        // Verify button
        Button(
            onClick  = { onVerify(otp) },
            enabled  = otp.length == 6 && authState !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = WarmCoral)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Verify OTP ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "OTP expires in 10 minutes",
            fontSize = 11.sp,
            color    = WarmMuted
        )
    }
}

// ── Tab button ────────────────────────────────────────────────────────────────

@Composable
private fun WarmTabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) WarmCard else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) WarmText else WarmMuted
        )
    }
}

// ── Warm text field ───────────────────────────────────────────────────────────

@Composable
private fun WarmField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePwd: (() -> Unit)? = null
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = WarmCoral, modifier = Modifier.size(18.dp)) },
        trailingIcon  = if (isPassword) {{
            IconButton(onClick = { onTogglePwd?.invoke() }) {
                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = WarmMuted)
            }
        }} else null,
        singleLine           = true,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(12.dp),
        colors               = OutlinedTextFieldDefaults.colors(
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
    )
}