package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
    onNavigateBack: () -> Unit,
    onNavigateToGuide: () -> Unit
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

    var showQuickSetupHelp by remember { mutableStateOf(false) }

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
                actions = {
                    IconButton(onClick = { showQuickSetupHelp = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Quick Help Guide",
                            tint = if (highContrastPref) Color.Black else Color(0xFF6750A4)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (highContrastPref) Color.White else Color(0xFFFDF8F6))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = if (highContrastPref) Color.White else Color(0xFFFDF8F6),
                border = if (highContrastPref) BorderStroke(1.dp, Color.Black) else null
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = if (highContrastPref) BorderStroke(2.dp, Color.Black) else BorderStroke(1.dp, Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (highContrastPref) Color.Black else Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = scaleFont(14).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                                    Toast.makeText(context, "Reminder saved successfully!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (highContrastPref) Color.Black else Color(0xFF27AE60),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (editingReminderId == null) "SAVE REMINDER" else "UPDATE REMINDER",
                            fontSize = scaleFont(14).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showQuickSetupHelp) {
            AlertDialog(
                onDismissRequest = { showQuickSetupHelp = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Help, contentDescription = null, tint = primaryColor)
                        Text(
                            text = "Parameter Quick Decoder",
                            fontSize = scaleFont(16).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Don't know what values to choose? Here is a simple reference table:",
                            fontSize = scaleFont(12).sp,
                            color = Color.DarkGray
                        )
                        
                        val decoderTips = listOf(
                            "Name / Dose" to "Medication or task details. e.g. Metformin 500mg.",
                            "Starting Step?" to "YES for an independent step; NO if this starts automatically AFTER another step completes.",
                            "Prerequisite Step" to "The precursor task. Choose which existing task must complete first.",
                            "Base Meal Anchor" to "Relative to Breakfast, Lunch, or Dinner etc., or Fixed Static Time.",
                            "Offset / Timing" to "Before/After the base time. e.g. 30 Minutes After Parent step completes."
                        )
                        
                        decoderTips.forEach { (field, info) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(field, fontSize = scaleFont(11).sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                Text(info, fontSize = scaleFont(10).sp, color = Color.Gray, lineHeight = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showQuickSetupHelp = false
                                onNavigateToGuide()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Open Full Visual Manual", fontSize = scaleFont(12).sp)
                        }
                        TextButton(
                            onClick = { showQuickSetupHelp = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss Guide", fontSize = scaleFont(12).sp)
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (highContrastPref) Color.White else Color(0xFFFDF8F6))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Help Tip Badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showQuickSetupHelp = true },
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "Quick Help", 
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Confused about fields or how to chain? Tap here for a quick decoder.",
                        fontSize = scaleFont(11).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

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
            Text("Link to Daily Routine?", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Box {
                OutlinedTextField(
                    value = if (anchorEvent == "None") "Independent (Specific Clock Time)" else "Linked to $anchorEvent routine",
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
                        "None" to "Independent (Specific Clock Time)",
                        "WakeUp" to "Waking Up Time",
                        "Breakfast" to "Breakfast Time",
                        "Lunch" to "Lunch Time",
                        "Dinner" to "Dinner Time",
                        "Sleep" to "Bedtime"
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
                        label = { Text("How many minutes?") },
                        colors = bentoTextColors,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = when (direction) {
                                "Before" -> "Before routine"
                                "After" -> "After routine"
                                else -> "Exactly at routine"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Timing") },
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
                            val options = listOf(
                                "Before" to "Before routine",
                                "After" to "After routine",
                                "Fixed" to "Exactly at routine"
                            )
                            options.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        direction = key
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
                        label = { Text("Select alarm clock time (Tap to select)") },
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

            HorizontalDivider()

            // Field 4: Connected Reminder Chain mapping (DAG)
            Text("Step-by-Step Task Ordering", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("Should this task wait until you mark another medicine as finished first?", fontSize = scaleFont(11).sp, color = Color.Gray)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isChainStart) "Starts by itself (Independent)" else "Waits for another task (Linked)",
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
                        value = availableParents.find { it.id == parentId }?.name ?: "Tap to choose which task to wait for",
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

            Spacer(modifier = Modifier.height(40.dp))
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
