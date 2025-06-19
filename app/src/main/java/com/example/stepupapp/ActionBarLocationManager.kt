package com.example.stepupapp

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Utility class to manage updating the actionbar location TextView
 * with the current location across different activities.
 */
class ActionBarLocationManager(private val activity: AppCompatActivity) {
    
    private var actionBarLocationTextView: TextView? = null
    private var locationManager: LocationManager? = null
    
    fun setupActionBarLocation() {
        // Find the actionbar location TextView
        actionBarLocationTextView = activity.findViewById(R.id.actionbar_location)
        
        // Initialize location manager if not already done
        if (locationManager == null) {
            locationManager = LocationManager(activity) { location ->
                updateActionBarLocation(location.latitude, location.longitude)
            }
        }
        
        // Start getting location updates
        if (locationManager?.checkLocationPermission() == true) {
            actionBarLocationTextView?.text = "Getting location..."
            locationManager?.startLocationUpdates()
        } else {
            // Keep default text if no permission
            actionBarLocationTextView?.text = "Location unavailable"
        }
    }
    
    // Alternative method to update location from existing location data
    fun updateLocationFromCoordinates(latitude: Double, longitude: Double) {
        if (locationManager == null) {
            locationManager = LocationManager(activity) { }
        }
        updateActionBarLocation(latitude, longitude)
    }
    
    private fun updateActionBarLocation(latitude: Double, longitude: Double) {
        locationManager?.let { locManager ->
            val locationName = locManager.getLocationName(latitude, longitude)
            actionBarLocationTextView?.text = locationName
            
            // Stop location updates after getting the location
            locManager.stopLocationUpdates()
        }
    }
    
    fun requestLocationPermission() {
        locationManager?.requestLocationPermission()
    }
    
    fun onPermissionGranted() {
        // Called when location permission is granted to start location updates
        if (locationManager?.checkLocationPermission() == true) {
            actionBarLocationTextView?.text = "Getting location..."
            locationManager?.startLocationUpdates()
        }
    }
    
    fun onDestroy() {
        locationManager?.stopLocationUpdates()
    }
} 