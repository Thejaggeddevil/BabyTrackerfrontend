package com.example.babyparenting

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BabyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: FirebaseApp.initializeApp(this)
    }
}