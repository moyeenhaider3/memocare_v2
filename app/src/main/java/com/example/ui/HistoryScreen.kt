package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.ConfirmationLog
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    textSizePref: String,
    highContrastPref: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    val allLogs by repo.allLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF1A3A5C)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF0288D1)

    fun scaleFont(baseSp: Int): Float {
        return when (textSizePref) {
            "Large" -> baseSp * 1.25f
            "Extra Large" -> baseSp * 1.50f
            else -> baseSp * 1.0f
        }
    }

    // Export Logs helper using native sharing sheet (CSV format)
    fun exportLogs(logs: List<ConfirmationLog>) {
        if (logs.isEmpty()) {
            android.widget.Toast.makeText(context, "No logs to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvBuilder = StringBuilder()
        csvBuilder.append("Reminder Name,Action,Timestamp\n")
        logs.forEach { log ->
            val logTime = sdf.format(Date(log.actionedAt))
            val nameEscaped = log.reminderName.replace("\"", "\"\"")
            csvBuilder.append("\"$nameEscaped\",${log.action},\"$logTime\"\n")
        }
        
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TITLE, "My MemoCare Compliance Logs")
            putExtra(android.content.Intent.EXTRA_SUBJECT, "MemoCare Compliance Logs Export")
            putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
            type = "text/csv"
        }
        
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Compliance History")
        context.startActivity(shareIntent)
    }

    // Confirmation AlertDialog with 3 options: Cancel, Export, and Permanent Clear
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { 
                Text(
                    "Clear Compliance History?", 
                    fontSize = scaleFont(18).sp, 
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                ) 
            },
            text = {
                Text(
                    "This action will permanently delete all your logged medical compliance history. " +
                    "Would you like to export your logs to save them before they are lost forever?",
                    fontSize = scaleFont(13).sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportLogs(allLogs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Log History", fontSize = scaleFont(14).sp)
                    }

                    Button(
                        onClick = {
                            showClearDialog = false
                            CoroutineScope(Dispatchers.IO).launch {
                                repo.clearLogs()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Permanently", fontSize = scaleFont(14).sp, color = Color.White)
                    }
                    
                    OutlinedButton(
                        onClick = { showClearDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontSize = scaleFont(14).sp)
                    }
                }
            }
        )
    }

    // Calculations
    val doneCount = allLogs.count { it.action == "DONE" }
    val skippedCount = allLogs.count { it.action == "SKIPPED" }
    val snoozeCount = allLogs.count { it.action == "SNOOZED" }
    val totalActionable = doneCount + skippedCount
    val complianceRate = if (totalActionable > 0) {
        (doneCount.toFloat() / totalActionable * 100).toInt()
    } else 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Compliance Log",
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
                actions = {
                    if (allLogs.isNotEmpty()) {
                        IconButton(onClick = {
                            exportLogs(allLogs)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Logs", tint = Color.White)
                        }
                        IconButton(onClick = {
                            showClearDialog = true
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(if (highContrastPref) Color.White else Color(0xFFF7F9FC))
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("My Compliance", color = Color.White.copy(alpha = 0.7f), fontSize = scaleFont(12).sp)
                            Text("$complianceRate% Success", color = Color.White, fontSize = scaleFont(20).sp, fontWeight = FontWeight.Black)
                            Text("$doneCount Done / $skippedCount Skipped", color = Color.White.copy(alpha = 0.7f), fontSize = scaleFont(12).sp)
                        }

                        // Rounded Progress Gauge (Text based for lightweight)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏆",
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "History Logs",
                    fontSize = scaleFont(16).sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            if (allLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                            Text("No logs yet", fontSize = scaleFont(14).sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Completed, skipped, or snoozed task logs appear here.", fontSize = scaleFont(12).sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(allLogs) { log ->
                    HistoryLogItem(log = log, scaleFont = ::scaleFont, accentColor = accentColor)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HistoryLogItem(
    log: ConfirmationLog,
    scaleFont: (Int) -> Float,
    accentColor: Color
) {
    val sdf = remember { SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.actionedAt) { sdf.format(Date(log.actionedAt)) }

    val (actionLabel, color) = when (log.action) {
        "DONE" -> Pair("DONE", Color(0xFF27AE60))
        "SKIPPED" -> Pair("SKIPPED", Color(0xFFE53935))
        "SNOOZED" -> Pair("SNOOZED", Color(0xFFFFB300))
        else -> Pair("MISSED", Color.DarkGray)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(log.reminderName, fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5C))
                Text("Logged: $formattedTime", fontSize = scaleFont(11).sp, color = Color.Gray)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = color,
                    fontSize = scaleFont(11).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
