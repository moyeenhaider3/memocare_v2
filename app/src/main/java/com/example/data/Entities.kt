package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Medical, Meal, Lifestyle, Kids, Hydration
    val anchorEvent: String = "None", // Breakfast, Lunch, Dinner, WakeUp, Sleep, None
    val anchorTime: String = "13:00", // HH:MM
    val offsetMinutes: Int = 0,
    val direction: String = "Fixed", // Before, After, Fixed
    val notes: String = "",
    val parentId: Int? = null, // Link to previous step in chain
    val isChainStart: Boolean = true, // Scheduled automatically, or only after parent is done
    val repeatDays: String = "1,2,3,4,5,6,7", // Monday=1 ... Sunday=7
    var scheduledTime: String? = null, // Calculated HH:MM for today
    val isActive: Boolean = true,
    var lastAction: String? = null, // DONE, SNOOZED, SKIPPED, MISSED
    var lastActionTime: Long? = null
) {
    // Helper to check if reminder is active today
    fun isActiveOnDay(dayOfWeek: Int): Boolean {
        if (repeatDays.isBlank()) return true
        val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        return days.isEmpty() || days.contains(dayOfWeek)
    }
}

@Entity(tableName = "confirmation_logs")
data class ConfirmationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val reminderName: String,
    val reminderType: String,
    val scheduledAt: Long,
    val action: String, // DONE, SNOOZED, SKIPPED, MISSED
    val actionedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
