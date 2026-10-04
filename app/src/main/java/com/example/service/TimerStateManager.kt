package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Customer
import com.example.data.model.ServiceItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveTimerData(
    val customer: Customer? = null,
    val service: ServiceItem? = null,
    val hourlyRate: Double = 500.0,
    val notes: String = "",
    val startTimeMs: Long = 0L,
    val initialStartMs: Long = 0L,
    val accumulatedSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

object TimerStateManager {
    private val _timerData = MutableStateFlow(ActiveTimerData())
    val timerData: StateFlow<ActiveTimerData> = _timerData.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTimer(
        context: Context,
        customer: Customer,
        service: ServiceItem?,
        rate: Double,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        _timerData.value = ActiveTimerData(
            customer = customer,
            service = service,
            hourlyRate = rate,
            notes = notes,
            startTimeMs = now,
            initialStartMs = now,
            accumulatedSeconds = 0L,
            isRunning = true,
            isPaused = false
        )
        _elapsedSeconds.value = 0L

        startTicker()
        startNotificationService(context)
        saveToPrefs(context)
    }

    fun pauseTimer(context: Context) {
        val current = _timerData.value
        if (!current.isRunning || current.isPaused) return

        val currentElapsed = _elapsedSeconds.value
        _timerData.value = current.copy(
            isPaused = true,
            accumulatedSeconds = currentElapsed
        )
        stopTicker()
        updateNotificationService(context)
        saveToPrefs(context)
    }

    fun resumeTimer(context: Context) {
        val current = _timerData.value
        if (!current.isRunning || !current.isPaused) return

        val now = System.currentTimeMillis()
        _timerData.value = current.copy(
            isPaused = false,
            startTimeMs = now
        )
        startTicker()
        updateNotificationService(context)
        saveToPrefs(context)
    }

    fun stopTimer(context: Context): ActiveTimerData {
        val finalData = _timerData.value
        stopTicker()
        _timerData.value = ActiveTimerData()
        _elapsedSeconds.value = 0L
        stopNotificationService(context)
        clearPrefs(context)
        return finalData
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timerData.value.isRunning && !_timerData.value.isPaused) {
                val current = _timerData.value
                val diffSeconds = (System.currentTimeMillis() - current.startTimeMs) / 1000L
                _elapsedSeconds.value = maxOf(0L, current.accumulatedSeconds + maxOf(0L, diffSeconds))
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    fun startNotificationService(context: Context) {
        try {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                action = TimerNotificationService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNotificationService(context: Context) {
        try {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                action = TimerNotificationService.ACTION_UPDATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopNotificationService(context: Context) {
        try {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                action = TimerNotificationService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToPrefs(context: Context) {
        val sp = context.getSharedPreferences("active_timer_prefs", Context.MODE_PRIVATE)
        val data = _timerData.value
        sp.edit()
            .putBoolean("is_running", data.isRunning)
            .putBoolean("is_paused", data.isPaused)
            .putString("customer_id", data.customer?.customerId ?: "")
            .putString("customer_name", data.customer?.name ?: "")
            .putString("customer_mobile", data.customer?.mobile ?: "")
            .putString("customer_village", data.customer?.village ?: "")
            .putString("service_id", data.service?.serviceId ?: "")
            .putString("service_name", data.service?.name ?: "")
            .putFloat("hourly_rate", data.hourlyRate.toFloat())
            .putString("notes", data.notes)
            .putLong("start_time_ms", data.startTimeMs)
            .putLong("initial_start_ms", data.initialStartMs)
            .putLong("accumulated_seconds", data.accumulatedSeconds)
            .apply()
    }

    private fun clearPrefs(context: Context) {
        val sp = context.getSharedPreferences("active_timer_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    fun restoreFromPrefsIfNeeded(context: Context) {
        if (_timerData.value.isRunning) return
        val sp = context.getSharedPreferences("active_timer_prefs", Context.MODE_PRIVATE)
        val isRunning = sp.getBoolean("is_running", false)
        if (!isRunning) return

        val isPaused = sp.getBoolean("is_paused", false)
        val custId = sp.getString("customer_id", "") ?: ""
        val custName = sp.getString("customer_name", "") ?: ""
        val custMobile = sp.getString("customer_mobile", "") ?: ""
        val custVillage = sp.getString("customer_village", "") ?: ""
        val servId = sp.getString("service_id", "") ?: ""
        val servName = sp.getString("service_name", "") ?: ""
        val rate = sp.getFloat("hourly_rate", 500f).toDouble()
        val notes = sp.getString("notes", "") ?: ""
        val startMs = sp.getLong("start_time_ms", 0L)
        val initStartMs = sp.getLong("initial_start_ms", startMs)
        val accumulated = sp.getLong("accumulated_seconds", 0L)

        val customer = if (custId.isNotEmpty() || custName.isNotEmpty()) {
            Customer(customerId = custId, name = custName, mobile = custMobile, village = custVillage)
        } else null

        val service = if (servId.isNotEmpty() || servName.isNotEmpty()) {
            ServiceItem(serviceId = servId, name = servName, hourlyRate = rate)
        } else null

        _timerData.value = ActiveTimerData(
            customer = customer,
            service = service,
            hourlyRate = rate,
            notes = notes,
            startTimeMs = startMs,
            initialStartMs = initStartMs,
            accumulatedSeconds = accumulated,
            isRunning = true,
            isPaused = isPaused
        )

        if (!isPaused) {
            val diffSeconds = (System.currentTimeMillis() - startMs) / 1000L
            _elapsedSeconds.value = maxOf(0L, accumulated + maxOf(0L, diffSeconds))
            startTicker()
            startNotificationService(context)
        } else {
            _elapsedSeconds.value = accumulated
            updateNotificationService(context)
        }
    }
}
