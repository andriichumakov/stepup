package com.example.stepupapp.presentation.explore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.stepupapp.R
import com.example.stepupapp.api.OpenTripMapResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URL
import kotlin.math.roundToInt

/**
 * Controller for managing map functionality in the Explore feature
 * Handles OSM map setup, markers, routing, and map interactions
 */
class MapController(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    interface MapControllerListener {
        fun onRouteCalculated(steps: Int, placeName: String)
        fun onRouteCleared()
        fun onMapError(message: String)
        fun showToast(message: String)
    }
    
    private var listener: MapControllerListener? = null
    private lateinit var mapView: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var currentRouteOverlay: Polyline? = null
    private val TAG = "MapController"
    
    private val STEP_LENGTH = 0.50 // Average step length in meters
    
    // Current location
    private var currentLatitude = 52.788040
    private var currentLongitude = 6.893176
    
    fun setListener(listener: MapControllerListener) {
        this.listener = listener
    }
    
    fun initializeMap(mapView: MapView) {
        this.mapView = mapView
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        
        // Set tile source to OpenStreetMap
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        
        // Enable zoom controls
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        
        // Set initial zoom level
        mapView.controller.setZoom(15.0)
        
        setupMyLocationOverlay()
    }
    
    fun initializeMapForLocationDetails(mapView: MapView) {
        this.mapView = mapView
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        
        // Set tile source to OpenStreetMap
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        
        // Enable zoom controls
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        
        // Set initial zoom level
        mapView.controller.setZoom(15.0)
        
        setupMyLocationOverlayWithoutFollow()
    }
    
    fun updateUserLocation(latitude: Double, longitude: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
        
        val userLocation = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(userLocation)
        
        // Update my location overlay
        myLocationOverlay?.let { overlay ->
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
        }
    }
    
    fun setUserLocationWithoutFollow(latitude: Double, longitude: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
        
        // Update my location overlay but don't enable follow location or center map
        myLocationOverlay?.let { overlay ->
            overlay.enableMyLocation()
            // Don't enable follow location to prevent auto-centering
        }
    }
    
    fun updateMapMarkers(places: List<OpenTripMapResponse>) {
        // Clear existing markers (except my location)
        mapView.overlays.clear()
        
        // Re-add my location overlay
        myLocationOverlay?.let { overlay ->
            mapView.overlays.add(overlay)
        }
        
        // Add markers for places
        places.forEach { place ->
            addPlaceMarker(place)
        }
        
        // Refresh the map
        mapView.invalidate()
    }
    
    fun centerOnUser() {
        val userLocation = GeoPoint(currentLatitude, currentLongitude)
        mapView.controller.animateTo(userLocation)
        mapView.controller.setZoom(16.0)
        listener?.showToast("Centered on your location")
    }
    
    fun centerOnLocation(latitude: Double, longitude: Double, locationName: String) {
        val location = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(location)
        mapView.controller.setZoom(16.0)
        
        // Add a marker for this specific location
        val marker = Marker(mapView)
        marker.position = location
        marker.title = locationName
        marker.snippet = "Tap for more details"
        marker.icon = createCustomMarker(android.R.color.holo_red_light)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        // Disable click listener for location details view (no route drawing)
        marker.setOnMarkerClickListener { _, _ -> false }
        
        mapView.overlays.add(marker)
        mapView.invalidate()
    }
    
    fun clearRoute() {
        currentRouteOverlay?.let { route ->
            mapView.overlays.remove(route)
            currentRouteOverlay = null
            mapView.invalidate()
        }
        listener?.onRouteCleared()
    }
    
    fun onResume() {
        if (::mapView.isInitialized) {
            mapView.onResume()
        }
    }
    
    fun onPause() {
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
    }
    
    private fun setupMyLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        myLocationOverlay?.let { overlay ->
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
            
            // Set custom user location icon
            overlay.setPersonIcon(createUserLocationBitmap())
            overlay.setDirectionIcon(createUserLocationBitmap())
            overlay.isDrawAccuracyEnabled = true
        }
        
        mapView.overlays.add(myLocationOverlay)
    }
    
    private fun setupMyLocationOverlayWithoutFollow() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        myLocationOverlay?.let { overlay ->
            // Only enable my location display but not location updates or following
            overlay.enableMyLocation()
            overlay.disableFollowLocation()
            
            // Set custom user location icon
            overlay.setPersonIcon(createUserLocationBitmap())
            overlay.setDirectionIcon(createUserLocationBitmap())
            overlay.isDrawAccuracyEnabled = true
        }
        
        mapView.overlays.add(myLocationOverlay)
    }
    
    fun disableLocationFollow() {
        myLocationOverlay?.disableFollowLocation()
    }
    
    private fun addPlaceMarker(place: OpenTripMapResponse) {
        val marker = Marker(mapView)
        marker.position = GeoPoint(place.point.lat, place.point.lon)
        marker.title = place.name.ifEmpty { "Unnamed Place" }
        marker.snippet = "${place.kinds.split(",").firstOrNull()?.replace("_", " ") ?: "Unknown"} • ${(place.dist/STEP_LENGTH).roundToInt()} steps away"
        
        // Set custom marker icon based on category
        marker.icon = getMarkerDrawableForCategory(place.kinds)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        // Add click listener for route drawing
        marker.setOnMarkerClickListener { _, _ ->
            drawRouteToPlace(place)
            true
        }
        
        mapView.overlays.add(marker)
    }
    
    private fun drawRouteToPlace(place: OpenTripMapResponse) {
        // Remove existing route
        clearRoute()
        
        listener?.showToast("Calculating route...")
        
        lifecycleScope.launch {
            try {
                val routePoints = getWalkingRoute(currentLatitude, currentLongitude, place.point.lat, place.point.lon)
                
                if (routePoints.isNotEmpty()) {
                    // Create polyline for the route
                    val routeLine = Polyline().apply {
                        setPoints(routePoints)
                        color = context.getColor(android.R.color.holo_blue_bright)
                        width = 10f
                        isGeodesic = false
                    }
                    
                    // Add route to map
                    currentRouteOverlay = routeLine
                    mapView.overlays.add(routeLine)
                    
                    // Zoom to show the route
                    val boundingBox = BoundingBox.fromGeoPoints(routePoints)
                    mapView.post {
                        mapView.zoomToBoundingBox(boundingBox, true, 100)
                    }
                    
                    // Calculate walking distance and steps
                    val routeDistance = calculateRouteDistance(routePoints)
                    val walkingSteps = (routeDistance / STEP_LENGTH).roundToInt()
                    
                    val placeName = place.name.ifEmpty { "Selected place" }
                    listener?.onRouteCalculated(walkingSteps, placeName)
                    
                } else {
                    // Fallback to straight line
                    drawStraightLineRoute(place)
                }
                
                mapView.invalidate()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting route: ${e.message}")
                drawStraightLineRoute(place)
            }
        }
    }
    
    private suspend fun getWalkingRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): List<GeoPoint> {
        return withContext(Dispatchers.IO) {
            try {
                // Using OSRM (OpenStreetMap Routing Machine)
                val url = "https://router.project-osrm.org/route/v1/foot/" +
                        "$startLon,$startLat;$endLon,$endLat?" +
                        "overview=full&geometries=geojson"
                
                val response = URL(url).readText()
                val json = JSONObject(response)
                
                if (json.getString("code") != "Ok") {
                    Log.w(TAG, "OSRM routing failed: ${json.optString("message", "Unknown error")}")
                    return@withContext emptyList()
                }
                
                val coordinates = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")
                
                val routePoints = mutableListOf<GeoPoint>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    routePoints.add(GeoPoint(lat, lon))
                }
                
                Log.d(TAG, "OSRM route found with ${routePoints.size} points")
                routePoints
                
            } catch (e: Exception) {
                Log.e(TAG, "OSRM routing failed: ${e.message}")
                emptyList()
            }
        }
    }
    
    private fun drawStraightLineRoute(place: OpenTripMapResponse) {
        val userLocation = GeoPoint(currentLatitude, currentLongitude)
        val placeLocation = GeoPoint(place.point.lat, place.point.lon)
        val routePoints = listOf(userLocation, placeLocation)
        
        val routeLine = Polyline().apply {
            setPoints(routePoints)
            color = context.getColor(android.R.color.holo_orange_dark)
            width = 8f
            isGeodesic = true
        }
        
        currentRouteOverlay = routeLine
        mapView.overlays.add(routeLine)
        
        val boundingBox = BoundingBox.fromGeoPoints(routePoints)
        mapView.post {
            mapView.zoomToBoundingBox(boundingBox, true, 100)
        }
        
        val placeName = place.name.ifEmpty { "Selected place" }
        val distance = (place.dist / STEP_LENGTH).roundToInt()
        listener?.onRouteCalculated(distance, placeName)
        
        mapView.invalidate()
    }
    
    private fun calculateRouteDistance(routePoints: List<GeoPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            val point1 = routePoints[i]
            val point2 = routePoints[i + 1]
            totalDistance += point1.distanceToAsDouble(point2)
        }
        return totalDistance
    }
    
    private fun createUserLocationBitmap(): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White background circle
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, backgroundPaint)
        
        // Blue border
        val borderPaint = Paint().apply {
            color = context.getColor(android.R.color.holo_blue_bright)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, borderPaint)
        
        try {
            // Load StepUp logo
            val logoDrawable = context.getDrawable(R.drawable.stepup_logo_bunny_small)
            logoDrawable?.let { drawable ->
                val logoSize = (size * 0.7f).toInt()
                val logoLeft = (size - logoSize) / 2
                val logoTop = (size - logoSize) / 2
                
                canvas.save()
                canvas.rotate(90f, size / 2f, size / 2f)
                drawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                drawable.draw(canvas)
                canvas.restore()
            }
        } catch (e: Exception) {
            // Fallback: blue circle
            val fallbackPaint = Paint().apply {
                color = context.getColor(android.R.color.holo_blue_bright)
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, 20f, fallbackPaint)
        }
        
        return bitmap
    }
    
    private fun getMarkerDrawableForCategory(kinds: String): Drawable? {
        val kindsLower = kinds.lowercase()
        val colorRes = when {
            kindsLower.contains("food") || kindsLower.contains("restaurant") -> android.R.color.holo_orange_dark
            kindsLower.contains("cultural") || kindsLower.contains("museum") -> android.R.color.holo_purple
            kindsLower.contains("natural") || kindsLower.contains("park") -> android.R.color.holo_green_dark
            kindsLower.contains("shop") || kindsLower.contains("mall") -> android.R.color.holo_orange_light
            kindsLower.contains("historic") -> android.R.color.holo_red_dark
            kindsLower.contains("sport") -> android.R.color.holo_blue_dark
            else -> android.R.color.holo_red_light
        }
        
        return createCustomMarker(colorRes)
    }
    
    private fun createCustomMarker(colorRes: Int): Drawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White outer circle
        val outerPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, outerPaint)
        
        // Colored inner circle
        val innerPaint = Paint().apply {
            color = context.getColor(colorRes)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, innerPaint)
        
        // White center dot
        val centerPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 8f, centerPaint)
        
        return BitmapDrawable(context.resources, bitmap)
    }
} 