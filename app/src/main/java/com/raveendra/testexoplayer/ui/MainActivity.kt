package com.raveendra.testexoplayer.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.raveendra.testexoplayer.R
import com.raveendra.testexoplayer.utility.Constants
import com.raveendra.testexoplayer.utility.Constants.DELAY_TIME
import com.raveendra.testexoplayer.utility.Constants.DISTANCE_IN_METER
import com.raveendra.testexoplayer.utility.LocationPerDistance
import com.raveendra.testexoplayer.view.PreviewFrameSeekBar
import com.raveendra.testexoplayer.view.PreviewLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*


class MainActivity : AppCompatActivity(), LocationPerDistance.ILocationCallback {

    private val playerView: PlayerView by lazy {
        findViewById<PlayerView>(R.id.playerView)
    }

    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var player: SimpleExoPlayer
    private lateinit var previewSeekbar: PreviewFrameSeekBar
    private var isLauncher = false
    private var locationPerDistance: LocationPerDistance? = null

    // Declaring sensorManager and acceleration constants
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var isPresetLocation = false
    private lateinit var presentLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestLocationPermissions()
        initialize()

    }

    private fun initialize() {
        initializeView()
        aspectRatioContainer.setAspectRatio(16f / 9)
        initializePlayer()
        initializePreviewSeekbar()
        initializeLocation()
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
                    if (player.currentPosition < 1000) {
                        isPresetLocation = false
                    }
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
        }, DELAY_TIME)
    }

    //Location

    private fun initializeLocation() {
        locationBroadReceiverManager()
        locationPerDistance = LocationPerDistance(
            this,
            this,
            DISTANCE_IN_METER
        )
        locationPerDistance?.requestLocationUpdates()
    }

    private val locationBroadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra("STATUS")?.let {
                videoResetReplay()
            }
        }
    }

    private fun locationBroadReceiverManager() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(locationBroadReceiver, IntentFilter("LOCATIONBROADRECEIVER"))
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
        locationPerDistance?.removeLocationUpdates()
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
                Toast.makeText(applicationContext, "Video Pause", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onLocationChanged(location: LatLng?) {
        if (isPresetLocation) {
            location?.let {
                if (SphericalUtil.computeDistanceBetween(presentLatLng, location) > 10) {
                    presentLatLng = location
                    videoResetReplay()
                }
            }
        } else {
            if (location != null) {
                presentLatLng = location
                isPresetLocation = true
            }
        }
    }

    private fun videoResetReplay() {
        if (isPresetLocation) {
            player.seekTo(0)
            isPresetLocation = false
            Toast.makeText(applicationContext, "Video Reset and Replay", Toast.LENGTH_SHORT).show()
            delayLaunch()
        }
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) === PackageManager.PERMISSION_GRANTED
                    ) {
                        locationPerDistance?.requestLocationUpdates()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
