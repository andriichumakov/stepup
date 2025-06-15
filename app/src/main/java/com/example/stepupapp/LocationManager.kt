package com.example.stepupapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.util.*

/**
 * LocationManager handles all location-related operations including
 * permissions, location updates, and geocoding.
 */
class LocationManager(
    private val activity: AppCompatActivity,
    private val onLocationUpdate: (Location) -> Unit
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isLocationUpdatesActive = false
    
    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1001
    }

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        createLocationRequest()
        createLocationCallback()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    onLocationUpdate(location)
                }
            }
        }
    }

    fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(activity)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permissions to show places near you.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isLocationUpdatesActive = true
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    fun isUpdatesActive(): Boolean = isLocationUpdatesActive

    fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(activity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val cityName = address.locality ?: address.subAdminArea ?: "Unknown"
                val countryName = address.countryName ?: ""
                if (countryName.isNotEmpty()) "$cityName, $countryName" else cityName
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            Log.e("LocationManager", "Error getting location name: ${e.message}")
            "Unknown Location"
        }
    }
} 