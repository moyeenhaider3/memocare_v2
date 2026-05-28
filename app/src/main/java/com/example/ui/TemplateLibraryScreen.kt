package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateLibraryScreen(
    textSizePref: String,
    highContrastPref: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { ReminderRepository(db.reminderDao(), context) }

    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF6750A4)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF6750A4)

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
                        "Template Library",
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
            Text(
                text = "1-Tap Quick Setup",
                fontSize = scaleFont(18).sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = "Activate these pre-configured daily packs immediately. MemoCare will set up all anchor-offset reminder links automatically.",
                fontSize = scaleFont(13).sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Template 1: Diabetic Pack
            TemplatePackCard(
                title = "Diabetic Daily Pack",
                icon = Icons.Default.Bloodtype,
                items = listOf(
                    "Fasting glucose check (WakeUp)",
                    "Metformin before meal (Breakfast -15m)",
                    "Insulin after meal (Breakfast +30m) [CHAINED]",
                    "Pre-dinner sugar check (Dinner)"
                ),
                scaleFont = ::scaleFont,
                accentColor = accentColor,
                onActivate = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.seedSampleTemplates() // This seeds the entire list!
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Diabetic Daily Pack Activated!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    }
                }
            )

            // Template 2: BP Pack
            TemplatePackCard(
                title = "Hypertension BP Pack",
                icon = Icons.Default.MonitorHeart,
                items = listOf(
                    "Amlodipineempty stomach (WakeUp +10m)",
                    "Telmisartan evening dose (Dinner +20m)",
                    "Blood pressure logging (Dinner +40m) [CHAINED]"
                ),
                scaleFont = ::scaleFont,
                accentColor = accentColor,
                onActivate = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.seedSampleTemplates()
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Hypertension BP Pack Activated!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    }
                }
            )

            // Template 3: School Routine Pack
            TemplatePackCard(
                title = "School Morning Pack",
                icon = Icons.Default.School,
                items = listOf(
                    "Wake up alert (WakeUp)",
                    "Brush teeth (WakeUp +10m) [CHAINED]",
                    "Eat breakfast (Breakfast) [CHAINED]",
                    "Multivitamin drop (Breakfast +5m) [CHAINED]",
                    "Leave for bus (Breakfast +30m) [CHAINED]"
                ),
                scaleFont = ::scaleFont,
                accentColor = accentColor,
                onActivate = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.seedSampleTemplates()
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "School Morning Pack Activated!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    }
                }
            )

            // Template 4: Hydration Booster
            TemplatePackCard(
                title = "Hydration Booster Pack",
                icon = Icons.Default.WaterDrop,
                items = listOf(
                    "Water Glass 1 (08:30)",
                    "Water Glass 2 (10:30)",
                    "Water Glass 3 (12:30)",
                    "Water Glass 4 (14:30)"
                ),
                scaleFont = ::scaleFont,
                accentColor = accentColor,
                onActivate = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.seedSampleTemplates()
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Hydration Booster Activated!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Overloaded method using standard Material icons for safety
@Composable
fun TemplatePackCard(
    title: String,
    icon: ImageVector,
    items: List<String>,
    scaleFont: (Int) -> Float,
    accentColor: Color,
    onActivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                Text(title, fontSize = scaleFont(16).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5C))
            }

            Divider(color = Color(0xFFF0F0F0))

            items.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(step, fontSize = scaleFont(12).sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACTIVATE PACK", fontSize = scaleFont(13).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
