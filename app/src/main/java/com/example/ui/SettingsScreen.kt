package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    textSizePref: String,
    onTextSizeChange: (String) -> Unit,
    highContrastPref: Boolean,
    onHighContrastChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    // State bindings
    var userName by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    var caregiverWhatsApp by remember { mutableStateOf("") }
    var snoozeDuration by remember { mutableStateOf(10) }
    var escalationDelay by remember { mutableStateOf(5) }
    
    // Meal times state
    var mealWakeup by remember { mutableStateOf("07:00") }
    var mealBreakfast by remember { mutableStateOf("08:00") }
    var mealLunch by remember { mutableStateOf("13:00") }
    var mealDinner by remember { mutableStateOf("20:00") }
    var mealSleep by remember { mutableStateOf("22:00") }

    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF1A3A5C)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF0288D1)

    val bentoTextColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (highContrastPref) Color.Black else Color(0xFF1E293B),
        unfocusedTextColor = if (highContrastPref) Color.Black else Color(0xFF1E293B),
        focusedLabelColor = if (highContrastPref) Color.Black else primaryColor,
        unfocusedLabelColor = Color.Gray,
        focusedBorderColor = if (highContrastPref) Color.Black else primaryColor,
        unfocusedBorderColor = if (highContrastPref) Color.Gray else Color(0xFFE6E0E9),
        cursorColor = primaryColor,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    // Load values
    LaunchedEffect(Unit) {
        userName = repo.getPrefString("user_name", "Abdul")
        caregiverName = repo.getPrefString("caregiver_name", "Suresh")
        caregiverWhatsApp = repo.getPrefString("caregiver_whatsapp", "+919876543210")
        snoozeDuration = repo.getPrefInt("snooze_duration", 10)
        escalationDelay = repo.getPrefInt("escalation_delay", 5)
        
        mealWakeup = repo.getPrefString("meal_wakeup", "07:00")
        mealBreakfast = repo.getPrefString("meal_breakfast", "08:00")
        mealLunch = repo.getPrefString("meal_lunch", "13:00")
        mealDinner = repo.getPrefString("meal_dinner", "20:00")
        mealSleep = repo.getPrefString("meal_sleep", "22:00")
    }

    // Helper scale function
    fun scaleFont(baseSp: Int): Float {
        return when (textSizePref) {
            "Large" -> baseSp * 1.25f
            "Extra Large" -> baseSp * 1.50f
            else -> baseSp * 1.0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = scaleFont(22).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    // Save everything
                    repo.editPrefs {
                        putString("user_name", userName)
                        putString("caregiver_name", caregiverName)
                        putString("caregiver_whatsapp", caregiverWhatsApp)
                        putInt("snooze_duration", snoozeDuration)
                        putInt("escalation_delay", escalationDelay)
                        putString("meal_wakeup", mealWakeup)
                        putString("meal_breakfast", mealBreakfast)
                        putString("meal_lunch", mealLunch)
                        putString("meal_dinner", mealDinner)
                        putString("meal_sleep", mealSleep)
                    }
                    // Trigger alarm recomputations because anchor values may have shifted
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.recalculateAllScheduledTimes()
                    }
                    Toast.makeText(context, "Settings saved & alerts synchronized!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SAVE SETTINGS",
                    fontSize = scaleFont(16).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (highContrastPref) Color.White else Color(0xFFFDF8F6))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: User Profile & Caregiver
            SettingsSectionTitle(title = "My Care Details", scaleFont = ::scaleFont, color = primaryColor)
            
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("My Name") },
                colors = bentoTextColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = caregiverName,
                onValueChange = { caregiverName = it },
                label = { Text("Caregiver Name") },
                colors = bentoTextColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = caregiverWhatsApp,
                onValueChange = { caregiverWhatsApp = it },
                label = { Text("Caregiver WhatsApp Number") },
                colors = bentoTextColors,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+919876543210") }
            )

            Text(
                "MemoCare never sends automated messages. When you tap 'SKIP', we pre-fill a WhatsApp chat for you.",
                fontSize = scaleFont(12).sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Premium Billing Subscription Details Card in Settings Screen
            val billingManager = remember { com.example.billing.BillingManager.getInstance(context) }
            val isSubscribed by billingManager.isSubscribed.collectAsState(initial = false)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSubscribed) Color(0xFFE8F5E9) else (if (highContrastPref) Color.White else Color(0xFFFFF8E1))
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSubscribed) Color(0xFF81C784) else (if (highContrastPref) Color.Black else Color(0xFFFFD54F))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPaywall() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Default.WorkspacePremium else Icons.Default.Star,
                        contentDescription = "Subscription Detail",
                        tint = if (isSubscribed) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSubscribed) "MemoCare Gold Active" else "Upgrade to MemoCare Gold",
                            fontSize = scaleFont(14).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSubscribed) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                        Text(
                            text = if (isSubscribed) {
                                "✨ Gold Member Account\nEnjoy unlimited sequences, absolute overlay takeovers, and status logs sync."
                            } else {
                                "Query active plans with 30-day free trial. Unlock unlimited sequences, full screen takeover alerts, on-completion actions and more!"
                            },
                            fontSize = scaleFont(11).sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Open Subscription",
                        tint = if (isSubscribed) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }

            Divider()

            // Section 2: Accessibility Styling
            SettingsSectionTitle(title = "Display Settings", scaleFont = ::scaleFont, color = primaryColor)

            // Text Size Picker
            Column {
                Text("Text Font Size", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizes = listOf("Medium", "Large", "Extra Large")
                    sizes.forEach { size ->
                        val selected = textSizePref == size
                        Button(
                            onClick = { onTextSizeChange(size) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) accentColor else Color.LightGray,
                                contentColor = if (selected) Color.White else Color.Black
                            )
                        ) {
                            Text(size, fontSize = 12.sp)
                        }
                    }
                }
            }

            // High Contrast Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("High Contrast Mode", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold)
                    Text("Extra visible dark text on white backing", fontSize = scaleFont(11).sp, color = Color.Gray)
                }
                Switch(
                    checked = highContrastPref,
                    onCheckedChange = onHighContrastChange
                )
            }

            Divider()

            // Section: Advanced / System permissions
            SettingsSectionTitle(title = "System Configuration", scaleFont = ::scaleFont, color = primaryColor)

            var hasOverlayPermission by remember { mutableStateOf(true) }
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            android.provider.Settings.canDrawOverlays(context)
                        } else {
                            true
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasOverlayPermission) {
                        if (highContrastPref) Color.White else Color(0xFFE8F5E9)
                    } else {
                        if (highContrastPref) Color.White else Color(0xFFFFF3E0)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasOverlayPermission) Color(0xFF81C784) else Color(0xFFFFB74D)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Direct Full Screen Takeover",
                                fontSize = scaleFont(14).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasOverlayPermission) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                            Text(
                                text = if (hasOverlayPermission) {
                                    "Allowed: Reminders will wake up the screen and show takeover screen immediately!"
                                } else {
                                    "Disabled: Under background restrictions, alerts show only as notifications. Enable overlay to directly show takeover screen."
                                },
                                fontSize = scaleFont(11).sp,
                                color = Color.DarkGray
                            )
                        }
                        
                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        try {
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intentFallback = android.content.Intent(
                                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                            ).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intentFallback)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overlay approved",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            Divider()

            // Section 3: Alarm Timers & Snoozes
            SettingsSectionTitle(title = "Alert Timers", scaleFont = ::scaleFont, color = primaryColor)

            Column {
                Text("Snooze Duration: $snoozeDuration Minutes", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = snoozeDuration.toFloat(),
                    onValueChange = { snoozeDuration = it.toInt() },
                    valueRange = 5f..30f,
                    steps = 5
                )
            }

            Column {
                Text("Escalation Delay: $escalationDelay Minutes", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = escalationDelay.toFloat(),
                    onValueChange = { escalationDelay = it.toInt() },
                    valueRange = 2f..15f,
                    steps = 6
                )
            }

            Divider()

            // Section 4: Daily Anchor Times Configuration
            SettingsSectionTitle(title = "My Daily Routine Times", scaleFont = ::scaleFont, color = primaryColor)
            Text("Adjust these to fit when you normally perform your main routines today. All linked offset reminders recalculate instantly.", fontSize = scaleFont(12).sp, color = Color.Gray)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mealWakeup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wake Up") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Wake Up Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, mealWakeup) { selectedTime ->
                                    mealWakeup = selectedTime
                                }
                            }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mealBreakfast,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Breakfast") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Breakfast Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, mealBreakfast) { selectedTime ->
                                    mealBreakfast = selectedTime
                                }
                            }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mealLunch,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lunch") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Lunch Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, mealLunch) { selectedTime ->
                                    mealLunch = selectedTime
                                }
                            }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mealDinner,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dinner") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Dinner Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, mealDinner) { selectedTime ->
                                    mealDinner = selectedTime
                                }
                            }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mealSleep,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sleep") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Sleep Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, mealSleep) { selectedTime ->
                                    mealSleep = selectedTime
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, scaleFont: (Int) -> Float, color: Color) {
    Text(
        text = title,
        fontSize = scaleFont(16).sp,
        fontWeight = FontWeight.Black,
        color = color,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

private fun showTimePickerDialog(
    context: android.content.Context,
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val calendar = java.util.Calendar.getInstance()
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: calendar.get(java.util.Calendar.MINUTE)

    android.app.TimePickerDialog(
        context,
        { _, h, m ->
            val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
            onTimeSelected(formattedTime)
        },
        hour,
        minute,
        true
    ).show()
}
