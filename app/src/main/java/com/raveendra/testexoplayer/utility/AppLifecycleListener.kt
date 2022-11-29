package com.raveendra.testexoplayer.utility

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.raveendra.testexoplayer.app.App

class AppLifecycleListener : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // app moved to foreground
        val intent = Intent("LOCATIONBROADRECEIVER")
        intent.putExtra("STATUS", "Foreground")
        App.instance?.applicationContext?.let {
            LocalBroadcastManager.getInstance(it)
                .sendBroadcast(intent)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        // app moved to background
        val intent = Intent("LOCATIONBROADRECEIVER")
        intent.putExtra("STATUS", "Background")
        App.instance?.let {
            LocalBroadcastManager.getInstance(it?.applicationContext)
                .sendBroadcast(intent)
        }
    }
}