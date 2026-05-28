package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
fun DashboardScreen(
    textSizePref: String,
    highContrastPref: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    val allReminders by repo.allReminders.collectAsStateWithLifecycle(initialValue = emptyList())

    val billingManager = remember { com.example.billing.BillingManager.getInstance(context) }
    val isSubscribed by billingManager.isSubscribed.collectAsStateWithLifecycle(initialValue = false)

    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF1A3A5C)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF0288D1)

    // Bento Grid colors & styling
    val bentoBg = if (highContrastPref) Color.White else Color(0xFFFDF8F6)
    val bentoPurple = if (highContrastPref) Color.Black else Color(0xFF6750A4)
    val bentoLightPurple = if (highContrastPref) Color.White else Color(0xFFEADDFF)
    val bentoDarkText = if (highContrastPref) Color.Black else Color(0xFF21005D)
    val bentoGrayBg = if (highContrastPref) Color.White else Color(0xFFF3EDF7)
    val bentoBorder = if (highContrastPref) Color.Black else Color(0xFFE6E0E9)

    var userName by remember { mutableStateOf("Abdul") }

    LaunchedEffect(Unit) {
        userName = repo.getPrefString("user_name", "Abdul")
    }

    fun scaleFont(baseSp: Int): Float {
        return when (textSizePref) {
            "Large" -> baseSp * 1.25f
            "Extra Large" -> baseSp * 1.50f
            else -> baseSp * 1.0f
        }
    }

    // Calculate completions
    val totalToday = allReminders.size
    val doneToday = allReminders.count { it.lastAction == "DONE" }
    val progressFraction = if (totalToday > 0) doneToday.toFloat() / totalToday else 0.0f

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User avatar box
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bentoLightPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                tint = bentoDarkText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                "Good Day Caregiver",
                                fontSize = scaleFont(11).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = userName,
                                fontSize = scaleFont(18).sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToPaywall,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSubscribed) Color(0xFFE8F5E9) else (if (highContrastPref) Color.LightGray else Color(0xFFFFF8E1)))
                    ) {
                        Icon(
                            imageVector = if (isSubscribed) Icons.Default.WorkspacePremium else Icons.Default.Star,
                            contentDescription = "Subscription Detail",
                            tint = if (isSubscribed) Color(0xFF2E7D32) else (if (highContrastPref) Color.Black else Color(0xFFFBC02D))
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (highContrastPref) Color.LightGray else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Logs",
                            tint = primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (highContrastPref) Color.LightGray else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Setting Options",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bentoBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = bentoPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder Link")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bentoBg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bento Grid Focus progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bentoPurple),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "DAILY PROGRESS ENGAGED",
                                    fontSize = scaleFont(10).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Simulation button helper
                            Button(
                                onClick = {
                                    val simulateIntent = Intent(context, AlertActivity::class.java).apply {
                                        putExtra("REMINDER_ID", 999)
                                        putExtra("REMINDER_NAME", "Simulated Metformin Alert 500mg")
                                        putExtra("REMINDER_NOTES", "Take 1 tablet before breakfast to observe full screen takeover.")
                                        putExtra("REMINDER_TYPE", "Medical Test")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(simulateIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("TEST TAKEOVER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Daily Care Progress",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = scaleFont(14).sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$doneToday of $totalToday Link Chains Done",
                                color = Color.White,
                                fontSize = scaleFont(20).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = progressFraction,
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                color = Color.White,
                                fontSize = scaleFont(12).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bento Stats Row: Completed Tasks & Pending Alerts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Completed Tasks (Lavender)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 145.dp),
                        colors = CardDefaults.cardColors(containerColor = bentoLightPurple),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = bentoDarkText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$doneToday",
                                    fontSize = scaleFont(28).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bentoDarkText,
                                    lineHeight = 30.sp
                                )
                                Text(
                                    text = "Tasks Done",
                                    fontSize = scaleFont(11).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = bentoDarkText.copy(alpha = 0.7f),
                                    softWrap = true
                                )
                            }
                        }
                    }

                    // Card 2: Pending Alerts (Lilac / White with border)
                    val pendingToday = (totalToday - doneToday).coerceAtLeast(0)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 145.dp),
                        colors = CardDefaults.cardColors(containerColor = bentoGrayBg),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, bentoBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = bentoPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$pendingToday",
                                    fontSize = scaleFont(28).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bentoPurple,
                                    lineHeight = 30.sp
                                )
                                Text(
                                    text = "Pending Alerts",
                                    fontSize = scaleFont(11).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            if (!hasOverlayPermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (highContrastPref) Color.White else Color(0xFFFFF3E0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (highContrastPref) Color.Black else Color(0xFFFFB74D)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permission Alert Warning",
                                tint = Color(0xFFEF6C00),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable Direct Reminder Screen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = scaleFont(13).sp,
                                    color = if (highContrastPref) Color.Black else Color(0xFF0F172A)
                                )
                                Text(
                                    "Show visual takeover screen directly over your current screen instead of just a standard notification.",
                                    fontSize = scaleFont(11).sp,
                                    color = if (highContrastPref) Color.Black else Color(0xFF475569)
                                )
                            }
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
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("ENABLE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Library Shortcut Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToTemplates,
                        colors = ButtonDefaults.buttonColors(containerColor = bentoPurple.copy(alpha = 0.12f), contentColor = bentoPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Template Library", 
                                fontSize = scaleFont(11).sp, 
                                fontWeight = FontWeight.Bold,
                                softWrap = true,
                                maxLines = 2,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            // Immediately seed local sample routines to show rich demo state in 1 tap
                            CoroutineScope(Dispatchers.IO).launch {
                                repo.seedSampleTemplates()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.25f), contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-Seed Lists", 
                                fontSize = scaleFont(11).sp, 
                                fontWeight = FontWeight.Bold,
                                softWrap = true,
                                maxLines = 2,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "My Connected Chains Flow",
                    fontSize = scaleFont(15).sp,
                    fontWeight = FontWeight.Bold,
                    color = bentoPurple
                )
            }

            // 2. Schedule List of Anchored steps & Sub chain nodes
            if (allReminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                            Text("No reminder links created", fontSize = scaleFont(14).sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Tap '+' to build manual links or tap 'Template Library' above to seed them.", fontSize = scaleFont(11).sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                // Grouping Logic: Get starting anchoring reminders, and map nested steps under them
                val parents = allReminders.filter { it.isChainStart && it.parentId == null }
                val satellitesMap = allReminders.filter { it.parentId != null }.groupBy { it.parentId }

                if (!isSubscribed) {
                    item(key = "premium_promo_banner") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPaywall() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (highContrastPref) Color.White else Color(0xFFFFFAEC)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (highContrastPref) Color.Black else Color(0xFFFFD54F)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (highContrastPref) Color.Black else Color(0xFFFFF9C4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (highContrastPref) Color.White else Color(0xFFF57F17),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Try MemoCare Gold Premium Free",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = scaleFont(13).sp,
                                        color = Color(0xFF5D4037)
                                    )
                                    Text(
                                        "Unlock unlimited chained sequences, complete compliance logging, & absolute full-screen alert takeovers. Try 30-day free trial risk-free.",
                                        fontSize = scaleFont(11).sp,
                                        color = Color(0xFF795548),
                                        lineHeight = 15.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Open Paywall Details",
                                    tint = Color(0xFF795548)
                                )
                            }
                        }
                    }
                }

                parents.forEach { parent ->
                    item(key = "p-${parent.id}") {
                        DashboardReminderCard(
                            reminder = parent,
                            scaleFont = ::scaleFont,
                            accentColor = accentColor,
                            onCardClick = { onNavigateToEdit(parent.id) },
                            onDeleteClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.deleteReminderById(parent.id)
                                }
                            }
                        )
                    }

                    // Render its child satellites
                    val children = satellitesMap[parent.id] ?: emptyList()
                    items(children, key = { "c-${it.id}" }) { child ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Chains trailing dots logic using basic Canvas drawing
                            Canvas(modifier = Modifier.width(20.dp).height(48.dp)) {
                                val stroke = 2.dp.toPx()
                                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                drawLine(
                                    color = Color.LightGray,
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, size.height),
                                    strokeWidth = stroke,
                                    pathEffect = pathEffect
                                )
                                drawLine(
                                    color = Color.LightGray,
                                    start = Offset(size.width / 2, size.height / 2),
                                    end = Offset(size.width, size.height / 2),
                                    strokeWidth = stroke,
                                    pathEffect = pathEffect
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                DashboardReminderCard(
                                    reminder = child,
                                    scaleFont = ::scaleFont,
                                    accentColor = accentColor.copy(alpha = 0.8f),
                                    prefixLabel = "Chained satellite step: ",
                                    onCardClick = { onNavigateToEdit(child.id) },
                                    onDeleteClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            repo.deleteReminderById(child.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DashboardReminderCard(
    reminder: Reminder,
    scaleFont: (Int) -> Float,
    accentColor: Color,
    prefixLabel: String = "",
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusColor = when (reminder.lastAction) {
        "DONE" -> Color(0xFF27AE60)
        "SKIPPED" -> Color(0xFFD32F2F)
        "SNOOZED" -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    val statusText = when (reminder.lastAction) {
        "DONE" -> "DONE"
        "SKIPPED" -> "SKIPPED"
        "SNOOZED" -> "SNOOZED"
        else -> "WAITING"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (prefixLabel.isNotBlank()) {
                    Text(
                        text = prefixLabel.uppercase(),
                        fontSize = scaleFont(8).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    text = reminder.name,
                    fontSize = scaleFont(15).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                if (reminder.notes.isNotBlank()) {
                    Text(
                        text = reminder.notes,
                        fontSize = scaleFont(11).sp,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = statusColor
                    )
                    Text(
                        text = if (reminder.anchorEvent != "None") {
                            "${reminder.scheduledTime ?: "Waiting"} (${reminder.anchorEvent} offset ${reminder.offsetMinutes}m)"
                        } else {
                            reminder.scheduledTime ?: "Static"
                        },
                        fontSize = scaleFont(11).sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Action visual tag status
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = scaleFont(10).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Delete Icon button to keep management easy
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Link",
                        tint = Color.Gray.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
