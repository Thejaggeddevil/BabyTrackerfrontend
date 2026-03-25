package com.example.babyparenting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.babyparenting.data.local.SessionManager
import com.example.babyparenting.ui.screens.AdminPanelScreen
import com.example.babyparenting.ui.screens.AdviceScreen
import com.example.babyparenting.ui.screens.AuthScreen
import com.example.babyparenting.ui.screens.BabyJourneyScreen
import com.example.babyparenting.ui.screens.LoginScreen
import com.example.babyparenting.ui.screens.OnboardingScreen
import com.example.babyparenting.ui.screens.ParentGuideDetailScreen
import com.example.babyparenting.ui.screens.ParentHubScreen
import com.example.babyparenting.ui.screens.PaywallScreen
import com.example.babyparenting.ui.screens.SettingsScreen
import com.example.babyparenting.viewmodel.AdminViewModel
import com.example.babyparenting.viewmodel.AuthViewModel
import com.example.babyparenting.viewmodel.JourneyViewModel
import com.example.babyparenting.viewmodel.ParentViewModel

object Routes {
    const val LOGIN         = "login"
    const val AUTH          = "auth"          // ← NEW: Email/Password Login+Register
    const val ONBOARDING    = "onboarding"
    const val JOURNEY       = "journey"
    const val ADVICE        = "advice"
    const val PAYWALL       = "paywall"
    const val SETTINGS      = "settings"
    const val ADMIN         = "admin"
    const val PARENT_HUB    = "parent_hub"
    const val PARENT_DETAIL = "parent_detail"
}

@Composable
fun AppNavigation(
    onToggleTheme: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val context   = LocalContext.current
    val session   = remember { SessionManager(context) }
    val journeyVm: JourneyViewModel = viewModel()
    val adminVm:   AdminViewModel   = viewModel()
    val parentVm:  ParentViewModel  = viewModel()
    val authVm:    AuthViewModel    = viewModel()   // ← NEW

    // Start destination — JWT token hai toh seedha journey/onboarding
    val startDestination = when {
        authVm.isLoggedIn() && journeyVm.getChildName().isNotBlank() -> Routes.JOURNEY
        authVm.isLoggedIn()                                          -> Routes.ONBOARDING
        else                                                         -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login — role selection ─────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onParentLogin = {
                    // "I'm a Parent" → AuthScreen (Login/Register)
                    navController.navigate(Routes.AUTH)
                },
                onAdminLogin = { password ->
                    val ok = adminVm.loginFromStart(password)
                    if (ok) {
                        navController.navigate(Routes.ADMIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                    ok
                }
            )
        }

        // ── Auth — Email + Password Login / Register ──────────────────────────
        composable(Routes.AUTH) {
            AuthScreen(
                viewModel     = authVm,
                onAuthSuccess = {
                    session.setLoggedIn(true)
                    val hasProfile = journeyVm.getChildName().isNotBlank()
                    if (hasProfile) {
                        navController.navigate(Routes.JOURNEY) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = { name, ageMonths ->
                    journeyVm.setChildName(name)
                    journeyVm.setChildAge(ageMonths)
                    navController.navigate(Routes.JOURNEY) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Journey map ───────────────────────────────────────────────────────
        composable(Routes.JOURNEY) {
            val showPaywall by journeyVm.showPaywall.collectAsState()
            if (showPaywall) {
                navController.navigate(Routes.PAYWALL)
                journeyVm.dismissPaywall()
            }

            BabyJourneyScreen(
                viewModel         = journeyVm,
                onMilestoneTapped = { milestone ->
                    journeyVm.onMilestoneTapped(milestone)
                    if (journeyVm.canAccessAdvice()) {
                        navController.navigate(Routes.ADVICE)
                    }
                },
                onSettingsTapped  = { navController.navigate(Routes.SETTINGS) },
                onParentHubTapped = { navController.navigate(Routes.PARENT_HUB) },
                onToggleTheme     = onToggleTheme
            )
        }

        // ── Advice ────────────────────────────────────────────────────────────
        composable(Routes.ADVICE) {
            AdviceScreen(
                viewModel = journeyVm,
                onBack    = {
                    journeyVm.resetAdvice()
                    navController.popBackStack()
                }
            )
        }

        // ── Paywall ───────────────────────────────────────────────────────────
        composable(Routes.PAYWALL) {
            PaywallScreen(
                viewModel        = journeyVm,
                onBack           = {
                    journeyVm.resetPaymentState()
                    navController.popBackStack()
                },
                onPaymentSuccess = {
                    navController.navigate(Routes.ADVICE) {
                        popUpTo(Routes.PAYWALL) { inclusive = true }
                    }
                }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = journeyVm,
                onBack    = { navController.popBackStack() },
                onLogout  = {
                    authVm.logout()        // JWT token clear
                    session.logout()       // Session clear
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Parent Hub ────────────────────────────────────────────────────────
        composable(Routes.PARENT_HUB) {
            ParentHubScreen(
                viewModel       = parentVm,
                onBack          = { navController.popBackStack() },
                onGuideSelected = { guide ->
                    parentVm.openGuide(guide)
                    navController.navigate(Routes.PARENT_DETAIL)
                }
            )
        }

        // ── Parent Guide Detail ───────────────────────────────────────────────
        composable(Routes.PARENT_DETAIL) {
            val guide = parentVm.selectedGuide.collectAsState().value
            if (guide != null) {
                ParentGuideDetailScreen(
                    guide  = guide,
                    onBack = {
                        parentVm.closeGuide()
                        navController.popBackStack()
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        // ── Admin panel ───────────────────────────────────────────────────────
        composable(Routes.ADMIN) {
            AdminPanelScreen(
                viewModel = adminVm,
                onBack    = {
                    journeyVm.refreshAfterAdminEdit()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}