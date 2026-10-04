package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*
import java.util.Locale

class TimerNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    companion object {
        const val CHANNEL_ID = "time_bill_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                TimerStateManager.pauseTimer(this)
            }
            ACTION_RESUME -> {
                TimerStateManager.resumeTimer(this)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE, null -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startPeriodicUpdates()
            }
        }
        return START_STICKY
    }

    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                if (!TimerStateManager.timerData.value.isRunning) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val notification = buildNotification()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
                delay(1000L)
            }
        }
    }

    private fun buildNotification(): Notification {
        val data = TimerStateManager.timerData.value
        val elapsed = TimerStateManager.elapsedSeconds.value
        val hours = elapsed / 3600
        val minutes = (elapsed % 3600) / 60
        val seconds = elapsed % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        val billableHours = elapsed / 3600.0
        val currentAmount = (billableHours * data.hourlyRate).toInt()

        val custName = data.customer?.name ?: "Customer"
        val serviceName = data.service?.name ?: "Work"
        val statusPrefix = if (data.isPaused) "[Paused] " else ""

        // Open app straight to Timer screen
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "timer")
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Pause/Resume PendingIntent
        val pauseResumeIntent = Intent(this, TimerNotificationService::class.java).apply {
            action = if (data.isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val actionIcon = if (data.isPaused) R.drawable.ic_play else R.drawable.ic_pause
        val actionText = if (data.isPaused) "Resume" else "Pause"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$statusPrefix$formattedTime - $custName")
            .setContentText("$serviceName • Rate: ₹${data.hourlyRate.toInt()}/hr • Amount: ₹$currentAmount")
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                actionIcon,
                actionText,
                pauseResumePendingIntent
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Work Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live running job timer and amount"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
