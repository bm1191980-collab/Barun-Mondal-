package com.example

import android.app.Application
import android.util.Log
import com.example.data.service.SatisfyFirebaseMessagingService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class SatisfyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        SatisfyFirebaseMessagingService.initializeChannels(this)
    }

    private fun initializeFirebase() {
        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:885696947638:android:satisfyvplay")
                    .setApiKey("AIzaSySatisfyDefaultKeyMockProdMode99")
                    .setProjectId("satisfy-vplay")
                    .setStorageBucket("satisfy-vplay.appspot.com")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("SatisfyApplication", "FirebaseApp successfully initialized with options")
            } else {
                Log.d("SatisfyApplication", "FirebaseApp already initialized (${apps.size} apps)")
            }
        } catch (e: Exception) {
            Log.w("SatisfyApplication", "FirebaseApp initialization fallback: ${e.message}")
        }
    }
}
