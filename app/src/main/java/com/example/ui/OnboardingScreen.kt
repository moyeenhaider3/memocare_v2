package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    textSizePref: String,
    onTextSizeChange: (String) -> Unit,
    highContrastPref: Boolean,
    onHighContrastChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    var currentSlide by remember { mutableStateOf(1) }
    
    // User Pref states
    var nameInput by remember { mutableStateOf("Abdul") }
    var caregiverName by remember { mutableStateOf("Son") }
    var caregiverWhatsBy by remember { mutableStateOf("+919876543210") }

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

    fun scaleFont(baseSp: Int): Float {
        return when (textSizePref) {
            "Large" -> baseSp * 1.25f
            "Extra Large" -> baseSp * 1.50f
            else -> baseSp * 1.0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastPref) Color.White else Color(0xFFFDF8F6))
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "MemoCare",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
            }

            // Carousel Slide Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "slide_transition"
                ) { slide ->
                    when (slide) {
                        1 -> OnboardingSlideInfo(
                            title = "Smart Reminders\nThat Understand You",
                            description = "Unlike normal chaotic alarm apps, MemoCare understands the relationship between tasks - connecting medications to your meal times dynamically.",
                            icon = Icons.Default.Hub,
                            scaleFont = ::scaleFont,
                            primaryColor = primaryColor,
                            accentColor = accentColor
                        )
                        2 -> OnboardingSlideInfo(
                            title = "Connected Reminder Chains",
                            description = "Define a mealtime, and MemoCare schedules satellite reminders (Paracetamol before, syrup after). Once you tap DONE, subsequent alerts lock in automatically.",
                            icon = Icons.Default.Link,
                            scaleFont = ::scaleFont,
                            primaryColor = primaryColor,
                            accentColor = accentColor
                        )
                        3 -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Choose Accessibility Level",
                                fontSize = scaleFont(22).sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "MemoCare is designed with high contrast and huge fonts to make reading clean for everyone.",
                                fontSize = scaleFont(14).sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Text Size Buttons
                            Text("Text Size Preferences", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Medium", "Large", "Extra Large").forEach { size ->
                                    val isSel = textSizePref == size
                                    Button(
                                        onClick = { onTextSizeChange(size) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) accentColor else Color.LightGray
                                        )
                                    ) {
                                        Text(size, fontSize = 11.sp, color = if (isSel) Color.White else Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // High Contrast switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("High Contrast Colors", fontSize = scaleFont(14).sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                Switch(
                                    checked = highContrastPref,
                                    onCheckedChange = onHighContrastChange
                                )
                            }
                        }
                        4 -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                "Add a Family Member",
                                fontSize = scaleFont(20).sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Skip reminders safely. When you skip, we help prepare a WhatsApp message for your caregiver.",
                                fontSize = scaleFont(14).sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("My Name") },
                                colors = bentoTextColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = caregiverName,
                                onValueChange = { caregiverName = it },
                                label = { Text("Caregiver Name (e.g. Son)") },
                                colors = bentoTextColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = caregiverWhatsBy,
                                onValueChange = { caregiverWhatsBy = it },
                                label = { Text("Caregiver WhatsApp (Country code)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = bentoTextColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Your numbers remain 100% offline. We never send messages automatically.",
                                fontSize = scaleFont(12).sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        5 -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                "All Hooked Up!",
                                fontSize = scaleFont(24).sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "MemoCare is configured and ready. We have prepared quick smart templates for you on the dashboard.",
                                fontSize = scaleFont(15).sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Bottom Nav Button Stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Carousel bullet indicators
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentSlide == i + 1) accentColor else Color.LightGray
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentSlide > 1) {
                        TextButton(onClick = { currentSlide-- }) {
                            Text("BACK", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    if (currentSlide < 5) {
                        Button(
                            onClick = { currentSlide++ },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("CONTINUE", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                // Save profile preferences
                                repo.editPrefs {
                                    putString("user_name", nameInput)
                                    putString("caregiver_name", caregiverName)
                                    putString("caregiver_whatsapp", caregiverWhatsBy)
                                    putBoolean("onboarding_complete", true)
                                    apply()
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.recalculateAllScheduledTimes()
                                }
                                onFinished()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("LET'S SECURE MY CARE", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingSlideInfo(
    title: String,
    description: String,
    icon: ImageVector,
    scaleFont: (Int) -> Float,
    primaryColor: Color,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = title,
            fontSize = scaleFont(22).sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            fontSize = scaleFont(14).sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
