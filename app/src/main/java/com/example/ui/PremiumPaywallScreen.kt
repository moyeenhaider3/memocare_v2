package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = remember { BillingManager.getInstance(context) }
    
    val isSubscribed by billingManager.isSubscribed.collectAsState()
    val productDetails by billingManager.productDetails.collectAsState()

    // Determine if high-contrast is enabled in shared prefs for readability
    val db = remember { com.example.data.AppDatabase.getDatabase(context) }
    val repo = remember { com.example.data.ReminderRepository(db.reminderDao(), context) }
    val highContrastPref = remember { repo.getPrefBoolean("contrast_mode", false) }
    val textSizePref = remember { repo.getPrefString("font_size_mode", "Large") }

    fun scaleFont(baseSize: Int): Int {
        return when (textSizePref) {
            "Extra Large" -> baseSize + 6
            "Large" -> baseSize + 2
            "Medium" -> baseSize
            else -> baseSize
        }
    }

    // Modern Luxury visual colors
    val darkBackground = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val goldAccent = Color(0xFFFBBF24)
    val checkGreen = Color(0xFF10B981)

    val backColor = if (highContrastPref) Color.White else darkBackground
    val textColor = if (highContrastPref) Color.Black else Color.White
    val secondaryTextColor = if (highContrastPref) Color.DarkGray else Color(0xFF94A3B8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MemoCare Premium",
                        fontSize = scaleFont(18).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (highContrastPref) Color.White else Color(0xFF1E293B),
                    titleContentColor = textColor
                )
            )
        },
        containerColor = backColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backColor)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Hero Banner with Golden Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (highContrastPref) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(goldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Text(
                        text = "UNLOCK MEMOCARE GOLD",
                        fontWeight = FontWeight.Black,
                        fontSize = scaleFont(18).sp,
                        color = goldAccent,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = "The ultimate caregiving safety net. Streamline and coordinate routine support.",
                        fontSize = scaleFont(13).sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Benefits Grid / List
            Text(
                text = "✨ PREMIUM VALUE INCLUDED:",
                fontWeight = FontWeight.Bold,
                fontSize = scaleFont(13).sp,
                color = if (highContrastPref) Color.Black else goldAccent,
                modifier = Modifier.align(Alignment.Start)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BenefitRow(
                    icon = Icons.Default.Layers,
                    title = "Unlimited Chained Reminders",
                    description = "Deploy interconnected Care Chains (such as scheduling meals, medications, water, and rests in logical order automatically).",
                    iconColor = goldAccent,
                    textColor = textColor,
                    secColor = secondaryTextColor,
                    scaleFont = ::scaleFont
                )
                BenefitRow(
                    icon = Icons.Default.Fullscreen,
                    title = "Direct Full Screen Alert Takeovers",
                    description = "Alerts directly wake up and take over the mobile screen immediately, ensuring memory care responses are never missed.",
                    iconColor = goldAccent,
                    textColor = textColor,
                    secColor = secondaryTextColor,
                    scaleFont = ::scaleFont
                )
                BenefitRow(
                    icon = Icons.Default.SupervisedUserCircle,
                    title = "Caregiver Notifications",
                    description = "Sync status actions directly onto caregiver dashboards with priority state logging.",
                    iconColor = goldAccent,
                    textColor = textColor,
                    secColor = secondaryTextColor,
                    scaleFont = ::scaleFont
                )
                BenefitRow(
                    icon = Icons.Default.HighlightOff,
                    title = "100% Ad-Free Experience",
                    description = "Eliminate visual obstacles. Clear, clean interface optimized for simplicity and accessibility.",
                    iconColor = goldAccent,
                    textColor = textColor,
                    secColor = secondaryTextColor,
                    scaleFont = ::scaleFont
                )
            }

            // Plan Pricing Card Detail
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrastPref) Color.White else Color(0xFF1E293B)
                ),
                border = BorderStroke(
                    2.dp, 
                    if (isSubscribed) checkGreen else (if (highContrastPref) Color.Black else goldAccent)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Monthly Subscription",
                                fontWeight = FontWeight.Bold,
                                fontSize = scaleFont(16).sp,
                                color = textColor
                            )
                            Text(
                                "30 DAYS FREE TRIAL",
                                fontWeight = FontWeight.Black,
                                fontSize = scaleFont(11).sp,
                                color = if (isSubscribed) checkGreen else goldAccent
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$4.99/mo",
                                fontWeight = FontWeight.Black,
                                fontSize = scaleFont(18).sp,
                                color = textColor
                            )
                            Text(
                                "Cancels Anytime",
                                fontSize = scaleFont(11).sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    Divider(color = secondaryTextColor.copy(alpha = 0.3f))

                    Text(
                        text = "🔒 Billing Details:\nEnjoy unlimited Premium access entirely free of charge for your first 30 days! No charges will occur during your trial. After 30 days, Google Play billing will automatically charge $4.99/month on a monthly auto-renewing contract. Cancel easily at least 24 hours before your trial of the subscription ends to avoid future cycles.",
                        fontSize = scaleFont(11).sp,
                        color = secondaryTextColor,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // EXPLICIT STEP BY STEP USER INSTRUCTIONS ON SCREEN
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrastPref) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, if (highContrastPref) Color(0xFFCBD5E1) else Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ℹ️ HOW PAYMENTS WORK - STEP-BY-STEP GUILD:",
                        fontWeight = FontWeight.Black,
                        fontSize = scaleFont(11).sp,
                        color = if (highContrastPref) Color(0xFF475569) else Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )

                    InstructionStep(
                        step = "1",
                        title = "Start Your Trial Below",
                        desc = "Tap 'Start 30-Day Free Trial' below. This securely connects to external Google Play Billing API. You will be asked to confirm your payment instrument, but your card balance is not debited today.",
                        textColor = textColor,
                        secColor = secondaryTextColor,
                        scaleFont = ::scaleFont
                    )

                    InstructionStep(
                        step = "2",
                        title = "Enjoy Care Integration",
                        desc = "All gold level features (unlimited steps in order, reliable full-screen alerts, complete history logs) will instantly unlock. Test them fully without limits.",
                        textColor = textColor,
                        secColor = secondaryTextColor,
                        scaleFont = ::scaleFont
                    )

                    InstructionStep(
                        step = "3",
                        title = "Auto-Renew or Free Cancel",
                        desc = "Love the experience? Keep it active; the plan auto-renews at $4.99/month starting day 31. Want to stop? Open the Google Play Store, navigate to Subscriptions -> MemoCare, and click 'Cancel Subscription' at any time. Totally risk free.",
                        textColor = textColor,
                        secColor = secondaryTextColor,
                        scaleFont = ::scaleFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CTA Button Trigger
            if (isSubscribed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = checkGreen.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, checkGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Subscribed badge",
                            tint = checkGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✨ YOU'RE A PREMIUM MEMBER!",
                            color = if (highContrastPref) Color(0xFF065F46) else Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = scaleFont(14).sp
                        )
                    }
                }

                Button(
                    onClick = { billingManager.updateSubscriptionState(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Deactivate / Reset Premium Status (Dev Sandbox Mode)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = {
                        if (activity != null) {
                            billingManager.purchaseSubscription(activity) {
                                Toast.makeText(context, "Premium billing simulator enabled!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Activity state error.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrastPref) Color.Black else goldAccent,
                        contentColor = if (highContrastPref) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (highContrastPref) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "START 30-DAY FREE TRIAL",
                        fontWeight = FontWeight.Black,
                        fontSize = scaleFont(14).sp,
                        letterSpacing = 0.5.sp
                    )
                }

                TextButton(
                    onClick = {
                        // Secret developer unlock in trial
                        billingManager.updateSubscriptionState(true)
                        Toast.makeText(context, "Premium status recovered!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        "Restore Purchases / Standard Simulation",
                        color = secondaryTextColor,
                        fontSize = scaleFont(11).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun BenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    textColor: Color,
    secColor: Color,
    scaleFont: (Int) -> Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = scaleFont(13).sp,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                fontSize = scaleFont(11).sp,
                color = secColor,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun InstructionStep(
    step: String,
    title: String,
    desc: String,
    textColor: Color,
    secColor: Color,
    scaleFont: (Int) -> Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontWeight = FontWeight.Bold,
                fontSize = scaleFont(10).sp,
                color = textColor
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = scaleFont(12).sp,
                color = textColor
            )
            Text(
                desc,
                fontSize = scaleFont(10).sp,
                color = secColor,
                lineHeight = 14.sp
            )
        }
    }
}
