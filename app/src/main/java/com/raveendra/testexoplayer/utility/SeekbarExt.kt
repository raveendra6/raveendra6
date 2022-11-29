package com.raveendra.testexoplayer.utility

import android.widget.SeekBar

fun SeekBar.setOnSeekBarChangeListener(
    onStartTrackingTouch: (seekbar: SeekBar?) -> Unit = {},
    onStopTrackingTouch: (seekbar: SeekBar?) -> Unit = {},
    onProgressChanged: (seekbar: SeekBar?, progress: Int, isFromUser: Boolean) -> Unit = { _, _, _ -> }
) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekbar: SeekBar?, progress: Int, isFromUser: Boolean) {
            onProgressChanged(seekbar, progress, isFromUser)
        }

        override fun onStartTrackingTouch(seekbar: SeekBar?) {
            onStartTrackingTouch(seekbar)
        }

        override fun onStopTrackingTouch(seekbar: SeekBar?) {
            onStopTrackingTouch(seekbar)
        }

    })
}