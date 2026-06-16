package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.glassmorphicContainer
import com.example.ui.viewmodel.PromptVaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    viewModel: PromptVaultViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userTierState by viewModel.userTier.collectAsState()
    var activeFeatureTab by remember { mutableStateOf("coop") } // "coop" (Team Collaboration) or "ai" (Advanced AI Tools)

    // Layout configuration
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0E17),
            Color(0xFF07050C)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PREMIUM UPGRADE HUB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Upgrade to Pro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("upgrade_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to dashboard",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0E17),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0E17)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Active Subscription Tier Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicContainer(RoundedCornerShape(16.dp), borderOpacity = 1.0f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT ACCOUNT LEVEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (userTierState == "Premium") "Premium Pro Active 🚀" else "Free Account Tier",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userTierState == "Premium") EmeraldAccent else Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (userTierState == "Premium") EmeraldAccent.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (userTierState == "Premium") EmeraldAccent else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (userTierState == "Premium") "PRO TIER" else "FREE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userTierState == "Premium") EmeraldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Feature Switcher Sub-navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161320))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 1: Cooperation & Teams
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeFeatureTab == "coop") Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (activeFeatureTab == "coop") 1.dp else 0.dp,
                            color = if (activeFeatureTab == "coop") EmeraldAccent.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeFeatureTab = "coop" }
                        .padding(vertical = 10.dp)
                        .testTag("tab_cooperation"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = if (activeFeatureTab == "coop") Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Cooperation",
                            color = if (activeFeatureTab == "coop") Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab 2: AI & Compiler Tools
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeFeatureTab == "ai") Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (activeFeatureTab == "ai") 1.dp else 0.dp,
                            color = if (activeFeatureTab == "ai") EmeraldAccent.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeFeatureTab = "ai" }
                        .padding(vertical = 10.dp)
                        .testTag("tab_ai_tools"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (activeFeatureTab == "ai") Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Advanced AI",
                            color = if (activeFeatureTab == "ai") Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Standalone Interactive Showcase Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .glassmorphicContainer(RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeFeatureTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "feature_showcase"
                ) { tab ->
                    if (tab == "coop") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Canvas(modifier = Modifier.size(52.dp)) {
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(4.dp.toPx(), 8.dp.toPx())
                                    lineTo(12.dp.toPx(), 8.dp.toPx())
                                    lineTo(18.dp.toPx(), 14.dp.toPx())
                                    lineTo(48.dp.toPx(), 14.dp.toPx())
                                    lineTo(48.dp.toPx(), 44.dp.toPx())
                                    lineTo(4.dp.toPx(), 44.dp.toPx())
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                    style = Fill
                                )
                                drawPath(
                                    path = path,
                                    color = EmeraldAccent,
                                    style = Stroke(width = 1.8.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF10B981),
                                    radius = 6.dp.toPx(),
                                    center = Offset(36.dp.toPx(), 32.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = Offset(36.dp.toPx(), 32.dp.toPx()),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Instant Collaborative Directory Sharing",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Work smoothly beside your engineering team inside shared writeable folder nodes with customized secure role structures.",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Gemini 3 Ultra Active",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Deep Prompt Optimization Pipeline",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Automated high-context model analysis, recursive expansion of variables, and self-testing diagnostic compilers mapping CPU outputs.",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            // Billing Subscription & Package Details Screen Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upgrade_plan_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, Color(0xFF262135))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Premium Plan Architecture",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Line breaks benefits list
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val planBenefits = listOf(
                            "Instant Multi-user workspace capacity connects" to Icons.Default.Group,
                            "Prioritized ultra high-context model routing blocks" to Icons.Default.ElectricBolt,
                            "Automatic backup engine logging historical versions" to Icons.Default.History,
                            "Comprehensive metrics analysis & frequency trends" to Icons.Default.TrendingUp
                        )

                        for ((benefit, icon) in planBenefits) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = benefit,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    // Interactive Custom Premium Pro price checkout action block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$15",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "/ month",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.4e-1f).copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "Cancel subscription anytime",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.setTier("Premium")
                                viewModel.showToast("Successfully Upgraded to Premium Pro! 🚀")
                                Toast.makeText(context, "Welcome to Premium Pro! 🚀", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF7C3AED),
                                            Color(0xFFC084FC)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp),
                            enabled = userTierState != "Premium"
                        ) {
                            Text(
                                text = if (userTierState == "Premium") "Active Pro Tier" else "Upgrade Now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Downward downgrade / reset debug option
            if (userTierState == "Premium") {
                TextButton(
                    onClick = {
                        viewModel.setTier("Free")
                        viewModel.showToast("Switched Back to Free Tier ℹ️")
                        Toast.makeText(context, "Account reset to Free Tier", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Downgrade Account (Sandbox Mode)",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
