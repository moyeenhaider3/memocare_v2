package com.example

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import com.example.ui.DashboardScreen
import com.example.ui.HistoryScreen
import com.example.ui.ManualSetupScreen
import com.example.ui.OnboardingScreen
import com.example.ui.SettingsScreen
import com.example.ui.TemplateLibraryScreen
import com.example.ui.PremiumPaywallScreen
import com.example.ui.MemoCareGuideScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Ask for runtime notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val context = this
            val db = remember { AppDatabase.getDatabase(context) }
            val repo = remember { ReminderRepository(db.reminderDao(), context) }

            // Dynamic User Style states across screens
            var textSizeState by remember { mutableStateOf("Large") }
            var highContrastState by remember { mutableStateOf(false) }

            // Sync States with offline SharedPreferences on startup
            LaunchedEffect(Unit) {
                textSizeState = repo.getPrefString("text_size", "Large")
                highContrastState = repo.getPrefBoolean("contrast_mode", false)
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        // 0. Initial Router splash
                        composable("splash") {
                            LaunchedEffect(Unit) {
                                val complete = repo.getPrefBoolean("onboarding_complete", false)
                                if (complete) {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize())
                        }

                        // 1. Onboarding Flow Routing
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinished = {
                                    repo.editPrefs { putBoolean("onboarding_complete", true) }
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                },
                                textSizePref = textSizeState,
                                onTextSizeChange = { newSize ->
                                    textSizeState = newSize
                                    repo.editPrefs { putString("text_size", newSize) }
                                },
                                highContrastPref = highContrastState,
                                onHighContrastChange = { boldColor ->
                                    highContrastState = boldColor
                                    repo.editPrefs { putBoolean("contrast_mode", boldColor) }
                                }
                            )
                        }

                        // 2. Main Landing Screen Routing
                        composable("dashboard") {
                            DashboardScreen(
                                textSizePref = textSizeState,
                                highContrastPref = highContrastState,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToTemplates = { navController.navigate("templates") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToAdd = { navController.navigate("manual_setup") },
                                onNavigateToEdit = { id -> navController.navigate("manual_setup?editingId=$id") },
                                onNavigateToPaywall = { navController.navigate("paywall") },
                                onNavigateToGuide = { navController.navigate("user_guide") }
                            )
                        }

                        // 3. Manual Connection & Offset Form
                        composable(
                            route = "manual_setup?editingId={editingId}",
                            arguments = listOf(
                                navArgument("editingId") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val editingId = backStackEntry.arguments?.getString("editingId")?.toIntOrNull()
                            ManualSetupScreen(
                                editingReminderId = editingId,
                                textSizePref = textSizeState,
                                highContrastPref = highContrastState,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToGuide = { navController.navigate("user_guide") }
                            )
                        }

                        // 4. Template Setup Library
                        composable("templates") {
                            TemplateLibraryScreen(
                                textSizePref = textSizeState,
                                highContrastPref = highContrastState,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 5. Compliance logs & Stats History
                        composable("history") {
                            HistoryScreen(
                                textSizePref = textSizeState,
                                highContrastPref = highContrastState,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. User Preferences & Meal config Settings
                        composable("settings") {
                            SettingsScreen(
                                textSizePref = textSizeState,
                                onTextSizeChange = { sz ->
                                    textSizeState = sz
                                    repo.editPrefs { putString("text_size", sz) }
                                },
                                highContrastPref = highContrastState,
                                onHighContrastChange = { hc ->
                                    highContrastState = hc
                                    repo.editPrefs { putBoolean("contrast_mode", hc) }
                                },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPaywall = { navController.navigate("paywall") }
                            )
                        }

                        // 7. Premium Subscription Paywall Screen
                        composable("paywall") {
                            PremiumPaywallScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 8. Caregiver & Setup User Manual
                        composable("user_guide") {
                            MemoCareGuideScreen(
                                textSizePref = textSizeState,
                                highContrastPref = highContrastState,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
