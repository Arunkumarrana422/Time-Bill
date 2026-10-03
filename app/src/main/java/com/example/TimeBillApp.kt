package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class TimeBillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("TimeBillApp", "FirebaseApp initialization error: ${e.message}", e)
        }
    }
}
