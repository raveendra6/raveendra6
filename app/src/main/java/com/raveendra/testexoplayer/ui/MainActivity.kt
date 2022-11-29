package com.raveendra.testexoplayer.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.raveendra.testexoplayer.R
import com.raveendra.testexoplayer.utility.Constants
import com.raveendra.testexoplayer.view.PreviewFrameSeekBar
import com.raveendra.testexoplayer.view.PreviewLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


class MainActivity : AppCompatActivity() {

    private val playerView: PlayerView by lazy {
        findViewById<PlayerView>(R.id.playerView)
    }

    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var player: SimpleExoPlayer
    private lateinit var previewSeekbar: PreviewFrameSeekBar
    private var isLauncher = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeView()
        aspectRatioContainer.setAspectRatio(16f / 9)
        initializePlayer()
        initializePreviewSeekbar()
        delayLaunch()
    }

    private fun initializeView() {
        previewSeekbar = findViewById(R.id.previewFrameSeekBar)
        pauseButton = playerView.findViewById(R.id.exo_pause)
        playButton = playerView.findViewById(R.id.exo_play)
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this)
            .build()
            .apply {
                playWhenReady = true
            }
        playerView.player = player

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (player.isPlaying) {
                        if (!isLauncher) {
                            pauseVideo()
                            isLauncher = true
                            delayLaunch()
                        } else {
                            playButton.visibility = View.GONE
                            pauseButton.visibility = View.VISIBLE
                        }
                    } else {
                        playButton.visibility = View.VISIBLE
                        pauseButton.visibility = View.GONE
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                } else if (playbackState == Player.STATE_ENDED) {

                } else if (playbackState == Player.STATE_IDLE) {

                }
            }
        })

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "TestExoplayer")
        )
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource((Uri.parse(Constants.VIDEO_URL)))

        player.prepare(videoSource)
    }

    private fun delayLaunch() {
        Handler(Looper.getMainLooper()).postDelayed({
            playVideo()
        }, 3000)
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    override fun onRestart() {
        super.onRestart()
        playVideo()
    }

    private fun pauseVideo() {
        player.playWhenReady = false
    }

    private fun playVideo() {
        player.playWhenReady = true
    }

    override fun onDestroy() {
        player.playWhenReady = false
        super.onDestroy()
    }

    //Preview Seekbar
    private fun initializePreviewSeekbar() {
        lifecycleScope.launchWhenStarted {
            val videoPreviewLoader = PreviewLoader(
                coroutineScope = this,
                context = this@MainActivity,
                url = Constants.VIDEO_URL
            )

            previewSeekbar.setPreviewLoader(videoPreviewLoader)

            while (isActive) {
                previewSeekbar.setSeekbarUpdate(player)
                delay(1000)
            }
        }
    }

}
