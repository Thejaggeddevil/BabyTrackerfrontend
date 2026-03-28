package com.example.babyparenting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel  // ✅ ADD THIS IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.babyparenting.data.local.SessionManager
import com.example.babyparenting.data.local.SubscriptionManager
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
import com.example.babyparenting.ui.screens.millionaire.MillionaireClubScreen
import com.example.babyparenting.ui.screens.millionaire.StrategyDetailScreen
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel

object Routes {
    const val LOGIN         = "login"
    const val AUTH          = "auth"
    const val ONBOARDING    = "onboarding"
    const val JOURNEY       = "journey"
    const val ADVICE        = "advice"
    const val PAYWALL       = "paywall"
    const val SETTINGS      = "settings"
    const val ADMIN         = "admin"
    const val PARENT_HUB    = "parent_hub"
    const val PARENT_DETAIL = "parent_detail"
    const val MILLIONAIRE = "millionaire"
    const val STRATEGY_DETAIL = "strategy_detail"
}

@Composable
fun AppNavigation(
    onToggleTheme: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val context   = LocalContext.current
    val session   = remember { SessionManager(context) }
    val subMgr    = remember { SubscriptionManager(context) }   // ← trial manager
    val journeyVm: JourneyViewModel = viewModel()
    val adminVm:   AdminViewModel   = viewModel()
    val parentVm:  ParentViewModel  = viewModel()
    val authVm:    AuthViewModel    = viewModel()

    val startDestination =
      when {
      authVm.isLoggedIn() && journeyVm.getChildName().isNotBlank() -> Routes.JOURNEY
        authVm.isLoggedIn()                                          -> Routes.ONBOARDING
        else                                                         -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login — role selection ─────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onParentLogin = { navController.navigate(Routes.AUTH) },
                onAdminLogin  = { password ->
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

                    // ── TRIAL ACTIVATION ──────────────────────────────────────
                    // Pehli baar login → 14-din trial shuru karo.
                    // subMgr.activateTrial() internally checks: agar already
                    // started hai toh kuch nahi karta (idempotent).
                    subMgr.activateTrial()
                    // ─────────────────────────────────────────────────────────

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
                onMillionaireTapped = { navController.navigate(Routes.MILLIONAIRE) },  // ✅ FIXED: Added callback
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

        // ── Paywall (shown after trial expires) ───────────────────────────────
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
                    authVm.logout()
                    session.logout()
                    //journeyVm.clearAllData()
                    // NOTE: We do NOT reset the trial on logout.
                    // If user logs back in, their trial continues from where it left off.
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

        // ── Millionaire Club ─────────────────────────────────────────────────
        // ✅ FIXED: Changed viewModel() to hiltViewModel()
        composable(Routes.MILLIONAIRE) {
            val vm: MillionaireViewModel = hiltViewModel()  // ✅ USE HILT VERSION

            MillionaireClubScreen(
                viewModel = vm,
                onStrategyClick = { strategyId ->
                    navController.navigate("${Routes.STRATEGY_DETAIL}/$strategyId")
                },
                onActivityClick = { activityId, strategyId ->
                    // abhi ignore kar sakti hai
                }
            )
        }

        // ── Strategy Detail ─────────────────────────────────────────────────
        // ✅ FIXED: Changed viewModel() to hiltViewModel()
        composable("${Routes.STRATEGY_DETAIL}/{id}") { backStackEntry ->
            val vm: MillionaireViewModel = hiltViewModel()  // ✅ USE HILT VERSION
            val id = backStackEntry.arguments?.getString("id")?.toInt() ?: 0

            StrategyDetailScreen(
                strategyId = id,
                strategyTitle = "Strategy",
                viewModel = vm,
                onActivityClick = { _, _ -> },
                onBackClick = { navController.popBackStack() }
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