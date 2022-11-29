package com.raveendra.testexoplayer.app

import android.app.Application


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @get:Synchronized
        var instance: App? = null
            private set
    }
}