package com.example.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPage(val title: String, val subtitle: String, val emoji: String)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage("Track Working Time", "Effortless live timer with break recording for tractor and farm work or hourly services.", "⏱️"),
        OnboardingPage("Automatic Billing", "Calculates wages instantly based on hours, minutes, custom rates, additional charges and discounts.", "💰"),
        OnboardingPage("Customers & Ledgers", "Maintain customer details, job history, pending balances and payment records in one place.", "👥"),
        OnboardingPage("Professional Invoices", "Generate, print, share and export professional PDF invoices and payment reminders via WhatsApp.", "📄")
    )

    var currentPage by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pages[currentPage].emoji,
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = pages[currentPage].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pages[currentPage].subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.forEachIndexed { index, _ ->
                        Surface(
                            modifier = Modifier.size(if (index == currentPage) 24.dp else 8.dp, 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(
                        text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
