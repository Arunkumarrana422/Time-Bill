package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TimeBillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
        try {
            com.example.service.TimerStateManager.restoreFromPrefsIfNeeded(this)
        } catch (e: Exception) {
            Log.e("TimeBillApp", "Timer restore error: ${e.message}")
        }
    }

    private fun initFirebase() {
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:648141005997:android:31dcf5a9729b4979c65224")
                .setApiKey("AIzaSyAUe5cJqA1PDO6LLe0a4Hv1vdDjuM8WEuk")
                .setProjectId("time-bill-management")
                .setDatabaseUrl("https://time-bill-management-default-rtdb.firebaseio.com")
                .setStorageBucket("time-bill-management.firebasestorage.app")
                .build()

            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.e("TimeBillApp", "FirebaseApp initialization error: ${e.message}", e)
        }
    }
}
