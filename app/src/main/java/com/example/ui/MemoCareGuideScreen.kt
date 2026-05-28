package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoCareGuideScreen(
    textSizePref: String,
    highContrastPref: Boolean,
    onNavigateBack: () -> Unit
) {
    val primaryColor = if (highContrastPref) Color.Black else Color(0xFF1A3A5C)
    val accentColor = if (highContrastPref) Color.Black else Color(0xFF0288D1)
    val bentoBg = if (highContrastPref) Color.White else Color(0xFFFDF8F6)
    val bentoPurple = if (highContrastPref) Color.Black else Color(0xFF6750A4)
    val bentoLightPurple = if (highContrastPref) Color.White else Color(0xFFEADDFF)
    val bentoDarkText = if (highContrastPref) Color.Black else Color(0xFF21005D)
    val bentoGrayBg = if (highContrastPref) Color.White else Color(0xFFF3EDF7)
    val bentoBorder = if (highContrastPref) Color.Black else Color(0xFFE6E0E9)

    var selectedTab by remember { mutableStateOf(0) } // 0 = Scenarios & Chains, 1 = Field Decoder, 2 = FAQ & Templates

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
                        text = "Caregiver & Setup Guide",
                        fontSize = scaleFont(18).sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bentoBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bentoBg)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bentoGrayBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Scenarios", "Field Decoder", "FAQs & Templates").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) bentoPurple else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = scaleFont(11).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> GuideScenariosSection(::scaleFont, highContrastPref, bentoBorder, bentoLightPurple, bentoDarkText)
                    1 -> GuideFieldDecoderSection(::scaleFont, highContrastPref, bentoBorder, bentoPurple, bentoLightPurple, bentoDarkText)
                    2 -> GuideFaqSection(::scaleFont, highContrastPref, bentoBorder, bentoPurple, bentoLightPurple)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GuideScenariosSection(
    scaleFont: (Int) -> Float,
    highContrastPref: Boolean,
    bentoBorder: Color,
    bentoLightPurple: Color,
    bentoDarkText: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Concept Introduction
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = bentoLightPurple),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, bentoBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.LinearScale,
                        contentDescription = null,
                        tint = bentoDarkText,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "What is a Linked Chain?",
                        fontSize = scaleFont(16).sp,
                        fontWeight = FontWeight.Bold,
                        color = bentoDarkText
                    )
                }
                Text(
                    text = "Traditional reminders fire at strict, set times. However, if care routines shift (e.g. eating breakfast late), subsequent alerts can overlap or fire too early. " +
                            "\n\nMemoCare lets you group related tasks into a sequential 'Link Chain'. If Step A is delayed, Step B automatically shifts and won't go off until a calculated interval AFTER Step A is actually completed!",
                    fontSize = scaleFont(12).sp,
                    color = bentoDarkText.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }

        Text(
            text = "Real-World Care Scenarios",
            fontSize = scaleFont(15).sp,
            fontWeight = FontWeight.Bold,
            color = bentoDarkText
        )

        // Scenario 1: Diabetic Routine
        ScenarioCard(
            title = "Scenario A: Daily Diabetic Care Chain",
            icon = Icons.Default.Bloodtype,
            accentColor = Color(0xFFD32F2F),
            steps = listOf(
                "1. Take Insulin (Linked to 'Breakfast' breakfast anchor)" to "Baseline alert scheduled around standard breakfast.",
                "2. Post-Insulin Blood Glucose Test (30 mins AFTER Insulin Step)" to "Only fires after taking insulin, preventing premature painful fingerpricks.",
                "3. Rehydrating Lifestyle Walk (15 mins AFTERglucose check)" to "Fires exactly after blood stats are reviewed to maintain routine."
            ),
            scaleFont = scaleFont,
            bentoBorder = bentoBorder
        )

        // Scenario 2: Cardiac Lineup
        ScenarioCard(
            title = "Scenario B: Complex Cardiac Medication Timing",
            icon = Icons.Default.Favorite,
            accentColor = Color(0xFFE91E63),
            steps = listOf(
                "1. Blood Pressure Check (Morning Anchor, e.g. 08:00)" to "Measures standard resting cardiovascular state.",
                "2. Beta Blocker Dosage (10 mins AFTER Blood Pressure check)" to "Fires only when baseline test is confirmed."
            ),
            scaleFont = scaleFont,
            bentoBorder = bentoBorder
        )

        // Scenario 3: Pre-Meal Intake link
        ScenarioCard(
            title = "Scenario C: Pre-meal Stomach Settler",
            icon = Icons.Default.Restaurant,
            accentColor = Color(0xFFEF6C00),
            steps = listOf(
                "1. Drink Gastric Settler Water Check (30 mins BEFORE Lunch anchor)" to "Fires 30 minutes preceding lunch to prepare digestive tract."
            ),
            scaleFont = scaleFont,
            bentoBorder = bentoBorder
        )
    }
}

