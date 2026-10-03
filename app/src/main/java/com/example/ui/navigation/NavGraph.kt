package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.timer.TimerScreen
import com.example.ui.timer.ManualJobScreen
import com.example.ui.customers.CustomerListScreen
import com.example.ui.customers.CustomerDetailScreen
import com.example.ui.services.ServiceListScreen
import com.example.ui.jobs.JobListScreen
import com.example.ui.jobs.JobDetailScreen
import com.example.ui.payments.PaymentListScreen
import com.example.ui.expenses.ExpenseListScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Setup : Screen("setup")
    object Dashboard : Screen("dashboard")
    object Timer : Screen("timer")
    object ManualJob : Screen("manual_job")
    object Customers : Screen("customers")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
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
}

@Composable
fun TimeBillNavGraph(
    navController: NavHostController,
    repository: TimeBillRepository,
    currentUserId: String,
    userProfile: UserProfile?,
    onSignOut: () -> Unit
) {
    val startRoute = Screen.Dashboard.route

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = { navController.navigate(Screen.Setup.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Setup.route) {
            SetupScreen(
                currentUserId = currentUserId,
                initialProfile = userProfile,
                onSaveComplete = { profile ->
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Setup.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) }
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
        composable(Screen.Customers.route) {
            CustomerListScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerDetailScreen(
                currentUserId = currentUserId,
                customerId = customerId,
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
        composable(Screen.Jobs.route) {
            JobListScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) },
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
        composable(Screen.Reports.route) {
            ReportsScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) },
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
        composable(Screen.Settings.route) {
            SettingsScreen(
                currentUserId = currentUserId,
                repository = repository,
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = onSignOut,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
