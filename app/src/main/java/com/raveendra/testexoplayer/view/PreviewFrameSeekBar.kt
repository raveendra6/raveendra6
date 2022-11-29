package com.raveendra.testexoplayer.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.exoplayer2.SimpleExoPlayer
import com.raveendra.testexoplayer.R
import com.raveendra.testexoplayer.utility.TimeUtils
import com.raveendra.testexoplayer.utility.setOnSeekBarChangeListener
import kotlinx.android.synthetic.main.preview_seekbar.view.*

class PreviewFrameSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mCanUpdate: Boolean = true
    private var mLastTotalValue: Long = 0
    private var mDuration: Long = 0
    private var mMaxProgress = 0
    private var mPreviewLoader: PreviewLoader? = null
    private var mExoplayer: SimpleExoPlayer? = null
    private var view : View = LayoutInflater.from(context).inflate(R.layout.preview_seekbar, this, true)
//    private val mBinding = PreviewSeekbarBinding.inflate(
//        LayoutInflater.from(context),
//        this,
//        true
//    )

    init {
        view.seekBar.setOnSeekBarChangeListener(
            onStartTrackingTouch = {
                mCanUpdate = false
                view.seekGroup.isVisible = true
                view.progressGroup.isInvisible = true
            },
            onStopTrackingTouch = {
                mCanUpdate = true
                view.seekGroup.isVisible = false
                view.progressGroup.isInvisible = false
            },
            onProgressChanged = { _, progress, isFromUser ->
                if (isFromUser) {
                    val seekProgress = progress * mDuration / mMaxProgress
                    view.seekProgress.text = TimeUtils.msToTime(seekProgress)
                    setSeekGroupPosition(progress)
                    setVideoFrameFor(seekProgress)
                    mExoplayer?.let {
                        it.seekTo((progress).toLong())
                        it.playWhenReady = true
                    }
                }
            }
        )
    }

    fun setPreviewLoader(previewLoader: PreviewLoader) {
        mPreviewLoader = previewLoader
    }

    fun setSeekbarUpdate(exoPlayer: SimpleExoPlayer) {
        mExoplayer = exoPlayer
        mMaxProgress = (exoPlayer.duration).toInt()
        val progressMs = exoPlayer.currentPosition
        val durationMs = exoPlayer.duration
        if (durationMs < 0) return

        if (mDuration == 0L) {
            mPreviewLoader ?: throw RuntimeException("Need to set previewLoader")
            mPreviewLoader?.preload(durationMs)
        }

        mDuration = durationMs
        if (!mCanUpdate) return
        mLastTotalValue = durationMs
        view.progress.text = TimeUtils.msToTime(progressMs)
        view.total.text = TimeUtils.msToTime(durationMs)
        view.seekBar.progress = progressMs.toInt()
        view.seekBar.max = mDuration.toInt()
    }

    private fun setSeekGroupPosition(progress: Int) {
        val progressWidth = view.progress.width
        val totalTimeWidth = view.total.width
        val seekBarWidth = view.seekBar.width
        val frameWidth = view.image.width
        val margin = resources.getDimensionPixelSize(R.dimen.margin)
        val seekbarPadding = resources.getDimensionPixelSize(R.dimen.seekbar_padding)

        val minSeekFrameValue =
            ((frameWidth.toFloat() / 2) - progressWidth - margin - seekbarPadding) /
                    seekBarWidth * mMaxProgress
        val maxSeekFrameValue =
            (1 - ((frameWidth.toFloat() / 2) - totalTimeWidth - margin - seekbarPadding) /
                    seekBarWidth) * mMaxProgress

        val params = view.image.layoutParams as ConstraintLayout.LayoutParams

        val seekProgress =
            progress.coerceIn(minSeekFrameValue.toInt() + 1, maxSeekFrameValue.toInt())

        val bias =
            (seekProgress.toFloat() - minSeekFrameValue) / (maxSeekFrameValue - minSeekFrameValue)

        params.horizontalBias = bias
        view.image.layoutParams = params
    }

    private fun setVideoFrameFor(seekProgress: Long) {
        mPreviewLoader?.load(seekProgress, view.image)
    }

}