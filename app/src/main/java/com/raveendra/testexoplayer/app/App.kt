package com.raveendra.testexoplayer.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.raveendra.testexoplayer.utility.AppLifecycleListener


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())
    }

    companion object {
        @get:Synchronized
        var instance: App? = null
            private set
    }
}