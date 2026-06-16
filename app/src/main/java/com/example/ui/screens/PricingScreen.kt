package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PromptVaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    viewModel: PromptVaultViewModel,
    fromDashboard: Boolean,
    onNavigateBack: () -> Unit,
    onAuthRequired: (isSignUp: Boolean) -> Unit,
    onLaunchDashboard: () -> Unit
) {
    val context = LocalContext.current
    val userTier by viewModel.userTier.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val scope = rememberCoroutineScope()

    var isAnnualBilling by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArtisticNavBg),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFE2B840),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "PromptVault Plans",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pricing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Hero Header Section
            PricingHeroHeader()

            // Billing Switcher
            BillingSwitcher(
                isAnnual = isAnnualBilling,
                onToggle = { isAnnualBilling = !isAnnualBilling }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Plans Side-by-Side or Stack Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hobby Free Card
                FreeHobbyPlanCard(
                    userTier = userTier,
                    onGetStarted = {
                        if (currentUserEmail != null) {
                            onLaunchDashboard()
                        } else {
                            onAuthRequired(true)
                        }
                    }
                )

                // Premium Pro Card
                PremiumProPlanCard(
                    isAnnual = isAnnualBilling,
                    userTier = userTier,
                    onUpgrade = {
                        if (currentUserEmail != null) {
                            showCheckoutDialog = true
                        } else {
                            Toast.makeText(context, "Please log in or sign up first!", Toast.LENGTH_SHORT).show()
                            onAuthRequired(true)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Perks Comparison Matrix Table
            DetailedComparisonMatrix()

            Spacer(modifier = Modifier.height(24.dp))

            // Money Back Guarantee Section
            RefundPolicySection()
        }
    }

    if (showCheckoutDialog) {
        CheckoutSimulationDialog(
            isAnnual = isAnnualBilling,
            onClose = { showCheckoutDialog = false },
            onPaymentSuccess = {
                viewModel.setTier("Premium")
                showCheckoutDialog = false
                Toast.makeText(context, "Payment Successful! Premium Pro unlocked ⚡", Toast.LENGTH_LONG).show()
                onLaunchDashboard()
            }
        )
    }
}

@Composable
fun PricingHeroHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE2B840).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFE2B840), RoundedCornerShape(50.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFFE2B840),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "ACCELERATE YOUR WORKFLOW",
                    color = Color(0xFFE2B840),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fine-tuned pricing for every AI workflow",
            textAlign = TextAlign.Center,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start with a safe hobby sandbox and upgrade anytime you are ready to scale with collaborative multiplayer environments & compiler pipelines.",
            textAlign = TextAlign.Center,
            color = CoolGrayMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun BillingSwitcher(
    isAnnual: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(ArtisticNavBg)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (!isAnnual) EmeraldAccent else Color.Transparent)
                    .clickable { if (isAnnual) onToggle() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Monthly",
                    color = if (!isAnnual) ObsidianBg else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isAnnual) EmeraldAccent else Color.Transparent)
                    .clickable { if (!isAnnual) onToggle() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Yearly Plan",
                        color = if (isAnnual) ObsidianBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAnnual) ObsidianBg else Color(0xFFE2B840))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-20%",
                            color = if (isAnnual) Color(0xFFE2B840) else ObsidianBg,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FreeHobbyPlanCard(
    userTier: String,
    onGetStarted: () -> Unit
) {
    val isCurrent = userTier == "Free"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(
                1.dp,
                if (isCurrent) EmeraldAccent.copy(alpha = 0.5f) else ArtisticBorder.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hobby Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "For individual experimenters",
                    color = CoolGrayMuted,
                    fontSize = 11.sp
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EmeraldAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "YOUR CURRENT TIER",
                        color = EmeraldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$0",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "/ forever",
                color = CoolGrayMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            "Local sandbox storage up to 25 items",
            "Sqlite client-side persistence layers",
            "Standard filters and sorting search",
            "Automatic history (capped at 3 steps)"
        ).forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = feature,
                    color = LightGrayText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrent) CardBackground else ObsidianBg,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isCurrent) EmeraldAccent.copy(alpha = 0.5f) else CoolGrayMuted.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().testTag("free_plan_cta")
        ) {
            Text(
                text = if (isCurrent) "Active Playground" else "Choose Free Hobby",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PremiumProPlanCard(
    isAnnual: Boolean,
    userTier: String,
    onUpgrade: () -> Unit
) {
    val isCurrent = userTier == "Premium"
    val price = if (isAnnual) 12 else 15

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF221F2B)) // Darker luxury shade
            .border(
                2.dp,
                Color(0xFFE2B840), // Premium gold border
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Premium Pro ⚡",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2B840))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BEST VALUE",
                            color = ObsidianBg,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = "For serious developers & professional teams",
                    color = CoolGrayMuted,
                    fontSize = 11.sp
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2B840).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ACTIVE PLAN",
                        color = Color(0xFFE2B840),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$$price",
                color = Color(0xFFE2B840),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "/ month",
                color = CoolGrayMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (isAnnual) {
            Text(
                text = "$144 billed annually (Saves $36)",
                color = Color(0xFFE2B840),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE2B840).copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            "Unlimited cloud prompts & categories",
            "Supabase collaborative team workspaces",
            "Unlimited automated rollback versions",
            "Prioritized AI Routing (Gemini 3 Ultra)",
            "Exclusive Compiler Loops & Opt Pipelines",
            "Workspace seat management checks"
        ).forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFE2B840),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = feature,
                    color = LightGrayText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onUpgrade,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE2B840),
                contentColor = ObsidianBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upgrade_to_premium_cta")
        ) {
            Text(
                text = if (isCurrent) "Manage Subscription" else "Upgrade to Premium Pro ⚡",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun DetailedComparisonMatrix() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "DETAILED BENEFIT MATRIX",
            color = EmeraldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Capability", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                    Text("Hobby", color = CoolGrayMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Pro", color = Color(0xFFE2B840), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }

                HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.5f))

                val matrixRows = listOf(
                    Triple("Saved Prompts Limit", "Up to 25", "Unlimited ⚡"),
                    Triple("Database Backend", "Local SQLite", "Cloud Postgre (Supabase) + RLS"),
                    Triple("Team Collaboration", "None", "Shared Folders (Admin, Edit, View)"),
                    Triple("Versioning Rollbacks", "Cap 3", "Infinite History 🔄"),
                    Triple("AI Model Speed", "Standard Cooldown", "Top-Priority Ultra Cluster"),
                    Triple("Automated Refactoring", "❌ Unsupported", "Included (SaaS Engine)"),
                    Triple("Diagnostic compiler loops", "❌ Unsupported", "Included (Loop Optimizer)"),
                    Triple("Workspace Capacity", "Local Only", "Socket Seat Allocation Control")
                )

                matrixRows.forEach { (feature, freeVal, proVal) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feature,
                            color = LightGrayText,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1.8f)
                        )
                        Text(
                            text = freeVal,
                            color = CoolGrayMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = proVal,
                            color = if (proVal.contains("Unlimited") || proVal.contains("Included")) Color(0xFFE2B840) else EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                    HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
fun RefundPolicySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = EmeraldAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "100% Risk-Free Guarantee",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try Premium Pro risk-free. If you are not fully satisfied with collaborative sockets or optimized compiler logs, request a full refund within 14 days.",
            color = CoolGrayMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSimulationDialog(
    isAnnual: Boolean,
    onClose: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val totalToPay = if (isAnnual) "$144/year" else "$15/month"
    val scope = rememberCoroutineScope()

    var cardholderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvvInput by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var simulationStep by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onClose() },
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2B840), RoundedCornerShape(24.dp))
            .testTag("checkout_simulation_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color(0xFFE2B840),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Secure Checkout",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFE2B840),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Processing Secure Payment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = simulationStep,
                            color = Color(0xFFE2B840),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                } else {
                    Text(
                        text = "You are subscribing to PromptVault Premium Pro. Price is $totalToPay.",
                        color = LightGrayText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Order Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Upgrade Tier", color = CoolGrayMuted, fontSize = 9.sp)
                                Text("Premium Pro ⚡", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(totalToPay, color = Color(0xFFE2B840), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = ErrorColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cardholder Input
                    TextField(
                        value = cardholderName,
                        onValueChange = { cardholderName = it },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
                        label = { Text("Cardholder Name", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ArtisticBorder,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg,
                            focusedLabelColor = EmeraldAccent,
                            unfocusedLabelColor = CoolGrayMuted
                        ),
                        singleLine = true
                    )

                    // Card Number
                    TextField(
                        value = cardNumber,
                        onValueChange = { input ->
                            // Simple digit filter
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 16) {
                                cardNumber = digits
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_card_input"),
                        label = { Text("Credit Card Number (16 Digits)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ArtisticBorder,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg,
                            focusedLabelColor = EmeraldAccent,
                            unfocusedLabelColor = CoolGrayMuted
                        ),
                        singleLine = true
                    )

                    // Code row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Expiry (MM/YY)
                        TextField(
                            value = expiryDate,
                            onValueChange = { input ->
                                if (input.length <= 5) {
                                    expiryDate = input
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("checkout_expiry_input"),
                            label = { Text("Expiry (MM/YY)", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = ArtisticBorder,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg,
                                focusedLabelColor = EmeraldAccent,
                                unfocusedLabelColor = CoolGrayMuted
                            ),
                            singleLine = true
                        )

                        // CVV
                        TextField(
                            value = cvvInput,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 4) {
                                    cvvInput = digits
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("checkout_cvv_input"),
                            label = { Text("CVC/CVV", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = ArtisticBorder,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg,
                                focusedLabelColor = EmeraldAccent,
                                unfocusedLabelColor = CoolGrayMuted
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isProcessing) {
                Button(
                    onClick = {
                        if (cardholderName.isBlank() || cardNumber.length < 16 || expiryDate.isBlank() || cvvInput.length < 3) {
                            errorMsg = "Please fill in all credit card details correctly."
                            return@Button
                        }
                        errorMsg = null
                        isProcessing = true
                        scope.launch {
                            simulationStep = "Handshaking with bank terminal gateway..."
                            delay(1200)
                            simulationStep = "Running socket membership capacity authorization..."
                            delay(1000)
                            simulationStep = "Verifying charge authorization limits..."
                            delay(1100)
                            simulationStep = "Synchronizing secure token with Supabase cluster..."
                            delay(800)
                            onPaymentSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B840)),
                    modifier = Modifier.testTag("checkout_pay_button")
                ) {
                    Text("Secure Pay Now", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onClose) {
                    Text("Cancel", color = CoolGrayMuted, fontSize = 12.sp)
                }
            }
        }
    )
}
