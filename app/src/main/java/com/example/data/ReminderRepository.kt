package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.receiver.AlarmHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("memocare_prefs", Context.MODE_PRIVATE)

    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveRemindersFlow()
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllRemindersFlow()
    val allLogs: Flow<List<ConfirmationLog>> = reminderDao.getAllLogsFlow()

    init {
        // Enforce shared preference defaults
        if (!prefs.contains("user_name")) {
            prefs.edit().apply {
                putString("user_name", "Abdul")
                putString("caregiver_name", "Suresh")
                putString("caregiver_whatsapp", "+919876543210")
                putString("text_size", "Large")
                putBoolean("contrast_mode", false)
                putInt("snooze_duration", 10)
                putInt("escalation_delay", 5)
                putString("meal_wakeup", "07:00")
                putString("meal_breakfast", "08:00")
                putString("meal_lunch", "13:00")
                putString("meal_dinner", "20:00")
                putString("meal_sleep", "22:00")
                putBoolean("onboarding_complete", false)
                apply()
            }
        }
    }

    // Get simple preference values
    fun getPrefString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun getPrefInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun getPrefBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    fun editPrefs(action: SharedPreferences.Editor.() -> Unit) {
        val editor = prefs.edit()
        action(editor)
        editor.apply()
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder)
        val updated = reminderDao.getReminderById(id.toInt())
        if (updated != null) {
            val recalculated = calculateSingleScheduledTime(updated)
            reminderDao.updateReminder(recalculated)
            AlarmHelper.scheduleReminderAlarm(context, recalculated)
        }
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        val calculated = calculateSingleScheduledTime(reminder)
        reminderDao.updateReminder(calculated)
        if (calculated.isActive) {
            AlarmHelper.scheduleReminderAlarm(context, calculated)
        } else {
            AlarmHelper.cancelReminderAlarm(context, calculated.id)
        }
    }

    suspend fun deleteReminderById(id: Int) {
        AlarmHelper.cancelReminderAlarm(context, id)
        reminderDao.deleteReminderById(id)
    }

    suspend fun clearLogs() {
        reminderDao.clearAllLogs()
    }

    // Trigger chain logic and register confirmation states
    suspend fun confirmReminder(reminderId: Int, action: String) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        val now = System.currentTimeMillis()

        // 1. Log the completion state
        val log = ConfirmationLog(
            reminderId = reminder.id,
            reminderName = reminder.name,
            reminderType = reminder.type,
            scheduledAt = now, // Approximate today
            action = action,
            actionedAt = now,
            notes = reminder.notes
        )
        reminderDao.insertConfirmationLog(log)

        // 2. Update current reminder action state for today
        var updatedReminder = reminder.copy(
            lastAction = action,
            lastActionTime = now
        )

        if (action == "SNOOZED") {
            // Re-fire in snooze duration
            val snoozeMinutes = getPrefInt("snooze_duration", 10)
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, snoozeMinutes)
            val newHour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val newMinute = String.format("%02d", cal.get(Calendar.MINUTE))
            
            updatedReminder = updatedReminder.copy(
                scheduledTime = "$newHour:$newMinute"
            )
            reminderDao.updateReminder(updatedReminder)
            AlarmHelper.scheduleReminderAlarm(context, updatedReminder)
            return // Don't trigger downstream or complete yet
        }

        // Cancel existing alarm because it's either DONE or SKIPPED
        AlarmHelper.cancelReminderAlarm(context, reminder.id)

        // Reset scheduled time back to original anchor base for next days,
        // but for today, keep the action state.
        reminderDao.updateReminder(updatedReminder)

        // 3. Process Downstream Satellite Chains
        val children = reminderDao.getChildrenReminders(reminderId)
        for (child in children) {
            if (action == "DONE") {
                // LOCK IN next reminder based on CURRENT completion time + offset
                val cal = Calendar.getInstance()
                cal.add(Calendar.MINUTE, child.offsetMinutes)
                val targetHour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
                val targetMinute = String.format("%02d", cal.get(Calendar.MINUTE))
                
                val activatedChild = child.copy(
                    scheduledTime = "$targetHour:$targetMinute",
                    isActive = true,
                    lastAction = null, // Reset for its fresh run
                    lastActionTime = null
                )
                reminderDao.updateReminder(activatedChild)
                AlarmHelper.scheduleReminderAlarm(context, activatedChild)
            } else if (action == "SKIPPED") {
                // SUSPEND downstream chain for this slot
                val suspendedChild = child.copy(
                    isActive = false,
                    lastAction = "SKIPPED",
                    lastActionTime = now
                )
                reminderDao.updateReminder(suspendedChild)
                AlarmHelper.cancelReminderAlarm(context, child.id)
            }
        }
    }

    // Force system-wide recomputation of anchor-estimated times
    suspend fun recalculateAllScheduledTimes() {
        val list = reminderDao.getAllRemindersList()
        for (rem in list) {
            val updated = calculateSingleScheduledTime(rem)
            reminderDao.updateReminder(updated)
            if (updated.isActive && updated.lastAction == null) {
                AlarmHelper.scheduleReminderAlarm(context, updated)
            } else {
                AlarmHelper.cancelReminderAlarm(context, updated.id)
            }
        }
    }

    private fun calculateSingleScheduledTime(reminder: Reminder): Reminder {
        if (reminder.anchorEvent == "None" || reminder.direction == "Fixed") {
            return reminder.copy(scheduledTime = reminder.anchorTime)
        }

        // Fetch user's custom set meal/vitals times
        val anchorBaseTime = when (reminder.anchorEvent) {
            "WakeUp" -> getPrefString("meal_wakeup", "07:00")
            "Breakfast" -> getPrefString("meal_breakfast", "08:00")
            "Lunch" -> getPrefString("meal_lunch", "13:00")
            "Dinner" -> getPrefString("meal_dinner", "20:00")
            "Sleep" -> getPrefString("meal_sleep", "22:00")
            else -> "12:00"
        }

        val parts = anchorBaseTime.split(":")
        if (parts.size != 2) return reminder.copy(scheduledTime = reminder.anchorTime)

        val hour = parts[0].toIntOrNull() ?: 12
        val minute = parts[1].toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val offset = reminder.offsetMinutes
        if (reminder.direction == "Before") {
            cal.add(Calendar.MINUTE, -offset)
        } else if (reminder.direction == "After") {
            cal.add(Calendar.MINUTE, offset)
        }

        val targetHour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
        val targetMinute = String.format("%02d", cal.get(Calendar.MINUTE))

        return reminder.copy(scheduledTime = "$targetHour:$targetMinute")
    }

    // Seeds beautiful pre-defined templates if DB is raw
    suspend fun seedSampleTemplates() {
        val current = reminderDao.getAllRemindersList()
        if (current.isNotEmpty()) return

        // 1. Diabetic Daily Pack
        val sugarMorningId = reminderDao.insertReminder(
            Reminder(
                name = "Morning Blood Sugar Check",
                type = "Medical",
                anchorEvent = "WakeUp",
                offsetMinutes = 0,
                direction = "Fixed",
                notes = "Check fasting glucose. Target: < 100 mg/dL",
                isChainStart = true
            )
        ).toInt()

        val metforminId = reminderDao.insertReminder(
            Reminder(
                name = "Metformin 500mg",
                type = "Medical",
                anchorEvent = "Breakfast",
                offsetMinutes = 15,
                direction = "Before",
                notes = "Take before breakfast to regulate spike.",
                isChainStart = true
            )
        ).toInt()

        reminderDao.insertReminder(
            Reminder(
                name = "Insulin Dose",
                type = "Medical",
                anchorEvent = "Breakfast",
                offsetMinutes = 30,
                direction = "After",
                notes = "Dose with breakfast. Chained step.",
                parentId = metforminId,
                isChainStart = false // Fires only after Metformin is taken!
            )
        )

        reminderDao.insertReminder(
            Reminder(
                name = "Evening Glucose Check",
                type = "Medical",
                anchorEvent = "Dinner",
                offsetMinutes = 0,
                direction = "Fixed",
                notes = "Check blood sugar before dinner.",
                isChainStart = true
            )
        )

        // 2. BP Pack
        reminderDao.insertReminder(
            Reminder(
                name = "Amlodipine (BP Pill)",
                type = "Medical",
                anchorEvent = "WakeUp",
                offsetMinutes = 10,
                direction = "After",
                notes = "Take on empty stomach with water.",
                isChainStart = true
            )
        )

        val bpEveningId = reminderDao.insertReminder(
            Reminder(
                name = "Telmisartan (Evening BP)",
                type = "Medical",
                anchorEvent = "Dinner",
                offsetMinutes = 20,
                direction = "After",
                notes = "Take in evening. Linked to BP Check downstairs.",
                isChainStart = true
            )
        ).toInt()

        reminderDao.insertReminder(
            Reminder(
                name = "BP measurement check",
                type = "Medical",
                anchorEvent = "Dinner",
                offsetMinutes = 40,
                direction = "After",
                notes = "Log blood pressure using monitor.",
                parentId = bpEveningId,
                isChainStart = false // Fires after BP pill taken!
            )
        )

        // 3. School Morning Pack
        val wakeupKidsId = reminderDao.insertReminder(
            Reminder(
                name = "Wake up, Aryan!",
                type = "Kids",
                anchorEvent = "WakeUp",
                offsetMinutes = 0,
                direction = "Fixed",
                notes = "Stretch and drink a glass of warm water.",
                isChainStart = true
            )
        ).toInt()

        val brushKidsId = reminderDao.insertReminder(
            Reminder(
                name = "Brush Teeth",
                type = "Kids",
                anchorEvent = "WakeUp",
                offsetMinutes = 10,
                direction = "After",
                notes = "2 minutes thorough brushing.",
                parentId = wakeupKidsId,
                isChainStart = false
            )
        ).toInt()

        val breakfastKidsId = reminderDao.insertReminder(
            Reminder(
                name = "Eat Breakfast",
                type = "Kids",
                anchorEvent = "Breakfast",
                offsetMinutes = 0,
                direction = "Fixed",
                notes = "Healthy breakfast.",
                parentId = brushKidsId,
                isChainStart = false
            )
        ).toInt()

        reminderDao.insertReminder(
            Reminder(
                name = "Multivitamin Drop",
                type = "Kids",
                anchorEvent = "Breakfast",
                offsetMinutes = 5,
                direction = "After",
                notes = "Chewable tablet after food.",
                parentId = breakfastKidsId,
                isChainStart = false
            )
        )

        reminderDao.insertReminder(
            Reminder(
                name = "Leave for School School Bus",
                type = "Kids",
                anchorEvent = "Breakfast",
                offsetMinutes = 30,
                direction = "After",
                notes = "Double check school bag. Stay safe!",
                parentId = breakfastKidsId,
                isChainStart = false
            )
        )

        // 4. Hydration Booster
        reminderDao.insertReminder(
            Reminder(
                name = "Water Tracker Glass 1/8",
                type = "Hydration",
                anchorEvent = "None",
                anchorTime = "08:30",
                notes = "Keep yourself hydrated.",
                isChainStart = true
            )
        )
        reminderDao.insertReminder(
            Reminder(
                name = "Water Tracker Glass 2/8",
                type = "Hydration",
                anchorEvent = "None",
                anchorTime = "10:30",
                notes = "Keep yourself hydrated.",
                isChainStart = true
            )
        )
        reminderDao.insertReminder(
            Reminder(
                name = "Water Tracker Glass 3/8",
                type = "Hydration",
                anchorEvent = "None",
                anchorTime = "12:30",
                notes = "Keep yourself hydrated.",
                isChainStart = true
            )
        )
        reminderDao.insertReminder(
            Reminder(
                name = "Water Tracker Glass 4/8",
                type = "Hydration",
                anchorEvent = "None",
                anchorTime = "14:30",
                notes = "Keep yourself hydrated.",
                isChainStart = true
            )
        )

        recalculateAllScheduledTimes()
    }
}
