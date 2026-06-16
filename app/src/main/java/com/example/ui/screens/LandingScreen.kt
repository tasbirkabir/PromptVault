package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.theme.*
import com.example.ui.viewmodel.PromptVaultViewModel
import com.example.data.PromptEntity
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

// Premium aesthetic design system variables (luxury violet-blue theme)
private val AmbientDeepBg = Color(0xFF07050A)
private val DarkGlassBg = Color(0xFF0F0B18).copy(alpha = 0.75f)
private val LightGlassBorder = Color(0xFFC084FC).copy(alpha = 0.15f)
private val NeonPurpleTheme = Color(0xFF8B5CF6)
private val NeonPurpleActive = Color(0xFF7C3AED)
private val ElectricCyanAccent = Color(0xFF0EA5E9)

enum class LandingTab {
    HOME, FEATURES, DASHBOARD, SETTINGS
}

@Composable
fun LandingScreen(
    viewModel: PromptVaultViewModel,
    onNavigateToAuth: (isSignUp: Boolean) -> Unit,
    onNavigateToPricing: () -> Unit,
    onLaunchDashboard: () -> Unit,
    isLoggedIn: Boolean
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(LandingTab.HOME) }

    Scaffold(
        containerColor = AmbientDeepBg,
        topBar = {
            LandingAppBar(
                isLoggedIn = isLoggedIn,
                currentTab = currentTab,
                onProfileClick = {
                    currentTab = LandingTab.SETTINGS
                },
                onNavigateToAuth = onNavigateToAuth
            )
        },
        bottomBar = {
            LandingBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        // Premium ambient neon background with top glow light arc
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .drawBehind {
                    // Soft, diffuse cosmic radial glows
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPurpleTheme.copy(alpha = 0.2f), Color.Transparent),
                            radius = size.width * 0.8f
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.1f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricCyanAccent.copy(alpha = 0.15f), Color.Transparent),
                            radius = size.width * 0.7f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.2f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPurpleTheme.copy(alpha = 0.1f), Color.Transparent),
                            radius = size.width * 0.9f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.5f)
                    )

                    // Thin glowing light arc across the top section
                    val arcHeight = size.height * 0.12f
                    val arcWidth = size.width * 2.0f
                    val arcX = -size.width * 0.5f
                    val arcY = -size.height * 0.08f

                    drawArc(
                        color = NeonPurpleTheme.copy(alpha = 0.22f),
                        startAngle = 10f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(arcX, arcY - 3f),
                        size = Size(arcWidth, arcHeight),
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = Color(0xFFD8B4FE).copy(alpha = 0.7f),
                        startAngle = 10f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(arcX, arcY),
                        size = Size(arcWidth, arcHeight),
                        style = Stroke(width = 2.0f, cap = StrokeCap.Round)
                    )
                }
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "ScreenTransitions"
            ) { tab ->
                when (tab) {
                    LandingTab.HOME -> {
                        HomeTabContent(
                            onLaunch = {
                                if (isLoggedIn) onLaunchDashboard() else onNavigateToAuth(false)
                            },
                            onNavigateToPricing = onNavigateToPricing,
                            onTabSelected = { currentTab = it }
                        )
                    }
                    LandingTab.FEATURES -> {
                        FeaturesTabContent()
                    }
                    LandingTab.DASHBOARD -> {
                        DashboardTabContent(
                            viewModel = viewModel,
                            onLaunchPromptVault = onLaunchDashboard
                        )
                    }
                    LandingTab.SETTINGS -> {
                        SettingsTabContent(
                            viewModel = viewModel,
                            isLoggedIn = isLoggedIn,
                            onNavigateToAuth = onNavigateToAuth,
                            onNavigateToPricing = onNavigateToPricing
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LandingAppBar(
    isLoggedIn: Boolean,
    currentTab: LandingTab,
    onProfileClick: () -> Unit,
    onNavigateToAuth: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmbientDeepBg.copy(alpha = 0.85f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawBehind {
                // Subtle separator line under App Bar
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity with modern graphic asset style
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = "https://i.imgur.com/kt8KWwR.png",
                contentDescription = "PromptVault Logo",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "PromptVault",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = (-0.5).sp
            )
        }

        // Active screen indicator label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = when (currentTab) {
                    LandingTab.HOME -> "/"
                    LandingTab.FEATURES -> "/features"
                    LandingTab.DASHBOARD -> "/dashboard"
                    LandingTab.SETTINGS -> "/settings"
                },
                color = NeonPurpleTheme,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // Action button for account state / user avatar
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, NeonPurpleTheme.copy(alpha = 0.25f), CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoggedIn) {
                Text(
                    text = "P",
                    color = Color(0xFFC084FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Guest user",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LandingBottomBar(
    currentTab: LandingTab,
    onTabSelected: (LandingTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF09060E),
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .drawBehind {
                // Purple edge lighting line on top of Bottom Bar
                drawLine(
                    color = Color(0xFFC084FC).copy(alpha = 0.12f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
    ) {
        NavigationBarItem(
            selected = currentTab == LandingTab.HOME,
            onClick = { onTabSelected(LandingTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LandingTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                indicatorColor = NeonPurpleTheme.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentTab == LandingTab.FEATURES,
            onClick = { onTabSelected(LandingTab.FEATURES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LandingTab.FEATURES) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = "Features"
                )
            },
            label = { Text("Features", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                indicatorColor = NeonPurpleTheme.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentTab == LandingTab.DASHBOARD,
            onClick = { onTabSelected(LandingTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LandingTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "SaaS Live"
                )
            },
            label = { Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                indicatorColor = NeonPurpleTheme.copy(alpha = 0.25f)
            )
        )

        NavigationBarItem(
            selected = currentTab == LandingTab.SETTINGS,
            onClick = { onTabSelected(LandingTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LandingTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                indicatorColor = NeonPurpleTheme.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
fun HomeTabContent(
    onLaunch: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onTabSelected: (LandingTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Hero Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing neon workflow pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonPurpleTheme.copy(alpha = 0.12f))
                    .border(1.dp, NeonPurpleTheme.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = Color(0xFFD8B4FE),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "100% Native Mobile Architect",
                        color = Color(0xFFD8B4FE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mobile responsive auto-scaling typography
            Text(
                text = "Save & Organize Your Best AI Prompts in One Place",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                lineHeight = 31.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle text for mobile viewport
            Text(
                text = "A dark-mode private vault built to store, search, and instantly copy your top engineering prompts with built-in version control.",
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stacked CTA actions layout (100% mobile design guideline)
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleActive),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Perfect 48dp+ tap targets
                    .testTag("landing_signup_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Get Started (Free)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowRightAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onTabSelected(LandingTab.DASHBOARD) },
                border = BorderStroke(1.2.dp, NeonPurpleTheme.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Interactive Live Demo",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = NeonPurpleTheme,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Sneak peek card of live SaaS platform
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .glassmorphicContainer(RoundedCornerShape(16.dp))
                .clickable { onTabSelected(LandingTab.DASHBOARD) }
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonPurpleTheme.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = Color(0xFFD8B4FE),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Explore Live Sandbox Prompts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Observe real-time database prompts, instantly inject test items, and track total counts secure in the dev sandbox.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trusted-By Banner
        TrustedByLogosBanner()

        Spacer(modifier = Modifier.height(32.dp))

        // Landing page final footer CTA section
        LandingFooterCtaSection(
            onLaunch = onLaunch,
            onNavigateToPricing = onNavigateToPricing
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Compact pricing preview module
        PricingCompactSection(onNavigateToPricing)

        Spacer(modifier = Modifier.height(16.dp))

        // Frequently asked questions
        FaqSection()

        // Tiny footer signature
        LandingFooter()
    }
}

@Composable
fun FeaturesTabContent() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Grid Title and low-code highlights
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(16.dp, 1.dp).background(NeonPurpleTheme))
                Text(
                    text = "SWIPEABLE METRICS",
                    color = NeonPurpleTheme,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Box(modifier = Modifier.size(16.dp, 1.dp).background(NeonPurpleTheme))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Low-Code Mobile Processes",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Harnessing optimized backend microservices with client-side PostgreSQL row state isolations.",
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Premium Horizontally swipeable card carousel component as requested
        Text(
            text = "⚡ FEATURED CORE SOLUTIONS (Swipe)",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal card 1
            CarouselItemCard(
                icon = Icons.Outlined.Lock,
                title = "Private Vault",
                accentColor = NeonPurpleTheme,
                stat = "Supabase RLS"
            )

            // Horizontal card 2
            CarouselItemCard(
                icon = Icons.Outlined.ContentCopy,
                title = "Instant Copy",
                accentColor = ElectricCyanAccent,
                stat = "Global Search"
            )

            // Horizontal card 3
            CarouselItemCard(
                icon = Icons.Outlined.Category,
                title = "Smart Buckets",
                accentColor = Color(0xFFFCD34D),
                stat = "Marketing & Dev"
            )

            // Horizontal card 4
            CarouselItemCard(
                icon = Icons.Outlined.Groups,
                title = "Team Workspaces",
                accentColor = Color(0xFF34D399),
                stat = "Version Control"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vertical stack of Features (re-architected for responsive mobile lists)
        Text(
            text = "📋 DETAILED PRODUCT OUTLINE",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1
            MobileFeatureListItem(
                icon = Icons.Outlined.Lock,
                title = "Private Prompt Vault",
                description = "Secure storage isolated safely by Supabase Row Level Security (RLS)."
            )

            // Card 2
            MobileFeatureListItem(
                icon = Icons.Outlined.ContentCopy,
                title = "Instant Copy & Filter",
                description = "Fast global search, filtering by tags, and quick-copy buttons for production workflows."
            )

            // Card 3
            MobileFeatureListItem(
                icon = Icons.Outlined.Category,
                title = "Smart Categorization",
                description = "Default organized buckets for Marketing, SEO, Copywriting, Sales, Email, Coding, and General."
            )

            // Card 4
            MobileFeatureListItem(
                icon = Icons.Outlined.Groups,
                title = "Team Workspaces",
                description = "Collaborative prompt folders with automated prompt text versioning."
            )
        }
    }
}

@Composable
fun CarouselItemCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    stat: String
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .glassmorphicContainer(RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(stat, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )

            Text(
                text = "Premium responsive visual layout leveraging custom vector parameters & dynamic values.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun MobileFeatureListItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicContainer(RoundedCornerShape(14.dp), borderOpacity = 0.5f)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonPurpleTheme.copy(alpha = 0.12f))
                    .border(1.dp, NeonPurpleTheme.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD8B4FE),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun DashboardTabContent(
    viewModel: PromptVaultViewModel,
    onLaunchPromptVault: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time Database state collection
    val prompts by viewModel.filteredPrompts.collectAsState(emptyList())
    val favorites by viewModel.favoritePrompts.collectAsState(emptyList())
    val workspacesList by viewModel.workspaces.collectAsState(emptyList())

    // Input fields for Custom Sandbox Prompt Injector
    var simTitle by remember { mutableStateOf("") }
    var simContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Welcoming Card Info Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back, Developer! 👩‍💻",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Database Sandbox (Real-Time SQLite/Room Synced)",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                    Text("SECURE GATE", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expanded Chart Container (Full-Width Mobile viewport alignment)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicContainer(RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonPurpleTheme))
                        Text(
                            text = "Prompt Sync & Search Volume",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Live Sandbox",
                            color = NeonPurpleTheme,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High fidelity bar chart with glow highlights
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = listOf(0.35f, 0.55f, 0.44f, 0.7f, 0.95f, 0.62f, 0.5f)
                    bars.forEachIndexed { i, factor ->
                        val isHighlighted = i == 4 // Key indicator matching the montage arc

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (isHighlighted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFCD34D))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+42 syncs",
                                        color = Color.Black,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(factor)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isHighlighted) {
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFC084FC), NeonPurpleActive)
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.01f))
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Text(
                            text = day,
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive dynamic vault card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicContainer(RoundedCornerShape(16.dp), borderOpacity = 1.1f)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROMPTVAULT SECURE DEPOSITORY",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonPurpleTheme,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Real Database Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOTAL PROMPTS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${prompts.size}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STARRED FAVORITES",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${favorites.size}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WORKSPACES",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${workspacesList.size.coerceAtLeast(1)}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local SQLite Isolation Stack",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Quick simulation interactive button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                viewModel.addPrompt(
                                    title = "Sandbox Template #${prompts.size + 1}",
                                    description = "Fast sandbox-injected template",
                                    content = "Injected system configuration prompt for testing security and copy workflows securely...",
                                    category = "General",
                                    workspaceId = 0
                                )
                                Toast.makeText(context, "SaaS Prompt Injected successfully! ✨", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            Text("INJECT TEMPLATE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Sandbox Prompt Injector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicContainer(RoundedCornerShape(16.dp), borderOpacity = 0.3f)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🛠️ QUICK SANDBOX PROMPT INJECTOR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )

                // Input fields
                OutlinedTextField(
                    value = simTitle,
                    onValueChange = { simTitle = it },
                    label = { Text("Prompt Title (e.g. SEO Optimizer)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = NeonPurpleTheme,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = simContent,
                    onValueChange = { simContent = it },
                    label = { Text("Prompt Instruction Content", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = false,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = NeonPurpleTheme,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (simTitle.isNotBlank() && simContent.isNotBlank()) {
                            viewModel.addPrompt(
                                title = simTitle,
                                description = "Injected from custom sandbox input",
                                content = simContent,
                                category = "General",
                                workspaceId = 0
                            )
                            simTitle = ""
                            simContent = ""
                            Toast.makeText(context, "SaaS Prompt Injected successfully! ✨", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter both details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleActive),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Inject Custom Prompt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent SQL/Room Database Prompt Feed logs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROMPTVAULT ACTIVE DATABASE FEED",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${prompts.size} Active",
                color = NeonPurpleTheme,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (prompts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No prompts in SQLite database.\nClick Inject to populate with mock data!",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            } else {
                prompts.take(4).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = NeonPurpleTheme,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = prompt.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Category: ${prompt.category}",
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            Text(
                                text = "${prompt.content.length} chars",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Open full Prompt Vault workspace action button at base
        Button(
            onClick = onLaunchPromptVault,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleTheme),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Open PromptVault Workspaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    viewModel: PromptVaultViewModel,
    isLoggedIn: Boolean,
    onNavigateToAuth: (Boolean) -> Unit,
    onNavigateToPricing: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val highContrast by viewModel.highContrast.collectAsState()
    val hapticSwipeToDelete by viewModel.hapticSwipeToDelete.collectAsState()
    val hapticCopyToClipboard by viewModel.hapticCopyToClipboard.collectAsState()

    // Toggle states for mobile settings toggler
    var gpuRendering by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(false) }
    var biometricLock by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "PREFERENCES & SYSTEM",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom sandbox status layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicContainer(RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonPurpleTheme.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = NeonPurpleTheme)
                    }
                    Column {
                        Text(
                            text = if (isLoggedIn) "PromptVault Client Verified" else "Developer Local Profile",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isLoggedIn) "Authenticated Vault User" else "Unauthorized Anonymous Guest",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.sp
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))

                if (isLoggedIn) {
                    Button(
                        onClick = {
                            onNavigateToAuth(false) // Logs out or manages session
                            Toast.makeText(context, "Simulated Log Out successful!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect Account Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { onNavigateToAuth(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleActive),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Cloud Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Settings Checklist
        Text(
            text = "HARDWARE & PERFORMANCE",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // High Contrast Option
            SettingsToggleRow(
                icon = Icons.Outlined.Contrast,
                title = "High Contrast Mode",
                subtitle = "Forces pure black background variant with maximum contrast for readability.",
                checked = highContrast,
                onCheckedChange = { viewModel.setHighContrast(it) }
            )

            // Option 1
            SettingsToggleRow(
                icon = Icons.Outlined.Speed,
                title = "High Performance GPU Rendering",
                subtitle = "Enables hardware canvas acceleration for animations.",
                checked = gpuRendering,
                onCheckedChange = { gpuRendering = it }
            )

            // Option 2
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                subtitle = "Recieve instant PostgreSQL server synchronization logs.",
                checked = pushNotifications,
                onCheckedChange = { pushNotifications = it }
            )

            // Option 3
            SettingsToggleRow(
                icon = Icons.Outlined.Fingerprint,
                title = "Biometric Lock",
                subtitle = "Require touch ID keys when unlocking the prompt dashboard.",
                checked = biometricLock,
                onCheckedChange = { biometricLock = it }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Haptic Feedback Settings Section
        Text(
            text = "HAPTIC VIBRATION FEEDBACK",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsToggleRow(
                icon = Icons.Outlined.TouchApp,
                title = "Swipe to Delete Vibration",
                subtitle = "Trigger tactile vibration feedback upon swiping to delete prompt card items.",
                checked = hapticSwipeToDelete,
                onCheckedChange = { viewModel.setHapticSwipeToDelete(it) }
            )

            SettingsToggleRow(
                icon = Icons.Outlined.ContentCopy,
                title = "Copy to Clipboard Vibration",
                subtitle = "Trigger tactile vibration feedback when a prompt string snippet is copied.",
                checked = hapticCopyToClipboard,
                onCheckedChange = { viewModel.setHapticCopyToClipboard(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subscription overview linkage
        PricingCompactSection(onNavigateToPricing)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonPurpleTheme,
                    modifier = Modifier.size(18.dp)
                )

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonPurpleTheme,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.scale(0.8f) // Compact responsive standard
            )
        }
    }
}

@Composable
fun TrustedByLogosBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TRUSTED BY 10,000+ DEVELOPERS GLOBALLY",
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Grayscale premium placeholder logos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("React", "Next.js", "Supabase", "GitLab", "Vercel").forEach { company ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.alpha(0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterVintage,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = company,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
fun LandingFooterCtaSection(
    onLaunch: () -> Unit,
    onNavigateToPricing: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF221142), Color(0xFF0F0824))
                )
            )
            .border(1.dp, NeonPurpleTheme.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Ready to elevate your workflow?",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Join over 10,000 developers managing database variables securely with PostgreSQL row policies.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleActive),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Get Started", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onNavigateToPricing,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Details", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PricingCompactSection(onNavigateToPricing: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FLEXIBLE SUBSCRIPTION PLANS",
            color = NeonPurpleTheme,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Differentiated benefit tiers for serious users",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .clickable { onNavigateToPricing() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Free Hobby",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "vs",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Premium Pro ⚡",
                            color = Color(0xFFFCD34D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Unlimited Postgres storage, Supabase Collaborative Sockets, and detailed metrics.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = null,
                    tint = NeonPurpleTheme,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FaqSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "FAQ",
            color = NeonPurpleTheme,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Frequently Asked Questions",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val faqs = listOf(
            Pair("How does security isolation operate?", "We run row-level security (RLS) policies matching authenticated tokens, separating workspace directories completely."),
            Pair("What features are in Premium Pro?", "Continuous cloud sync, unlimited automated version history backups, diagnostic analytical trends, and additional multiplayer member sockets.")
        )

        faqs.forEach { (q, a) ->
            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NeonPurpleTheme,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (expanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(a, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LandingFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ElectricCyanAccent))
            Text("Engineered by PromptVault Devs with Supabase Auth Protocols", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
        }
    }
}
