package com.raveendra.testexoplayer.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import com.raveendra.testexoplayer.R
import com.raveendra.testexoplayer.utility.Constants
import com.raveendra.testexoplayer.utility.Constants.DISTANCE_IN_METER
import com.raveendra.testexoplayer.utility.LocationPerDistance
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), LocationPerDistance.ILocationCallback {

    private val playerView: PlayerView by lazy {
        findViewById<PlayerView>(R.id.playerView)
    }

    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var player: SimpleExoPlayer
    private var isLauncher = false
    private var locationPerDistance: LocationPerDistance? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pauseButton = playerView.findViewById(R.id.exo_pause)
        playButton = playerView.findViewById(R.id.exo_play)
        aspectRatioContainer.setAspectRatio(16f / 9)
        requestLocationPermissions()
        initialize()
    }

    private fun initialize() {
        initializePlayer()
        delayLaunch()
        initializeLocation()
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
            intent.getStringExtra("STATUS")?.let { videoResetReplay() }
        }
    }

    private fun locationBroadReceiverManager() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(locationBroadReceiver, IntentFilter("LOCATIONBROADRECEIVER"))
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
        locationPerDistance?.removeLocationUpdates()
    }

    override fun onLocationChanged(location: LatLng?) {
        videoResetReplay()
    }

    private fun videoResetReplay() {
        player.seekTo(0)
        player.setPlayWhenReady(true)
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
    }

}
