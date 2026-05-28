package com.example.ui

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import com.example.data.Reminder
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class AlertActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var reminderId = -1
    private var reminderName = "Medication Reminder"
    private var reminderNotes = ""
    private var reminderType = "Medical"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Screen wake and overlay flags
        setupScreenFlags()

        // Disable standard back press gesture/button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing. Dismissal ONLY via the three buttons.
            }
        })

        // Fetch extras
        reminderId = intent.getIntExtra("REMINDER_ID", -1)
        reminderName = intent.getStringExtra("REMINDER_NAME") ?: "Medication Reminder"
        reminderNotes = intent.getStringExtra("REMINDER_NOTES") ?: ""
        reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "Medical"

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Play standard loud alert ringtone
        startAlarmSound()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AlertScreenContent(
                    reminderId = reminderId,
                    reminderName = reminderName,
                    reminderNotes = reminderNotes,
                    reminderType = reminderType,
                    onDoneClick = { performAction("DONE") },
                    onSnoozeClick = { performAction("SNOOZED") },
                    onSkipClick = { performAction("SKIPPED") }
                )
            }
        }
    }

    private fun setupScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmSound() {
        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("AlertActivity", "Ringtone error", e)
        }
    }

    private fun performAction(action: String) {
        // Stop sounds & Speech
        stopAudio()

        val context = this
        val db = AppDatabase.getDatabase(context)
        val repo = ReminderRepository(db.reminderDao(), context)

        CoroutineScope(Dispatchers.IO).launch {
            if (reminderId != -1) {
                repo.confirmReminder(reminderId, action)
            }

            // If user clicked SKIP, dispatch the WhatsApp caregiver message deep link
            if (action == "SKIPPED") {
                val caregiverWhatsApp = repo.getPrefString("caregiver_whatsapp", "")
                val caregiverName = repo.getPrefString("caregiver_name", "Caregiver")
                val userName = repo.getPrefString("user_name", "User")
                
                if (caregiverWhatsApp.isNotBlank()) {
                    val message = "Hi $caregiverName, I just skipped my alert for '$reminderName'. Wanted to let you know. - $userName"
                    launchWhatsApp(caregiverWhatsApp, message)
                }
            }

            launch(Dispatchers.Main) {
                finish()
            }
        }
    }

    private fun launchWhatsApp(number: String, message: String) {
        val sanitizedNumber = number.replace("+", "").replace(" ", "")
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uriString = "whatsapp://send?phone=$sanitizedNumber&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback opening URL
            try {
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val fallbackUri = "https://wa.me/$sanitizedNumber?text=$encodedMsg"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (ex: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Could not open WhatsApp. Caregiver alert failed locally.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun stopAudio() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            val textToSpeak = "Time alert: $reminderName. $reminderNotes"
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "MemoSpeech")
        }
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }
}

@Composable
fun AlertScreenContent(
    reminderId: Int,
    reminderName: String,
    reminderNotes: String,
    reminderType: String,
    onDoneClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val context = LocalContext.current
    var caregiverName by remember { mutableStateOf("Caregiver") }
    var parentReminder by remember { mutableStateOf<Reminder?>(null) }
    var childrenReminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }

    LaunchedEffect(reminderId) {
        val db = AppDatabase.getDatabase(context)
        val repo = ReminderRepository(db.reminderDao(), context)
        caregiverName = repo.getPrefString("caregiver_name", "Caregiver")
        
        if (reminderId != -1 && reminderId != 999) {
            try {
                val rem = db.reminderDao().getReminderById(reminderId)
                if (rem != null) {
                    if (rem.parentId != null) {
                        parentReminder = db.reminderDao().getReminderById(rem.parentId)
                    }
                    childrenReminders = db.reminderDao().getChildrenReminders(reminderId)
                }
            } catch (e: Exception) {
                Log.e("AlertActivity", "Error loading reminder sequence", e)
            }
        }
    }

    // 2-second slower breathing pulse animation for background
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Pulsate the Bell Icon for high alert
    val bellScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val timeText = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val dateText = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }

    val baseNavy = Color(0xFF1A3A5C)
    // Breathing pulsed Navy color
    val breathingColor = baseNavy.copy(alpha = pulseAlpha)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2236)) // solid bottom backing
            .background(breathingColor)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // TOP SECTION: Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "REMINDER ALERT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Ringing Reminder Bell",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier
                        .size(64.dp * bellScale)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = dateText,
                    color = Color(0xFFAACCFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CENTER SECTION: Large Medicine Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = reminderType.uppercase(Locale.getDefault()),
                        color = Color(0xFF0288D1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = reminderName,
                        color = Color(0xFF1A3A5C),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "👉 ACTION TO TAKE NOW:",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )
                            
                            val promptText = when (reminderType.lowercase(Locale.getDefault())) {
                                "medical" -> "Please take this medication directly as prescribed. Double-check your dosage instructions."
                                "meal" -> "It is time to consume your meal. Setup the tray and enjoy warm nutrition."
                                "hydration" -> "Ensure you drink a full glass of water now to stay hydrated and energized."
                                "lifestyle" -> "Carry out your physical exercise, cognitive exercise, or daily routine habit now."
                                else -> "Proceed to perform this critical care routine task immediately."
                            }
                            Text(
                                text = promptText,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            )
                            
                            if (reminderNotes.isNotBlank()) {
                                Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "📝 Specific Instructions:\n$reminderNotes",
                                    fontSize = 14.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic interconnected dependency chain guide
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (childrenReminders.isNotEmpty()) Color(0xFFFFF8E1) else Color(0xFFF1F5F9)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (childrenReminders.isNotEmpty()) Color(0xFFFFD54F) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🔗 INTERCONNECTED CHAIN STATUS:",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (childrenReminders.isNotEmpty()) Color(0xFF7F5F00) else Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )

                            if (parentReminder != null) {
                                Text(
                                    text = "✅ Chained following completed task: '${parentReminder?.name}'",
                                    fontSize = 13.sp,
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp
                                )
                            } else {
                                Text(
                                    text = "📌 This is a top-level independent starting routine task.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 18.sp
                                )
                            }

                            if (childrenReminders.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "➡️ ON-COMPLETION ACTION: Clicking 'I've Done It' will automatically trigger and schedule the next chain sequence tasks: '${childrenReminders.joinToString { it.name }}' inside the next ${childrenReminders.firstOrNull()?.offsetMinutes ?: 0} minutes.",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "🏁 This is the final step in the current connected sequence. Completion finishes the chain!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // BOTTOM SECTION: Stacked Large Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DONE Button: 72dp, dark green
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "I've Done It",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // SNOOZE Button: 72dp, outlined
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSnoozeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Remind me in 10 min",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // SKIP Button: 48dp, borderless
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSkipClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skip This Time",
                        color = Color(0xFFCCCCCC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Caregiver Note
                Text(
                    text = "Tapping Skip will open WhatsApp to message $caregiverName.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}
