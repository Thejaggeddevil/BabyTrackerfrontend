package com.example.babyparenting.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.babyparenting.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ── Auth steps ────────────────────────────────────────────────────────────────

sealed class AuthStep {
    object EnterDetails : AuthStep()    // Email + Password form
    object EnterOtp     : AuthStep()    // OTP input form
    object Done         : AuthStep()    // Success — navigate away
}

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    data class Success(val name: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authStep = MutableStateFlow<AuthStep>(AuthStep.EnterDetails)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    // Temp storage — needed after OTP verified
    private var pendingEmail    = ""
    private var pendingPassword = ""
    private var pendingName     = ""
    private var pendingPurpose  = ""   // "signup" or "login"

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun clearError() {
        if (_authState.value is AuthState.Error) _authState.value = AuthState.Idle
    }

    fun backToDetails() {
        _authStep.value  = AuthStep.EnterDetails
        _authState.value = AuthState.Idle
    }

    // ── Step 1: Send OTP ──────────────────────────────────────────────────────

    fun sendOtpForSignup(name: String, email: String, password: String) {
        if (name.isBlank()) {
            _authState.value = AuthState.Error("Please enter your name.")
            return
        }
        if (!email.contains("@")) {
            _authState.value = AuthState.Error("Please enter a valid email.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }

        pendingEmail    = email.trim()
        pendingPassword = password
        pendingName     = name.trim()
        pendingPurpose  = "signup"

        sendOtp(email.trim(), "signup")
    }

    fun sendOtpForLogin(email: String, password: String) {
        if (!email.contains("@")) {
            _authState.value = AuthState.Error("Please enter a valid email.")
            return
        }
        if (password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your password.")
            return
        }

        pendingEmail    = email.trim()
        pendingPassword = password
        pendingPurpose  = "login"

        sendOtp(email.trim(), "login")
    }

    private fun sendOtp(email: String, purpose: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val body = JSONObject().apply {
                    put("email",   email)
                    put("purpose", purpose)
                }
                val response = postRequest("$BASE_URL/send-otp", body.toString())
                if (response.optBoolean("success", false)) {
                    _authState.value = AuthState.Idle
                    _authStep.value  = AuthStep.EnterOtp   // OTP screen dikhao
                } else {
                    _authState.value = AuthState.Error(
                        response.optString("detail", "Failed to send OTP. Please try again.")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState.Error("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // ── Step 2: Verify OTP ────────────────────────────────────────────────────

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _authState.value = AuthState.Error("Please enter the 6-digit OTP.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val body = JSONObject().apply {
                    put("email",   pendingEmail)
                    put("otp",     otp)
                    put("purpose", pendingPurpose)
                }
                val response = postRequest("$BASE_URL/verify-otp", body.toString())

                if (response.optBoolean("verified", false)) {
                    // OTP sahi — ab register ya login karo
                    if (pendingPurpose == "signup") {
                        doRegister()
                    } else {
                        doLogin()
                    }
                } else {
                    _authState.value = AuthState.Error(
                        response.optString("detail", "Incorrect OTP. Please try again.")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState.Error("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // Resend OTP
    fun resendOtp() {
        sendOtp(pendingEmail, pendingPurpose)
    }

    // ── Step 3a: Register ─────────────────────────────────────────────────────

    private suspend fun doRegister() {
        try {
            val body = JSONObject().apply {
                put("email",    pendingEmail)
                put("password", pendingPassword)
                put("name",     pendingName)
            }
            val response = postRequest("$BASE_URL/register", body.toString())
            if (response.has("token")) {
                tokenManager.saveToken(response.getString("token"))
                tokenManager.saveUser(
                    email = response.optString("email", pendingEmail),
                    name  = response.optString("name", pendingName)
                )
                _authState.value = AuthState.Success(
                    name  = response.optString("name", pendingName),
                    email = response.optString("email", pendingEmail)
                )
            } else {
                _authState.value = AuthState.Error(
                    response.optString("detail", "Registration failed. Please try again.")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _authState.value = AuthState.Error("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // ── Step 3b: Login ────────────────────────────────────────────────────────

    private suspend fun doLogin() {
        try {
            val body = JSONObject().apply {
                put("email",    pendingEmail)
                put("password", pendingPassword)
            }
            val response = postRequest("$BASE_URL/login", body.toString())
            if (response.has("token")) {
                tokenManager.saveToken(response.getString("token"))
                tokenManager.saveUser(
                    email = response.optString("email", pendingEmail),
                    name  = response.optString("name", "")
                )
                _authState.value = AuthState.Success(
                    name  = response.optString("name", ""),
                    email = response.optString("email", pendingEmail)
                )
            } else {
                _authState.value = AuthState.Error(
                    response.optString("detail", "Login failed. Please try again.")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _authState.value = AuthState.Error("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun logout() {
        tokenManager.logout()
        _authState.value = AuthState.Idle
        _authStep.value  = AuthStep.EnterDetails
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private suspend fun postRequest(url: String, jsonBody: String): JSONObject {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput        = true
                connectTimeout  = 90_000   // 30 → 90 seconds (Render cold start ke liye)
                readTimeout     = 90_000   // 30 → 90 seconds
            }
            OutputStreamWriter(connection.outputStream).use { it.write(jsonBody) }
            val code   = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text   = stream.bufferedReader().readText()
            connection.disconnect()
            JSONObject(text)
        }
    }

    companion object {
        private const val BASE_URL = "http://192.168.1.7:8000"
    }
}