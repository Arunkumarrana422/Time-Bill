package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TimeBillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:648141005997:android:31dcf5a9729b4979c65224")
                        .setApiKey("AIzaSyAUe5cJqA1PDO6LLe0a4Hv1vdDjuM8WEuk")
                        .setProjectId("time-bill-management")
                        .setDatabaseUrl("https://time-bill-management-default-rtdb.firebaseio.com")
                        .setStorageBucket("time-bill-management.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            Log.e("TimeBillApp", "FirebaseApp initialization error: ${e.message}", e)
        }
    }
}
