package com.raveendra.testexoplayer.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import java.util.*


class MainActivity : AppCompatActivity() {

    private val playerView: PlayerView by lazy {
        findViewById<PlayerView>(R.id.playerView)
    }

    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var player: SimpleExoPlayer
    private lateinit var previewSeekbar: PreviewFrameSeekBar
    private var isLauncher = false

    // Declaring sensorManager and acceleration constants
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeView()
        aspectRatioContainer.setAspectRatio(16f / 9)
        initializePlayer()
        initializePreviewSeekbar()
        delayLaunch()
        initializeSensor()
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
        registerSensor()
        super.onResume()
    }

    override fun onPause() {
        unregisterSensor()
        super.onPause()
        isLauncher = true
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
    private fun initializeSensor() {
        // Getting the Sensor Manager instance
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        Objects.requireNonNull(sensorManager)!!
            .registerListener(
                sensorListener, sensorManager!!
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
            )

        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }


    //register Sensor Event Listner
    private fun registerSensor() {
        sensorListener.let {
            sensorManager?.registerListener(
                it, sensorManager?.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER
                ), SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    //unregister Sensor Event Listner
    private fun unregisterSensor() {
        sensorListener.let {
            sensorManager?.unregisterListener(it)
        }
    }


    // Sensor Event Listener
    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            // Fetching x,y,z values
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            // Getting current accelerations
            // with the help of fetched x,y,z values
            currentAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            // Display a Toast message if
            // acceleration value is over 12
            if (acceleration > 50) {
                player.playWhenReady = false
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }


}