@Composable
fun ScenarioCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    steps: List<Pair<String, String>>,
    scaleFont: (Int) -> Float,
    bentoBorder: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, bentoBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = scaleFont(13).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Divider(color = bentoBorder.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { (header, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = header,
                                fontSize = scaleFont(11).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = desc,
                                fontSize = scaleFont(10).sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideFieldDecoderSection(
    scaleFont: (Int) -> Float,
    highContrastPref: Boolean,
    bentoBorder: Color,
    bentoPurple: Color,
    bentoLightPurple: Color,
    bentoDarkText: Color
) {
    val bentoGrayBg = if (highContrastPref) Color.White else Color(0xFFF3EDF7)
    var expandedField by remember { mutableStateOf<Int?>(null) }

    val fields = listOf(
        Triple(
            "Reminder Name / Routine Title",
            "This identifies what medication, dose, or care activity list to execute.",
            "Example: 'Metformin 500mg' or 'Check Blood Sugar Levels'"
        ),
        Triple(
            "Category",
            "Tags the task for filtered views. Categories include: Medical, Meal, Lifestyle, Social, and Kids.",
            "Example: 'Medical' displays in dark purple; 'Meal' has standard plate food icons."
        ),
        Triple(
            "Is this the starting step of a chain?",
            "If set to YES, this is an independent task. If NO, it is a dependent task which waits on a parent step to be completed first.",
            "Choose YES for base pill anchor doses. Choose NO to trigger a sequence."
        ),
        Triple(
            "Select Prerequisite Parent Step",
            "Only shown if Starting Step is set to NO. Allows you to choose which task must trigger first.",
            "Example: Select 'Take Bio-insulin' so this step follows it."
        ),
        Triple(
            "Anchor Event / Fixed Time",
            "Defines the primary temporal reference point: Breakfast, Lunch, Dinner, Morning, Evening, or a Fixed absolute time (e.g. 14:00).",
            "Meal anchors sync automatically with actual standard hours."
        ),
        Triple(
            "Offset Minutes & Direction",
            "Sets the delay relative to the Anchor or Parent: 'Before', 'After', or 'Fixed' (exact time).",
            "Example: '30 Minutes' 'After' means the alarm sounds exactly 30 mins after parent is checked off."
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Form Parameter Decoder",
            fontSize = scaleFont(15).sp,
            fontWeight = FontWeight.Bold,
            color = bentoDarkText
        )
        Text(
            text = "Tap any field to understand exactly what values to input during setup:",
            fontSize = scaleFont(11).sp,
            color = Color.Gray
        )

        fields.forEachIndexed { index, field ->
            val isExpanded = expandedField == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedField = if (isExpanded) null else index },
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) bentoLightPurple.copy(alpha = 0.5f) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, bentoBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = bentoPurple
                            )
                            Text(
                                text = field.first,
                                fontSize = scaleFont(12).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = field.second,
                            fontSize = scaleFont(11).sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(bentoGrayBg)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = field.third,
                                fontSize = scaleFont(10).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = bentoPurple
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideFaqSection(
    scaleFont: (Int) -> Float,
    highContrastPref: Boolean,
    bentoBorder: Color,
    bentoPurple: Color,
    bentoLightPurple: Color
) {
    val faqs = listOf(
        "How do pre-created templates work?" to "Templates allow one-tap generation of medical adherence routines. Selecting arecipient template (like Diabetic Morning Pack) automatically seeds several beautifully configured sequential links in your local database.",
        "What happens if I miss the baseline step?" to "If you mark a parent step as SKIPPED, the children steps will notify you that the chain broke, allowing you to manually override, reset, or log them appropriately to preserve safety records.",
        "Does the system require internet access?" to "No! MemoCare stores your healthcare schedules securely offline within Android's room database. Alarms fire locally via exact Broadcast Receivers and do not depend on external servers."
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Frequently Asked Questions",
            fontSize = scaleFont(15).sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        faqs.forEach { (q, a) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, bentoBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Q: $q",
                        fontSize = scaleFont(12).sp,
                        fontWeight = FontWeight.Bold,
                        color = bentoPurple
                    )
                    Text(
                        text = a,
                        fontSize = scaleFont(11).sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
