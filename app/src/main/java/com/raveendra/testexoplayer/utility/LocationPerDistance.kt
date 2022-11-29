package com.raveendra.testexoplayer.utility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class LocationPerDistance(
    private val context: Context,
    private val callback: ILocationCallback,
    distanceInMeters: Int
) {
    private var mLocationCallback: LocationCallback? = null
    private var mLocationRequest: LocationRequest? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    init {
        setupFusedLocationClient()
        setupLocationRequest(distanceInMeters)
    }

    private fun setupFusedLocationClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                val latLng = lr.lastLocation?.longitude?.let { longtitude ->
                    lr.lastLocation?.latitude?.let { lantitude ->
                        LatLng(
                            lantitude,
                            longtitude
                        )
                    }
                }
                callback.onLocationChanged(latLng)
            }
        }
    }

    private fun setupLocationRequest(distanceInMeters: Int) {
        //Log.e("-->", "setupLocationRequest");
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval =
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mLocationRequest!!.smallestDisplacement = distanceInMeters.toFloat()
    }

    fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mLocationRequest?.let {
            mLocationCallback?.let { it1 ->
                mFusedLocationClient!!.requestLocationUpdates(
                    it,
                    it1,
                    Looper.myLooper()
                )
            }
        }
    }

    fun removeLocationUpdates() {
        mLocationCallback?.let { mFusedLocationClient!!.removeLocationUpdates(it) }
    }

    //______________________________________________________________________________________________
    interface ILocationCallback {
        fun onLocationChanged(location: LatLng?)
    }

    companion object {
        //The desired interval for location updates. Inexact. Updates may be more or less frequent.
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000 //30 seconds

        //The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2 //5 seconds
    }
}