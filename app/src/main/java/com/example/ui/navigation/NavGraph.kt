package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.customers.AddCustomerScreen
import com.example.ui.customers.CustomerDetailScreen
import com.example.ui.expenses.ExpenseListScreen
import com.example.ui.jobs.JobDetailScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.payments.PaymentListScreen
import com.example.ui.payments.ReceivePaymentScreen
import com.example.ui.services.ServiceListScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.timer.ManualJobScreen
import com.example.ui.timer.TimerScreen
import com.example.ui.settings.UpdatePasswordScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Setup : Screen("setup")
    object MainTabs : Screen("main_tabs")
    object Dashboard : Screen("dashboard")
    object Timer : Screen("timer")
    object ManualJob : Screen("manual_job")
    object Customers : Screen("customers")
    object AddCustomer : Screen("add_customer")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object ReceivePayment : Screen("receive_payment?customerId={customerId}") {
        fun createRoute(customerId: String = "") = if (customerId.isNotEmpty()) "receive_payment?customerId=$customerId" else "receive_payment"
    }
    object Services : Screen("services")
    object Jobs : Screen("jobs")
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object Payments : Screen("payments")
    object Expenses : Screen("expenses")
    object Reports : Screen("reports")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object UpdatePassword : Screen("update_password")
}

@Composable
fun TimeBillNavGraph(
    navController: NavHostController,
    repository: TimeBillRepository,
    currentUserId: String,
    userProfile: UserProfile?,
    isConnected: Boolean = true,
    onOfflineActionBlocked: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val startRoute = Screen.MainTabs.route

    val safeNavigate: (String) -> Unit = { route ->
        if (!isConnected && route != Screen.Timer.route) {
            onOfflineActionBlocked()
        } else {
            navController.navigate(route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.MainTabs.route) {
            MainTabContainerScreen(
                currentUserId = currentUserId,
                repository = repository,
                initialTab = 0,
                isConnected = isConnected,
                onOfflineActionBlocked = onOfflineActionBlocked,
                onNavigate = safeNavigate,
                onSignOut = onSignOut
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = { safeNavigate(Screen.Setup.route) }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.MainTabs.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Setup.route) {
            SetupScreen(
                currentUserId = currentUserId,
                initialProfile = userProfile,
                onSaveComplete = { profile ->
                    navController.navigate(Screen.MainTabs.route) { popUpTo(Screen.Setup.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Timer.route) {
            TimerScreen(
                currentUserId = currentUserId,
                repository = repository,
                onFinish = { navController.popBackStack() }
            )
        }
        composable(Screen.ManualJob.route) {
            ManualJobScreen(
                currentUserId = currentUserId,
                repository = repository,
                onFinish = { navController.popBackStack() }
            )
        }
        composable(Screen.AddCustomer.route) {
            AddCustomerScreen(
                currentUserId = currentUserId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerDetailScreen(
                currentUserId = currentUserId,
                customerId = customerId,
                repository = repository,
                onNavigate = safeNavigate,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ReceivePayment.route,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            ReceivePaymentScreen(
                currentUserId = currentUserId,
                initialCustomerId = customerId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Services.route) {
            ServiceListScreen(
                currentUserId = currentUserId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.JobDetail.route) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDetailScreen(
                currentUserId = currentUserId,
                jobId = jobId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Payments.route) {
            PaymentListScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = safeNavigate,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Expenses.route) {
            ExpenseListScreen(
                currentUserId = currentUserId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                currentUserId = currentUserId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UpdatePassword.route) {
            UpdatePasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
