package com.example.app_mensagem

import android.app.Application
import com.cloudinary.android.MediaManager
import com.example.app_mensagem.data.local.AppDatabase

class MyApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "drtexe8rh",
            "api_key" to "935377312844235"
        )

        MediaManager.init(this, config)
    }
}