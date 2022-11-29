package com.raveendra.testexoplayer.view

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.raveendra.testexoplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreviewLoader(
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val url: String
) {

    private var mLastInterval: Long? = null
    private var mDuration: Long? = null
    private val mFrameWidth = context.resources.getDimensionPixelSize(R.dimen.frame_width) / 3
    private val mFrameHeight = context.resources.getDimensionPixelSize(R.dimen.frame_height) / 3

    fun preload(durationMs: Long) {
        mDuration = durationMs

        coroutineScope.launch(Dispatchers.IO) {
            for (i in 0..durationMs * 1000) {
                val interval = getInterval(i, durationMs)
                showImage(interval)
            }
        }
    }

    fun load(timeUs: Long, imageView: ImageView) {
        mDuration?.let {
            val interval = getInterval(timeUs, it)
            showImage(interval, imageView)
        }
    }

    private fun showImage(
        interval: Long,
        imageView: ImageView? = null
    ) {
        if (interval == mLastInterval) return

        mLastInterval = interval

        val options: RequestOptions = RequestOptions().frame(interval * 1000)
            .override(mFrameWidth, mFrameHeight)

        Glide.with(context).asBitmap()
            .load(url)
            .placeholder(imageView?.drawable)
            .centerCrop()
            .apply(options)
            .apply {
                when (imageView) {
                    null -> preload()
                    else -> into(imageView)
                }
            }
    }

    private fun getInterval(timeMs: Long, durationMs: Long): Long {
        val coefficient = when {
            durationMs < 30000 -> 1
            else -> durationMs / 30
        }

        return timeMs - timeMs % coefficient
    }

}