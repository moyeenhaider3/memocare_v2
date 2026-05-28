package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Reminder
import java.util.*

object AlarmHelper {
    private const val TAG = "AlarmHelper"

    fun scheduleReminderAlarm(context: Context, reminder: Reminder) {
        val timeString = reminder.scheduledTime ?: return
        if (!reminder.isActive) {
            cancelReminderAlarm(context, reminder.id)
            return
        }

        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            // Check if reminder is scheduled for this specific day of week
            // (If not daily, we schedule anyway and the broadcast receiver can check, 
            // or we can find the next day it's active. Let's find the next active day to be precise!)
            var loops = 0
            while (!reminder.isActiveOnDay(calendar.get(Calendar.DAY_OF_WEEK)) && loops < 7) {
                calendar.add(Calendar.DATE, 1)
                loops++
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("REMINDER_ID", reminder.id)
                putExtra("REMINDER_NAME", reminder.name)
                putExtra("REMINDER_NOTES", reminder.notes)
                putExtra("REMINDER_TYPE", reminder.type)
            }

            // Flag immutable or mutable properly
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                flags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${reminder.name} at ${calendar.time} (id: ${reminder.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm for reminder ${reminder.id}", e)
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java)
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                flags
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Canceled alarm for reminder id: $reminderId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling alarm for $reminderId", e)
        }
    }
}
