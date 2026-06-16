package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.UpgradeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PromptVaultViewModel

sealed class AppScreen {
    object Landing : AppScreen()
    data class Auth(val isSignUp: Boolean) : AppScreen()
    object Dashboard : AppScreen()
    data class Pricing(val fromDashboard: Boolean) : AppScreen()
    object Upgrade : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PromptVaultViewModel = viewModel()
            val highContrast by viewModel.highContrast.collectAsState()

            val toastMessage by viewModel.toastMessage.collectAsState()

            MyApplicationTheme(highContrast = highContrast) {
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Landing) }
                val currentUserEmail by viewModel.currentUserEmail.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        BoxWithScreen(
                            currentScreen = currentScreen,
                            viewModel = viewModel,
                            currentUserEmail = currentUserEmail,
                            onNavigateToAuth = { isSignUp ->
                                currentScreen = AppScreen.Auth(isSignUp)
                            },
                            onNavigateToPricing = { fromDashboard ->
                                currentScreen = AppScreen.Pricing(fromDashboard)
                            },
                            onNavigateToUpgrade = {
                                currentScreen = AppScreen.Upgrade
                            },
                            onLaunchDashboard = {
                                currentScreen = AppScreen.Dashboard
                            },
                            onNavigateBackToHome = {
                                currentScreen = AppScreen.Landing
                            },
                            modifier = Modifier.padding(innerPadding)
                        )

                        // Beautiful Global Animated Toast Notification
                        AnimatedVisibility(
                            visible = toastMessage != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .statusBarsPadding()
                                .zIndex(99f)
                        ) {
                            toastMessage?.let { msg ->
                                GlobalToastNotification(
                                    message = msg,
                                    onDismiss = { viewModel.clearToast() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxWithScreen(
    currentScreen: AppScreen,
    viewModel: PromptVaultViewModel,
    currentUserEmail: String?,
    onNavigateToAuth: (isSignUp: Boolean) -> Unit,
    onNavigateToPricing: (fromDashboard: Boolean) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onLaunchDashboard: () -> Unit,
    onNavigateBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentScreen) {
        is AppScreen.Landing -> {
            LandingScreen(
                viewModel = viewModel,
                onNavigateToAuth = onNavigateToAuth,
                onNavigateToPricing = { onNavigateToPricing(false) },
                onLaunchDashboard = onLaunchDashboard,
                isLoggedIn = currentUserEmail != null
            )
        }
        is AppScreen.Auth -> {
            AuthScreen(
                viewModel = viewModel,
                initialIsSignUp = currentScreen.isSignUp,
                onAuthSuccess = {
                    onLaunchDashboard()
                },
                onNavigateBack = onNavigateBackToHome
            )
        }
        is AppScreen.Dashboard -> {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToPricing = { onNavigateToPricing(true) },
                onNavigateToUpgrade = onNavigateToUpgrade,
                onNavigateBackToHome = onNavigateBackToHome
            )
        }
        is AppScreen.Pricing -> {
            PricingScreen(
                viewModel = viewModel,
                fromDashboard = currentScreen.fromDashboard,
                onNavigateBack = {
                    if (currentScreen.fromDashboard) {
                        onLaunchDashboard()
                    } else {
                        onNavigateBackToHome()
                    }
                },
                onAuthRequired = { isSignUp ->
                     onNavigateToAuth(isSignUp)
                },
                onLaunchDashboard = onLaunchDashboard
            )
        }
        is AppScreen.Upgrade -> {
            UpgradeScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    onLaunchDashboard()
                }
            )
        }
    }
}

@Composable
fun GlobalToastNotification(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clickable { onDismiss() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xED0D0B14) // Beautiful twilight translucent card bg
        ),
        border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)) // Neon purple accent border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success Icon",
                    tint = Color(0xFFC084FC), // Lavender purple icon tint
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ACTION COMPLETED",
                    color = Color(0xFFC084FC).copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            }

            Text(
                text = "DISMISS",
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(4.dp)
            )
        }
    }
}
