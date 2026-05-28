package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "memocare_alarms_channel"
        private const val CHANNEL_NAME = "MemoCare Critical Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val reminderName = intent.getStringExtra("REMINDER_NAME") ?: "Medication Reminder"
        val reminderNotes = intent.getStringExtra("REMINDER_NOTES") ?: ""
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "Medical"

        Log.d(TAG, "Alarm received for Reminder ID: $reminderId - Name: $reminderName")

        if (reminderId == -1) return

        // 1. Create the alert intent pointing to our Full-Screen taking over Activity
        val alertIntent = Intent(context, com.example.ui.AlertActivity::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
            putExtra("REMINDER_NAME", reminderName)
            putExtra("REMINDER_NOTES", reminderNotes)
            putExtra("REMINDER_TYPE", reminderType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // 2. Setup Notification Channel
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires full-screen reminder takeovers"
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Build Full Screen Intent
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            alertIntent,
            pendingFlags
        )

        // 4. Build Notification with setFullScreenIntent
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Critical Reminder: $reminderName")
            .setContentText(reminderNotes.ifBlank { "Time to take action on your reminder." })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Launches full screen takeover
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Trigger notification
        notificationManager.notify(reminderId, notificationBuilder.build())

        // Also launch the activity directly as fallback for active foreground situations
        try {
            context.startActivity(alertIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch activity directly, relying on setFullScreenIntent notification", e)
        }
    }
}
