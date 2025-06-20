package com.example.stepupapp.managers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.stepupapp.MemoryActivity
import com.example.stepupapp.Place
import com.example.stepupapp.R
import com.google.android.material.snackbar.Snackbar
import android.view.View

class AppNotificationManager(private val context: Context) {
    
    companion object {
        private const val MEMORY_CHANNEL_ID = "memory_channel"
        private const val MEMORY_NOTIFICATION_ID = 1001
    }
    
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Memory Notifications"
            val descriptionText = "Notifications for newly added memories"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(MEMORY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                context.getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun sendMemoryNotification(place: Place) {
        if (!hasNotificationPermission()) {
            return
        }
        
        try {
            val intent = createMemoryIntent(place)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, MEMORY_CHANNEL_ID)
                .setSmallIcon(R.drawable.stepup_logo_bunny)
                .setContentTitle("New Memory Added")
                .setContentText("You added ${place.name} on ${place.date_saved}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            if (hasNotificationPermission()) {
                notificationManager.notify(MEMORY_NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
            android.util.Log.w("AppNotificationManager", "Notification permission denied: ${e.message}")
        }
    }
    
    fun showInAppSnackbar(rootView: View, place: Place, onViewClicked: (Place) -> Unit) {
        val snackbar = Snackbar.make(
            rootView,
            "New memory added: ${place.name} (${place.date_saved})",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("View") {
            onViewClicked(place)
        }
        snackbar.show()
    }
    
    private fun createMemoryIntent(place: Place): Intent {
        return Intent(context, MemoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("highlightMemoryId", place.id)
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications don't require explicit permission on older Android versions
        }
    }
} 