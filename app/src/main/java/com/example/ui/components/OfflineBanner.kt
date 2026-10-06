package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    triggerKey: Int = 0,
    modifier: Modifier = Modifier,
    onTurnOnWifi: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var isVisible by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(-220f) }

    // Every time isOffline changes or user taps any button/card while offline
    LaunchedEffect(isOffline, triggerKey) {
        if (!isOffline) {
            isVisible = false
            offsetY.snapTo(-220f)
        } else {
            isVisible = true
            // Play slide-in animation from top with spring bounce
            offsetY.snapTo(-220f)
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            // Auto hide after 4.5 seconds until next button/card click
            delay(4500)
            offsetY.animateTo(
                targetValue = -220f,
                animationSpec = tween(350, easing = FastOutLinearInEasing)
            )
            isVisible = false
        }
    }

    if (isVisible) {
        val cardBgColor = if (isDark) {
            Color(0xFF1E221E)
        } else {
            MaterialTheme.colorScheme.surface
        }

        val iconBgColor = if (isDark) {
            Color(0xFF991B1B)
        } else {
            Color(0xFFEF4444)
        }

        val buttonTextColor = if (isDark) {
            Color(0xFFF87171)
        } else {
            Color(0xFFDC2626)
        }

        val borderColor = if (isDark) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(9999f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    borderColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Red circular icon adapted for dark/light mode
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(iconBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Oops! You're offline.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Please check your internet connection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                if (onTurnOnWifi != null) {
                                    onTurnOnWifi()
                                } else {
                                    openWifiSettings(context)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TURN ON WIFI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = buttonTextColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun openWifiSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e3: Exception) {
                // ignore
            }
        }
    }
}
