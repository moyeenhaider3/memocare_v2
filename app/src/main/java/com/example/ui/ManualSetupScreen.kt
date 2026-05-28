package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Reminder
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSetupScreen(
    editingReminderId: Int?, // if null, in Add Mode. If not null, in Edit Mode.
    textSizePref: String,
    highContrastPref: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    val allReminders by repo.allReminders.collectAsStateWithLifecycle(initialValue = emptyList())

    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF6750A4)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF6750A4)

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

    // Form inputs
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Medical") }
    var anchorEvent by remember { mutableStateOf("Lunch") }
    var anchorTime by remember { mutableStateOf("13:00") }
    var offsetMinutes by remember { mutableStateOf("30") }
    var direction by remember { mutableStateOf("After") }
    var notes by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var isChainStart by remember { mutableStateOf(true) }

    // Dropdown toggles
    var typeExpanded by remember { mutableStateOf(false) }
    var anchorExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var parentExpanded by remember { mutableStateOf(false) }

    fun scaleFont(baseSp: Int): Float {
        return when (textSizePref) {
            "Large" -> baseSp * 1.25f
            "Extra Large" -> baseSp * 1.50f
            else -> baseSp * 1.0f
        }
    }

    // Populate editing values
    LaunchedEffect(editingReminderId) {
        if (editingReminderId != null) {
            val rem = db.reminderDao().getReminderById(editingReminderId)
            if (rem != null) {
                name = rem.name
                type = rem.type
                anchorEvent = rem.anchorEvent
                anchorTime = rem.anchorTime
                offsetMinutes = rem.offsetMinutes.toString()
                direction = rem.direction
                notes = rem.notes
                parentId = rem.parentId
                isChainStart = rem.isChainStart
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingReminderId == null) "Add Connected Reminder" else "Edit Reminder Link",
                        fontSize = scaleFont(18).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (highContrastPref) Color.Black else Color(0xFF21005D)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (highContrastPref) Color.Black else Color(0xFF6750A4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (highContrastPref) Color.White else Color(0xFFFDF8F6))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (highContrastPref) Color.White else Color(0xFFFDF8F6))
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Field 1: Name
            Text("What should we remind you about?", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medication / Routine Name (e.g., Paracetamol)") },
                colors = bentoTextColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Field 2: Type
            Text("Category", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Box {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    colors = bentoTextColors,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { typeExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { typeExpanded = true }
                )
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    val categories = listOf("Medical", "Meal", "Lifestyle", "Social", "Kids")
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                type = cat
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Field 3: Routine Anchor Link
            Text("Link to Daily Food/Sleep Routine?", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Box {
                OutlinedTextField(
                    value = if (anchorEvent == "None") "No Anchor (Hard Fixed Time)" else "Chained to $anchorEvent",
                    onValueChange = {},
                    readOnly = true,
                    colors = bentoTextColors,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { anchorExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { anchorExpanded = true }
                )
                DropdownMenu(expanded = anchorExpanded, onDismissRequest = { anchorExpanded = false }) {
                    val anchors = listOf(
                        "None" to "Fixed Static Time",
                        "WakeUp" to "Wake Up Routine",
                        "Breakfast" to "Breakfast Routine",
                        "Lunch" to "Lunch Routine",
                        "Dinner" to "Dinner Routine",
                        "Sleep" to "Sleep Routine"
                    )
                    anchors.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                anchorEvent = key
                                if (key == "None") {
                                    direction = "Fixed"
                                }
                                anchorExpanded = false
                            }
                        )
                    }
                }
            }

            // Offset fields (Only visible if an anchor index is picked)
            if (anchorEvent != "None") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = offsetMinutes,
                        onValueChange = { offsetMinutes = it },
                        label = { Text("Time gap (minutes)") },
                        colors = bentoTextColors,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = direction,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Direction") },
                            colors = bentoTextColors,
                            trailingIcon = {
                                IconButton(onClick = { directionExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { directionExpanded = true }
                        )
                        DropdownMenu(expanded = directionExpanded, onDismissRequest = { directionExpanded = false }) {
                            val options = listOf("Before", "After", "Fixed")
                            options.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        direction = opt
                                        directionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = anchorTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Static Alarm Time (Tap to select clock)") },
                        colors = bentoTextColors,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showTimePickerDialog(context, anchorTime) { selectedTime ->
                                    anchorTime = selectedTime
                                }
                            }
                    )
                }
            }

            Divider()

            // Field 4: Connected Reminder Chain mapping (DAG)
            Text("Chained Step Connection (Advanced Flow)", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("Does this alarm depend on another medication complete confirmation first?", fontSize = scaleFont(11).sp, color = Color.Gray)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isChainStart) "Independent Starting Node" else "Connected Node (Waiting for parent)",
                    fontSize = scaleFont(12).sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = !isChainStart,
                    onCheckedChange = { isChainStart = !it }
                )
            }

            if (!isChainStart) {
                val availableParents = allReminders.filter { it.id != editingReminderId }
                Box {
                    OutlinedTextField(
                        value = availableParents.find { it.id == parentId }?.name ?: "Select Parent Medicine",
                        onValueChange = {},
                        readOnly = true,
                        colors = bentoTextColors,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { parentExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { parentExpanded = true }
                    )
                    DropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                        if (availableParents.isEmpty()) {
                            DropdownMenuItem(text = { Text("No other steps available") }, onClick = {})
                        }
                        availableParents.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    parentId = p.id
                                    parentExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Divider()

            // Field 5: Dosage / Instruction Notes
            Text("Dosage / Core Instruction Note", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("e.g. Take 1 tablet, with a glass of water") },
                colors = bentoTextColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please write a name first.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (anchorEvent == "None" && !anchorTime.contains(":")) {
                        Toast.makeText(context, "Verify static time matches HH:mm format.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val offset = offsetMinutes.toIntOrNull() ?: 0

                    val reminder = Reminder(
                        id = editingReminderId ?: 0,
                        name = name,
                        type = type,
                        anchorEvent = anchorEvent,
                        anchorTime = anchorTime,
                        offsetMinutes = offset,
                        direction = direction,
                        notes = notes,
                        parentId = if (isChainStart) null else parentId,
                        isChainStart = isChainStart
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        if (editingReminderId == null) {
                            repo.insertReminder(reminder)
                        } else {
                            repo.updateReminder(reminder)
                        }
                        
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Reminder synced successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (editingReminderId == null) "SAVE AND ALIGN LINK" else "UPDATE ALIGNED LINK",
                    fontSize = scaleFont(15).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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
