package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val primaryColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    var startAnimation by remember { mutableStateOf(false) }
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.5f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500L)
        onSplashFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.15f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "App Logo",
                            tint = primaryColor,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Time Bill",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Professional Time Tracking & Billing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
