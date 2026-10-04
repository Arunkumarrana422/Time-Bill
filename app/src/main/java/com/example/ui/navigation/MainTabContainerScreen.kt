package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.TimeBillRepository
import com.example.ui.customers.CustomerListScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.jobs.JobListScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabContainerScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    initialTab: Int = 0,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 4), pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    // Back handling: If on another tab, back smoothly transitions to Home (tab 0).
    // If on Home (tab 0), double back exits app.
    BackHandler {
        if (pagerState.currentPage != 0) {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedTime = now
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Direct tab navigation handler for inner quick actions and buttons
    val handleTabNavigation: (String) -> Unit = { route ->
        when (route) {
            Screen.Dashboard.route -> scope.launch { pagerState.animateScrollToPage(0) }
            Screen.Jobs.route -> scope.launch { pagerState.animateScrollToPage(1) }
            Screen.Customers.route -> scope.launch { pagerState.animateScrollToPage(2) }
            Screen.Reports.route -> scope.launch { pagerState.animateScrollToPage(3) }
            Screen.Settings.route -> scope.launch { pagerState.animateScrollToPage(4) }
            else -> onNavigate(route)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Work, contentDescription = "Jobs") },
                    label = { Text("Jobs") },
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text("Customers") },
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    selected = pagerState.currentPage == 3,
                    onClick = { scope.launch { pagerState.animateScrollToPage(3) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = pagerState.currentPage == 4,
                    onClick = { scope.launch { pagerState.animateScrollToPage(4) } }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    currentUserId = currentUserId,
                    repository = repository,
                    showBottomBar = false,
                    onNavigate = handleTabNavigation
                )
                1 -> JobListScreen(
                    currentUserId = currentUserId,
                    repository = repository,
                    showBottomBar = false,
                    onNavigate = handleTabNavigation,
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                2 -> CustomerListScreen(
                    currentUserId = currentUserId,
                    repository = repository,
                    showBottomBar = false,
                    onNavigate = handleTabNavigation,
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                3 -> ReportsScreen(
                    currentUserId = currentUserId,
                    repository = repository,
                    showBottomBar = false,
                    onNavigate = handleTabNavigation,
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                4 -> SettingsScreen(
                    currentUserId = currentUserId,
                    repository = repository,
                    showBottomBar = false,
                    onNavigate = handleTabNavigation,
                    onSignOut = onSignOut,
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
            }
        }
    }
}
