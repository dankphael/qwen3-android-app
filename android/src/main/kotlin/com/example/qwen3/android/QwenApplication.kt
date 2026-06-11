package com.example.qwen3.android

import android.app.Application
import com.example.qwen3.android.di.androidPlatformModule
import com.example.qwen3.core.di.coreModule
import com.example.qwen3.ui.di.appUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QwenApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@QwenApplication)
            modules(
                coreModule(),
                androidPlatformModule(),
                appUiModule(),
            )
        }
    }
}
