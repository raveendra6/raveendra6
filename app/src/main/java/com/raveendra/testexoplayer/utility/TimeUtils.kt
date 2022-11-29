package com.raveendra.testexoplayer.utility

import java.util.concurrent.TimeUnit

object TimeUtils {

    fun msToTime(ms: Long): String {
        val hour = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms - TimeUnit.HOURS.toMillis(hour))
        val second = TimeUnit.MILLISECONDS
            .toSeconds(ms - TimeUnit.HOURS.toMillis(hour) - TimeUnit.MINUTES.toMillis(minutes))
        return when (hour) {
            0L -> String.format("%02d:%02d", minutes, second)
            else -> String.format("%02d:%02d:%02d", hour, minutes, second)
        }
    }

}