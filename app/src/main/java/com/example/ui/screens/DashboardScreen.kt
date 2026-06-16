package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PromptVaultViewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.scale
import kotlin.math.abs
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing

enum class SubPage {
    Dashboard,
    Library,
    Categories,
    Favorites,
    Collections,
    Templates,
    Analytics,
    Team,
    Activity,
    Search,
    Settings,
    PromptDetail
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PromptVaultViewModel,
    onNavigateToPricing: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onNavigateBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val prompts by viewModel.filteredPrompts.collectAsState()
    val favorites by viewModel.favoritePrompts.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val workspaces by viewModel.workspaces.collectAsState()
    val recentQueries by viewModel.recentSearchQueries.collectAsState()
    val usageLogs by viewModel.promptUsageLogs.collectAsState()

    var isSearchFocused by remember { mutableStateOf(false) }

    val selectedWorkspaceId by viewModel.selectedWorkspaceId.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    val selectedPromptDetails by viewModel.selectedPromptForDetails.collectAsState()
    val activeVersions by viewModel.activeVersions.collectAsState()

    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val currentWorkspaceMembers by viewModel.currentWorkspaceMembers.collectAsState()

    val userTierState by viewModel.userTier.collectAsState()
    val hapticSwipeToDelete by viewModel.hapticSwipeToDelete.collectAsState()
    var showPremiumPaywall by remember { mutableStateOf(false) }
    var premiumBlockedFeature by remember { mutableStateOf("") }
    var isRefreshingSupabase by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // UI Dialog Modes
    var showCreateDialog by remember { mutableStateOf(false) }
    var promptToEdit by remember { mutableStateOf<PromptEntity?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showSqlSchemaDialog by remember { mutableStateOf(false) }
    var showTemplatesLibraryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Toast Copied Prompt
    var copiedToastText by remember { mutableStateOf<String?>(null) }

    fun showCopiedFeedback(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        viewModel.logPromptCopy()
        copiedToastText = "Copied to Clipboard!"
        viewModel.showToast("Prompt Copied to Clipboard! 📋")
        if (viewModel.hapticCopyToClipboard.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(copiedToastText) {
        if (copiedToastText != null) {
            kotlinx.coroutines.delay(1500)
            copiedToastText = null
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentSubPage by remember { mutableStateOf(SubPage.Dashboard) }
    var clickedPromptDetail by remember { mutableStateOf<PromptEntity?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ArtisticNavBg,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                SidebarContent(
                    categories = categories,
                    workspaces = workspaces,
                    selectedWorkspaceId = selectedWorkspaceId,
                    selectedCategory = selectedCategory,
                    userTierState = userTierState,
                    currentSubPage = currentSubPage,
                    onSubPageSelect = { subPage ->
                        currentSubPage = subPage
                        clickedPromptDetail = null
                        scope.launch { drawerState.close() }
                    },
                    onWorkspaceSelect = { wsId ->
                        viewModel.selectedWorkspaceId.value = wsId
                        currentSubPage = SubPage.Library
                        clickedPromptDetail = null
                        scope.launch { drawerState.close() }
                    },
                    onCategorySelect = { catName ->
                        viewModel.selectedCategory.value = catName
                        currentSubPage = SubPage.Library
                        clickedPromptDetail = null
                        scope.launch { drawerState.close() }
                    },
                    onAddSubCategory = { name, parentId ->
                        viewModel.addNewCategory(name, parentId)
                    },
                    onNavigateToPricing = {
                        onNavigateToPricing()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = ObsidianBg,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open folders sidebar", tint = EmeraldAccent)
                        }
                    },
                    title = {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PERSONAL SPACE",
                            color = ArtisticMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PromptVaultLogo(
                                modifier = Modifier.size(36.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Prompt",
                                    color = EmeraldAccent, // Lavender
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Text(
                                    text = "Vault",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        // Artistic avatar profile matching HTML (<div class="w-10 h-10 rounded-full bg-[#4A4458]... border border-[#938F99]">)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(ArtisticActivePill)
                                .border(1.dp, ArtisticBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    currentSubPage = SubPage.Settings
                                    Toast.makeText(context, "Navigated to Settings", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Settings",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.logout()
                                onNavigateBackToHome()
                            },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = ErrorColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        floatingActionButton = {
            // Only show FloatingActionButton if user has Creator/Writer permissions (Admin or Editor)
            if (currentUserRole != "Viewer") {
                FloatingActionButton(
                    onClick = {
                        promptToEdit = null
                        showCreateDialog = true
                    },
                    containerColor = EmeraldAccent,
                    contentColor = ArtisticDarkPurple,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("floating_add_prompt_button")
                        .padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Prompt", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            AnimatedContent(
                targetState = currentSubPage,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 60)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(220, delayMillis = 60))).togetherWith(
                        fadeOut(animationSpec = tween(120)) + slideOutVertically(targetOffsetY = { -15 }, animationSpec = tween(120))
                    )
                },
                label = "subapi_transition_shell"
            ) { targetState ->
                when (targetState) {
                    SubPage.Dashboard -> {
                        DashboardSubPage(
                            prompts = prompts,
                            favorites = favorites,
                            usageLogs = usageLogs,
                            workspaces = workspaces,
                            selectedWorkspaceId = selectedWorkspaceId,
                            onNavigateToSubPage = { currentSubPage = it },
                            onCreatePromptClick = {
                                promptToEdit = null
                                showCreateDialog = true
                            },
                            onSelectPrompt = { prompt ->
                                clickedPromptDetail = prompt
                                currentSubPage = SubPage.PromptDetail
                            },
                            onQuickCopy = { showCopiedFeedback(it) },
                            userTier = userTierState,
                            onUpgradeClick = { onNavigateToUpgrade() }
                        )
                    }
                    SubPage.Library -> {
                        LibrarySubPage(
                            prompts = prompts,
                            categories = categories,
                            workspaces = workspaces,
                            selectedWorkspaceId = selectedWorkspaceId,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            sortBy = sortBy,
                            currentUserRole = currentUserRole,
                            userTierState = userTierState,
                            hapticSwipeToDelete = hapticSwipeToDelete,
                            onWorkspaceSelect = { viewModel.selectedWorkspaceId.value = it },
                            onCategorySelect = { viewModel.selectedCategory.value = it },
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            onSortChange = { viewModel.sortBy.value = it },
                            onSelectPrompt = { prompt ->
                                clickedPromptDetail = prompt
                                currentSubPage = SubPage.PromptDetail
                            },
                            onQuickCopy = { showCopiedFeedback(it) },
                            onEditPrompt = { prompt ->
                                promptToEdit = prompt
                                showCreateDialog = true
                            },
                            onDeletePrompt = { viewModel.deletePrompt(it.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            onUpgradeDialog = {
                                premiumBlockedFeature = it
                                showPremiumPaywall = true
                            }
                        )
                    }
                    SubPage.Categories -> {
                        CategoriesSubPage(
                            prompts = prompts,
                            categories = categories,
                            onSelectCategory = { catName ->
                                viewModel.selectedCategory.value = catName
                                currentSubPage = SubPage.Library
                            }
                        )
                    }
                    SubPage.Favorites -> {
                        FavoritesSubPage(
                            favorites = favorites,
                            categories = categories,
                            onSelectPrompt = { prompt ->
                                clickedPromptDetail = prompt
                                currentSubPage = SubPage.PromptDetail
                            },
                            onQuickCopy = { showCopiedFeedback(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) }
                        )
                    }
                    SubPage.Collections -> {
                        CollectionsSubPage(
                            prompts = prompts,
                            workspaces = workspaces,
                            selectedWorkspaceId = selectedWorkspaceId,
                            onWorkspaceSelect = { wsId ->
                                viewModel.selectedWorkspaceId.value = wsId
                                currentSubPage = SubPage.Library
                            }
                        )
                    }
                    SubPage.Templates -> {
                        TemplatesSubPage(
                            onCreateFromTemplate = { titleText, descText, contentText, catText ->
                                promptToEdit = null
                                viewModel.addPrompt(
                                    title = titleText,
                                    description = descText,
                                    content = contentText,
                                    category = catText,
                                    workspaceId = selectedWorkspaceId,
                                    isPremiumTool = false
                                )
                                Toast.makeText(context, "Template Cloned as Workspace Project! 🚀", Toast.LENGTH_SHORT).show()
                                currentSubPage = SubPage.Library
                            }
                        )
                    }
                    SubPage.Analytics -> {
                        AnalyticsSubPage(
                            prompts = prompts,
                            favorites = favorites,
                            usageLogs = usageLogs
                        )
                    }
                    SubPage.Team -> {
                        TeamSubPage(
                            workspaces = workspaces,
                            selectedWorkspaceId = selectedWorkspaceId,
                            currentUserRole = currentUserRole,
                            currentWorkspaceMembers = currentWorkspaceMembers,
                            onInviteMember = { email, role ->
                                viewModel.inviteUserToWorkspace(email, role)
                                Toast.makeText(context, "Invited $email as $role", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    SubPage.Activity -> {
                        ActivitySubPage(
                            usageLogs = usageLogs
                        )
                    }
                    SubPage.Search -> {
                        SearchSubPage(
                            prompts = prompts,
                            categories = categories,
                            onSelectPrompt = { prompt ->
                                clickedPromptDetail = prompt
                                currentSubPage = SubPage.PromptDetail
                            },
                            onQuickCopy = { showCopiedFeedback(it) }
                        )
                    }
                    SubPage.Settings -> {
                        val highContrast by viewModel.highContrast.collectAsState()
                        SettingsSubPage(
                            currentUserEmail = currentUserEmail ?: "tasbir777x@gmail.com",
                            userTierState = userTierState,
                            highContrast = highContrast,
                            onHighContrastToggle = { viewModel.setHighContrast(!highContrast) },
                            hapticSwipeToDelete = hapticSwipeToDelete,
                            onHapticSwipeToDeleteToggle = { viewModel.setHapticSwipeToDelete(!hapticSwipeToDelete) },
                            onUpgradeClick = { onNavigateToPricing() }
                        )
                    }
                    SubPage.PromptDetail -> {
                        if (clickedPromptDetail != null) {
                            PromptDetailPage(
                                prompt = clickedPromptDetail!!,
                                categories = categories,
                                onBackToLibrary = { currentSubPage = SubPage.Library },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(clickedPromptDetail!!.id)
                                    clickedPromptDetail = clickedPromptDetail!!.copy(isFavorite = !clickedPromptDetail!!.isFavorite)
                                },
                                onQuickCopy = { showCopiedFeedback(clickedPromptDetail!!.content) },
                                onEdit = {
                                    promptToEdit = clickedPromptDetail
                                    showCreateDialog = true
                                }
                            )
                        } else {
                            currentSubPage = SubPage.Library
                        }
                    }
                }
            }
        }

        if (false) {
            PullToRefreshWrapper(
                isRefreshing = isRefreshingSupabase,
                onRefresh = {
                    scope.launch {
                        isRefreshingSupabase = true
                        delay(1500)
                        isRefreshingSupabase = false
                        Toast.makeText(context, "Supabase Prompt Feed Synced! ✨", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
            // Stats Segment Bar
            StatsBanner(
                totalCount = prompts.size,
                favCount = favorites.size,
                selectedWorkspaceName = workspaces.find { it.id == selectedWorkspaceId }?.name ?: "Personal Core"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sparkline usage frequency chart widget
            DashboardWidget(usageLogs = usageLogs)

            Spacer(modifier = Modifier.height(10.dp))

            // Read-Only Warning Banner
            AnimatedVisibility(
                visible = currentUserRole == "Viewer",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C1010))
                        .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Read-Only Warning", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                    Text(
                        text = "READ-ONLY: You are a Viewer in this folder. Creating, editing, or deleting prompts is restricted.",
                        color = Color(0xFFECEFF1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workspaces / Collaborative Folders Section
            Text(
                text = "COLLABORATIVE WORKSPACE FOLDERS",
                color = CoolGrayMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillModifierWithScroll()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (ws in workspaces) {
                    val isSelected = selectedWorkspaceId == ws.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) EmeraldAccent else CardBackground)
                            .clickable {
                                if (ws.id != 0 && userTierState == "Free") {
                                    premiumBlockedFeature = "team_folders"
                                    showPremiumPaywall = true
                                } else {
                                    viewModel.selectedWorkspaceId.value = ws.id
                                }
                            }
                            .border(
                                1.dp,
                                if (isSelected) EmeraldAccent else CoolGrayMuted.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (ws.iconName) {
                                    "campaign" -> Icons.Default.Campaign
                                    "code" -> Icons.Default.Code
                                    else -> Icons.Default.FolderShared
                                },
                                contentDescription = null,
                                tint = if (isSelected) ObsidianBg else EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = ws.name,
                                    color = if (isSelected) ObsidianBg else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (ws.id == 0) "Private Drive" else "Shared Folder",
                                    color = if (isSelected) ObsidianBg.copy(alpha = 0.7f) else CoolGrayMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Add Workspace Folder Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground.copy(alpha = 0.5f))
                        .clickable {
                            if (userTierState == "Free") {
                                premiumBlockedFeature = "team_folders"
                                showPremiumPaywall = true
                            } else {
                                showAddWorkspaceDialog = true
                            }
                        }
                        .border(1.dp, CoolGrayMuted.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(16.dp))
                        Text("Add Team Folder", color = CoolGrayMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Workspace Control & Console Button Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedWorkspaceId != 0) {
                    Button(
                        onClick = {
                            if (userTierState == "Free") {
                                premiumBlockedFeature = "multiplayer_collaboration"
                                showPremiumPaywall = true
                            } else {
                                showMembersDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBackground,
                            contentColor = EmeraldAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Manage Members ($currentUserRole)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = { showSqlSchemaDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianBg,
                        contentColor = Color(0xFFECEFF1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArtisticBorder),
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Supabase SQL Schema Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showTemplatesLibraryDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent.copy(alpha = 0.12f),
                        contentColor = EmeraldAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.height(38.dp).testTag("browse_templates_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Browse Templates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent.copy(alpha = 0.12f),
                        contentColor = EmeraldAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.height(38.dp).testTag("export_backup_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { onNavigateToUpgrade() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianBg,
                        contentColor = Color(0xFFE2B840) // Golden Accent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2B840)),
                    modifier = Modifier.height(38.dp).testTag("premium_ai_lab_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFE2B840), modifier = Modifier.size(16.dp))
                        Text("Multiplayer & AI Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar & Filter Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .zIndex(10f)
                ) {
                    Column {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search your vault...", fontSize = 14.sp, color = ArtisticMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = CoolGrayMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                                .testTag("prompt_search_field"),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedTextColor = LightGrayText,
                                unfocusedTextColor = LightGrayText,
                                cursorColor = EmeraldAccent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.logSearchQuery(searchQuery)
                                    }
                                    focusManager.clearFocus()
                                }
                            ),
                            singleLine = true
                        )

                        // Dropdown list
                        if (isSearchFocused && recentQueries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, ArtisticBorder.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                                    .testTag("search_history_dropdown"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        color = CoolGrayMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    recentQueries.take(5).forEach { historyItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.searchQuery.value = historyItem.query
                                                    viewModel.logSearchQuery(historyItem.query)
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = CoolGrayMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = historyItem.query,
                                                color = LightGrayText,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteRecentQuery(historyItem.query)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete from history",
                                                    tint = CoolGrayMuted.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Select Sorting order dropdown - Rounded Capsule
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardBackground)
                        .border(1.dp, ArtisticBorder, RoundedCornerShape(28.dp))
                        .clickable {
                            val nextSort = when (sortBy) {
                                "Newest" -> "Oldest"
                                "Oldest" -> "Alphabetical"
                                else -> "Newest"
                            }
                            viewModel.sortBy.value = nextSort
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Icon", tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Text(sortBy, color = LightGrayText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Chip list
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryChip(
                            name = "All",
                            isSelected = selectedCategory == "All",
                            onClick = { viewModel.selectedCategory.value = "All" }
                        )
                    }

                    item {
                        CategoryChip(
                            name = "Favorites ⭐",
                            isSelected = selectedCategory == "Favorites",
                            onClick = { viewModel.selectedCategory.value = "Favorites" }
                        )
                    }

                    items(categories) { cat ->
                        CategoryChip(
                            name = cat.name,
                            isSelected = selectedCategory == cat.name,
                            onClick = { viewModel.selectedCategory.value = cat.name }
                        )
                    }
                }

                // Add Category Button
                IconButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBackground)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom category", tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Toast Overlay
            AnimatedVisibility(
                visible = copiedToastText != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldAccent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copiedToastText ?: "", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Prompts Scroll Selection List
            if (prompts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp).fillMaxWidth(0.85f)
                    ) {
                        NeonEmptyIllustration(modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            text = "Your Secure Vault is Empty",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "No folder prompts matches compile state. Press the floating action button to create your first encrypted prompt entry, or expand your sidebar to explore folders.",
                            color = CoolGrayMuted.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(prompts, key = { it.id }) { prompt ->
                        SwipeToActionWrapper(
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                fadeOutSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            onSwipeRight = {
                                viewModel.toggleFavorite(prompt.id)
                                Toast.makeText(context, if (prompt.isFavorite) "Removed from pinned favorites" else "Pinned to favorites!", Toast.LENGTH_SHORT).show()
                            },
                            onSwipeLeft = {
                                if (currentUserRole != "Viewer") {
                                    viewModel.deletePrompt(prompt.id)
                                    Toast.makeText(context, "Deleted: ${prompt.title}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Viewers can't delete items", Toast.LENGTH_SHORT).show()
                                }
                            },
                            isFavorite = prompt.isFavorite,
                            hapticFeedbackEnabled = hapticSwipeToDelete
                        ) {
                            PromptItemCard(
                                prompt = prompt,
                                onSelect = {
                                    if (prompt.isPremiumTool && userTierState == "Free") {
                                        premiumBlockedFeature = "premium_ai_tools"
                                        showPremiumPaywall = true
                                    } else {
                                        viewModel.selectPromptDetails(prompt)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(prompt.id) },
                                onQuickCopy = {
                                    if (prompt.isPremiumTool && userTierState == "Free") {
                                        premiumBlockedFeature = "premium_ai_tools"
                                        showPremiumPaywall = true
                                    } else {
                                        showCopiedFeedback(prompt.content)
                                    }
                                },
                                onEdit = {
                                    if (prompt.isPremiumTool && userTierState == "Free") {
                                        premiumBlockedFeature = "premium_ai_tools"
                                        showPremiumPaywall = true
                                    } else {
                                        promptToEdit = prompt
                                        showCreateDialog = true
                                    }
                                },
                                onDelete = { viewModel.deletePrompt(prompt.id) },
                                isEditable = (currentUserRole != "Viewer"),
                                categories = categories
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Modal Sheet Detail View for a single Prompt (with automated Version Restore)
    if (selectedPromptDetails != null) {
        PromptDetailsDialog(
            prompt = selectedPromptDetails!!,
            versions = activeVersions,
            onClose = { viewModel.selectPromptDetails(null) },
            onQuickCopy = { showCopiedFeedback(selectedPromptDetails!!.content) },
            onRestore = { versionContent ->
                viewModel.restoreVersionToContent(selectedPromptDetails!!.id, versionContent)
                Toast.makeText(context, "Restored to version state successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog form for creating / editing prompt with strict validation
    if (showCreateDialog) {
        CreateOrEditPromptDialog(
            prompt = promptToEdit,
            categories = categories,
            currentWorkspaceId = selectedWorkspaceId,
            onClose = { showCreateDialog = false },
            onSave = { title, desc, content, category, wsId, isPremium ->
                if (promptToEdit == null) {
                    viewModel.addPrompt(title, desc, content, category, wsId, isPremium)
                } else {
                    viewModel.editPrompt(promptToEdit!!.id, title, desc, content, category, wsId, isPremium)
                }
                showCreateDialog = false
            },
            viewModel = viewModel
        )
    }

    // Dialog for adding custom category
    if (showAddCategoryDialog) {
        QuickInputDialog(
            title = "Create New Category",
            placeholder = "e.g. Midjourney, SEO Hook",
            onClose = { showAddCategoryDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.addNewCategory(name, colorHex = colorHex)
                showAddCategoryDialog = false
            }
        )
    }

    // Dialog for adding collaborative workspace folder
    if (showAddWorkspaceDialog) {
        AddWorkspaceDialog(
            onClose = { showAddWorkspaceDialog = false },
            onConfirm = { name, desc ->
                viewModel.addNewWorkspaceFolder(name, desc)
                showAddWorkspaceDialog = false
            }
        )
    }

    // Dialog for managing team members
    if (showMembersDialog) {
        WorkspaceMembersDialog(
            currentUserRole = currentUserRole,
            members = currentWorkspaceMembers,
            onClose = { showMembersDialog = false },
            onInvite = { email, role ->
                viewModel.inviteUserToWorkspace(email, role)
                Toast.makeText(context, "Invited $email as $role!", Toast.LENGTH_SHORT).show()
            },
            onRemove = { id ->
                viewModel.removeUserFromWorkspace(id)
                Toast.makeText(context, "Member removed successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog for viewing Supabase database Schema
    if (showSqlSchemaDialog) {
        SupabaseSchemaConsoleDialog(
            onClose = { showSqlSchemaDialog = false },
            onCopySql = { sql ->
                showCopiedFeedback(sql)
            }
        )
    }

    // Dialog for browsing curated template library
    if (showTemplatesLibraryDialog) {
        PromptTemplatesLibraryDialog(
            onClose = { showTemplatesLibraryDialog = false },
            onClone = { templateTitle, templateDesc, templateContent, templateCat ->
                viewModel.addPrompt(
                    title = templateTitle,
                    description = templateDesc,
                    content = templateContent,
                    category = templateCat,
                    workspaceId = selectedWorkspaceId
                )
                Toast.makeText(context, "Cloned '$templateTitle' successfully to workspace!", Toast.LENGTH_SHORT).show()
                showTemplatesLibraryDialog = false
            }
        )
    }

    if (showExportDialog) {
        PromptExportBackupDialog(
            viewModel = viewModel,
            onClose = { showExportDialog = false }
        )
    }

    if (showPremiumPaywall) {
        PremiumPaywallDialog(
            feature = premiumBlockedFeature,
            onClose = { showPremiumPaywall = false },
            onUpgrade = {
                showPremiumPaywall = false
                onNavigateToPricing()
            }
        )
    }
}
}
}

// Custom extension to avoid compiler issues if scroll configuration is complex
private fun Modifier.fillModifierWithScroll(): Modifier = this.fillMaxWidth()

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) EmeraldAccent.copy(alpha = 0.15f) else CardBackground)
            .border(
                1.dp,
                if (isSelected) EmeraldAccent else CoolGrayMuted.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) EmeraldAccent else LightGrayText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun StatsBanner(totalCount: Int, favCount: Int, selectedWorkspaceName: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACTIVE DRIVE: ${selectedWorkspaceName.uppercase()}",
            color = ArtisticMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Prompts Card - Custom Highlight Total Background (#332D41)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArtisticHighlightTotal)
                    .border(1.dp, ArtisticBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL",
                        color = EmeraldAccent, // Lavender
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalCount.toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Favorites Card - Custom dark gray Background (#212121)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF212121))
                    .border(1.dp, ArtisticBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "FAVORITES",
                        color = Color(0xFFB0B0B0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = favCount.toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Versions Card - Custom dark gray Background (#212121)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF212121))
                    .border(1.dp, ArtisticBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "VERSIONS",
                        color = Color(0xFFB0B0B0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val estimatedVersions = totalCount * 2 + favCount
                    Text(
                        text = if (totalCount == 0) "0" else estimatedVersions.toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getCategoryColors(category: String): Triple<Color, Color, Color> {
    return when (category.lowercase()) {
        "marketing", "seo", "copywriting", "email", "sales" -> Triple(
            CategoryMarketingBg,
            CategoryMarketingText,
            CategoryMarketingBorder
        )
        "coding", "development", "react", "programming" -> Triple(
            CategoryCodingBg,
            CategoryCodingText,
            CategoryCodingBorder
        )
        else -> Triple(
            ObsidianBg,
            CoolGrayMuted,
            ArtisticBorder
        )
    }
}

@Composable
fun SwipeToActionWrapper(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val thresholdPx = with(density) { 100.dp.toPx() }
    val maxDragPx = with(density) { 140.dp.toPx() }

    // Keep track of whether haptic feedback has been triggered during the current swipe decision
    var hasHapticTriggeredForThreshold by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
    ) {
        // UNDERLAY ACTIONS BACKGROUND
        val currentOffset = offsetX.value
        if (currentOffset != 0f) {
            val isSwipingRight = currentOffset > 0
            val containerColor = if (isSwipingRight) {
                EmeraldAccent.copy(alpha = 0.15f)
            } else {
                Color(0xFFEF4444).copy(alpha = 0.15f)
            }
            val borderColor = if (isSwipingRight) EmeraldAccent else Color(0xFFEF4444)

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(containerColor)
                    .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val progress = (abs(currentOffset) / thresholdPx).coerceIn(0.6f, 1.3f)
                val isThresholdPassed = abs(currentOffset) >= thresholdPx

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.scale(progress)
                ) {
                    if (isSwipingRight) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.StarOutline else Icons.Default.Star,
                            contentDescription = "Favorite Action",
                            tint = if (isThresholdPassed) EmeraldAccent else EmeraldAccent.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isFavorite) "Unpin" else "Pin Item",
                            color = if (isThresholdPassed) EmeraldAccent else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Delete",
                            color = if (isThresholdPassed) Color(0xFFEF4444) else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Action",
                            tint = if (isThresholdPassed) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // SWIPEABLE CONTENT CONTAINED WITHIN BOX WITH INT OFFSET
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val resistance = 0.7f
                                val targetValue = offsetX.value + (dragAmount * resistance)
                                val resolvedValue = targetValue.coerceIn(-maxDragPx, maxDragPx)
                                offsetX.snapTo(resolvedValue)

                                // Subtle touch pulse toggle when crossing decision threshold.
                                // This provides native equivalence to premium web 'Navigator.vibrate()' pulses
                                val passed = abs(resolvedValue) >= thresholdPx
                                if (passed && !hasHapticTriggeredForThreshold) {
                                    if (hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    hasHapticTriggeredForThreshold = true
                                } else if (!passed && hasHapticTriggeredForThreshold) {
                                    if (hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    hasHapticTriggeredForThreshold = false
                                }
                            }
                        },
                        onDragEnd = {
                            if (offsetX.value > thresholdPx) {
                                scope.launch {
                                    if (hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onSwipeRight()
                                    offsetX.animateTo(0f, animationSpec = tween(durationMillis = 300))
                                    hasHapticTriggeredForThreshold = false
                                }
                            } else if (offsetX.value < -thresholdPx) {
                                scope.launch {
                                    if (hapticFeedbackEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onSwipeLeft()
                                    offsetX.animateTo(0f, animationSpec = tween(durationMillis = 300))
                                    hasHapticTriggeredForThreshold = false
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                                    hasHapticTriggeredForThreshold = false
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                                hasHapticTriggeredForThreshold = false
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val pullOffset = remember { Animatable(0f) }
    val thresholdPx = with(density) { 80.dp.toPx() }
    val maxPullPx = with(density) { 150.dp.toPx() }
    var hasHapticTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullOffset.animateTo(0f, animationSpec = tween(300))
        } else {
            pullOffset.animateTo(with(density) { 60.dp.toPx() }, animationSpec = tween(300))
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && pullOffset.value > 0) {
                    val newOffset = (pullOffset.value + delta).coerceAtLeast(0f)
                    scope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0 && !isRefreshing) {
                    val resistance = 0.5f
                    val newOffset = (pullOffset.value + delta * resistance).coerceAtMost(maxPullPx)
                    scope.launch {
                        pullOffset.snapTo(newOffset)
                        
                        val passed = newOffset >= thresholdPx
                        if (passed && !hasHapticTriggered) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hasHapticTriggered = true
                        } else if (!passed && hasHapticTriggered) {
                            hasHapticTriggered = false
                        }
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value >= thresholdPx && !isRefreshing) {
                    onRefresh()
                } else if (!isRefreshing) {
                    scope.launch {
                        pullOffset.animateTo(0f, animationSpec = tween(250))
                    }
                }
                hasHapticTriggered = false
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullOffset.value.roundToInt()) }
        ) {
            content()
        }

        if (pullOffset.value > 0f || isRefreshing) {
            val progress = (pullOffset.value / thresholdPx).coerceIn(0f, 1f)
            
            val rotation = if (isRefreshing) {
                val infiniteTransition = rememberInfiniteTransition(label = "RefSpin")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "RefAngle"
                )
                angle
            } else {
                progress * 360f
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (pullOffset.value * 0.45f).roundToInt() - 30) }
                    .size(44.dp)
                    .alpha(progress)
                    .scale(progress.coerceAtLeast(0.5f))
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(
                        1.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                EmeraldAccent,
                                Color(0xFF34D399),
                                EmeraldAccent.copy(alpha = 0.2f),
                                EmeraldAccent
                            )
                        ),
                        CircleShape
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Syncing with Supabase Cloud Database",
                    tint = EmeraldAccent,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotation)
                )
            }
        }
    }
}

@Composable
fun PromptItemCard(
    prompt: PromptEntity,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isEditable: Boolean = true,
    categories: List<CategoryEntity> = emptyList()
) {
    val matchingCategory = categories.find { it.name.equals(prompt.category, ignoreCase = true) }
    val (tagBg, tagText, tagBorder) = if (matchingCategory != null) {
        val baseColor = try {
            Color(android.graphics.Color.parseColor(matchingCategory.colorHex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
        Triple(
            baseColor.copy(alpha = 0.15f),
            baseColor,
            baseColor.copy(alpha = 0.4f)
        )
    } else {
        getCategoryColors(prompt.category)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .glassmorphicContainer(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag & Version Badge Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Tag with artistic borders
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(tagBg)
                            .border(1.dp, tagBorder, RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = prompt.category,
                            color = tagText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Version Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(ObsidianBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "v2.4",
                            color = CoolGrayMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (prompt.isPremiumTool) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFFE2B840).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFE2B840), RoundedCornerShape(50.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Premium ⚡",
                                color = Color(0xFFE2B840),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Favorite Star Icon aligned precisely
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (prompt.isFavorite) EmeraldAccent else CoolGrayMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Description
            Text(
                text = prompt.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = prompt.description,
                color = CoolGrayMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body content visual container Mono Editor Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBg)
                    .border(1.dp, ArtisticBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = prompt.content,
                    color = LightGrayText.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(10.dp))

            // Action Footer indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
                val formattedDate = formatter.format(Date(prompt.updatedAt))

                Text(
                    text = "Modified: $formattedDate",
                    color = CoolGrayMuted.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Prompt Accent Labeled Button
                    TextButton(
                        onClick = onQuickCopy,
                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("copy_prompt_button_${prompt.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Quick copy prompt to clipboard",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "COPY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isEditable) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Prompt", tint = Secondary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Prompt", tint = ErrorColor, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Read-Only Lock", tint = CoolGrayMuted, modifier = Modifier.size(14.dp))
                            Text("Viewer", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Full detailed view displaying code blocks & automated versioning logs
@Composable
fun PromptDetailsDialog(
    prompt: PromptEntity,
    versions: List<PromptVersionEntity>,
    onClose: () -> Unit,
    onQuickCopy: () -> Unit,
    onRestore: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(prompt.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Version History Logs (Supabase Audit)", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Details", tint = CoolGrayMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category and description Info
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(prompt.category, color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = if (prompt.workspaceId == 0) "Private Vault" else "Workspace Folder",
                        color = CoolGrayMuted,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = prompt.description,
                    color = LightGrayText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Prompt content Box
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Prompts Context", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onQuickCopy) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                                Text("COPY TEXT", color = EmeraldAccent, fontSize = 11.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, CoolGrayMuted.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = prompt.content,
                            color = LightGrayText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }

                HorizontalDivider(color = CoolGrayMuted.copy(alpha = 0.1f))

                // Versions history track
                Text("Revision History tracking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                if (versions.isEmpty()) {
                    Text("No past version logs registered.", color = CoolGrayMuted, fontSize = 12.sp)
                } else {
                    for (ver in versions) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBg.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldAccent)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("V${ver.versionNumber}", color = ObsidianBg, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }

                                        val simpleFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
                                        Text(
                                            text = simpleFormat.format(Date(ver.editedAt)),
                                            color = CoolGrayMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Restore Trigger if content differs of is older
                                    if (prompt.content != ver.content) {
                                        TextButton(
                                            onClick = { onRestore(ver.content) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Text("Restore This V${ver.versionNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldAccent.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("ACTIVE VERSION", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    text = ver.content,
                                    color = LightGrayText.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
            ) {
                Text("Dismiss View", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// Dialog prompt editor form with clean validations
@Composable
fun CreateOrEditPromptDialog(
    prompt: PromptEntity?,
    categories: List<CategoryEntity>,
    currentWorkspaceId: Int,
    onClose: () -> Unit,
    onSave: (title: String, desc: String, content: String, category: String, wsId: Int, isPremium: Boolean) -> Unit,
    viewModel: com.example.ui.viewmodel.PromptVaultViewModel
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(prompt?.title ?: "") }
    var description by remember { mutableStateOf(prompt?.description ?: "") }
    var content by remember { mutableStateOf(prompt?.content ?: "") }
    var category by remember { mutableStateOf(prompt?.category ?: "General") }
    var wsId by remember { mutableStateOf(prompt?.workspaceId ?: currentWorkspaceId) }
    var isPremiumTool by remember { mutableStateOf(prompt?.isPremiumTool ?: false) }

    // Strict validation client-side schema (Zod style)
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (prompt == null) "Create AI Prompt Vault" else "Edit Prompt Entry",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMsg != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ErrorColor.copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Text(errorMsg!!, color = ErrorColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Title Input
                Column {
                    Text("Prompt Title (Zod: Min 3 Chars)", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_title_input"),
                        singleLine = true,
                        placeholder = { Text("e.g. Sales Email Hook") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ObsidianBg,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Description Input
                Column {
                    Text("Vault Short Description", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Used for quick reference search") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ObsidianBg,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category Selection list
                Column {
                    Text("Operational Category", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in categories) {
                            val active = category == cat.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (active) EmeraldAccent else ObsidianBg)
                                    .border(1.dp, if (active) EmeraldAccent else CoolGrayMuted.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .clickable { category = cat.name }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (active) ObsidianBg else LightGrayText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Collaborative Workspace Selector
                Column {
                    Text("Target Folder / Workspace (Supabase isolation)", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Personal Core", 1 to "Marketing", 2 to "SaaS Dev").forEach { (id, label) ->
                            val active = wsId == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) EmeraldAccent else ObsidianBg)
                                    .clickable { wsId = id }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (active) ObsidianBg else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Premium Tool Switch Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, ArtisticBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mark as Premium Tool ⚡", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Restricts access to active Premium users for automated code logic and scripts.", color = CoolGrayMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = isPremiumTool,
                        onCheckedChange = { isPremiumTool = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldAccent,
                            uncheckedThumbColor = CoolGrayMuted,
                            uncheckedTrackColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("prompt_premium_switch")
                    )
                }

                // Prompt Body Context Editor
                Column {
                    Text("Command Context Prompts Body (Zod: Required)", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("prompt_content_input"),
                        placeholder = { Text("Act as professional writer...") },
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ObsidianBg,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // --- AI Assist Suite panel ---
                val aiLoading by viewModel.aiSuggestionLoading.collectAsState()
                val aiResult by viewModel.aiSuggestionResult.collectAsState()
                val aiError by viewModel.aiSuggestionError.collectAsState()

                val aiGenLoading by viewModel.aiGenerationLoading.collectAsState()
                val aiGenResult by viewModel.aiGenerationResult.collectAsState()
                val aiGenError by viewModel.aiGenerationError.collectAsState()

                var activeAiTab by remember { mutableStateOf("categorize") } // "categorize" or "generate"
                var aiPromptInput by remember { mutableStateOf("") }

                // If user opens dialog, clear previous suggestion session
                LaunchedEffect(Unit) {
                    viewModel.clearAiSuggestion()
                    viewModel.clearAiGeneration()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBg)
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // Header title with icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Gemini AI Assistant Suite",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardBackground)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (activeAiTab == "categorize") EmeraldAccent else Color.Transparent)
                                .clickable { activeAiTab = "categorize" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Categorize & Tag",
                                color = if (activeAiTab == "categorize") ObsidianBg else LightGrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (activeAiTab == "generate") EmeraldAccent else Color.Transparent)
                                .clickable { activeAiTab = "generate" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Auto-Generate / Improve ⭐",
                                color = if (activeAiTab == "generate") ObsidianBg else LightGrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeAiTab == "categorize") {
                        // --- CATEGORIZE TAB ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Analyze context & suggest folder",
                                color = CoolGrayMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (!aiLoading) {
                                TextButton(
                                    onClick = {
                                        if (content.isNotBlank() || title.isNotBlank()) {
                                            viewModel.requestAiSuggestion(title, description, content)
                                        } else {
                                            Toast.makeText(context, "Please enter a title or prompt body first!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            } else {
                                CircularProgressIndicator(
                                    color = EmeraldAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (aiLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Gemini is analyzing prompt context...",
                                    color = EmeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (aiError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiError!!,
                                color = ErrorColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (aiResult != null) {
                            val result = aiResult!!
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Suggested Category:", color = CoolGrayMuted, fontSize = 11.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldAccent.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = result.category,
                                                color = EmeraldAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            category = result.category
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Apply Category", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (result.tags.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Suggested Tags:", color = CoolGrayMuted, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                for (tag in result.tags) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color.White.copy(alpha = 0.05f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "#$tag",
                                                            color = LightGrayText,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }

                                            val tagsString = result.tags.joinToString(" ") { "#$it" }
                                            TextButton(
                                                onClick = {
                                                    if (description.isBlank()) {
                                                        description = tagsString
                                                    } else {
                                                        if (!description.contains(tagsString)) {
                                                            description = "$description $tagsString"
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Append Tags", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (result.explanation.isNotBlank()) {
                                    HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.2f))
                                    Text(
                                        text = result.explanation,
                                        color = CoolGrayMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    } else {
                        // --- AUTO-GENERATE / IMPROVE TAB ---
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter seed description/roughs:",
                                color = LightGrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            TextField(
                                value = aiPromptInput,
                                onValueChange = { aiPromptInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("prompt_ai_generator_input"),
                                placeholder = { Text("e.g. detailed python code commenter, or marketing email booster...", color = CoolGrayMuted, fontSize = 11.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldAccent,
                                    unfocusedBorderColor = CardBackground,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!aiGenLoading) {
                                    Button(
                                        onClick = {
                                            if (aiPromptInput.isNotBlank()) {
                                                viewModel.requestAiGeneration(aiPromptInput, "generate")
                                            } else {
                                                Toast.makeText(context, "Please enter an idea/seed first!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("btn_ai_generate_new"),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Gen New", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val source = if (aiPromptInput.isNotBlank()) aiPromptInput else content
                                            if (source.isNotBlank()) {
                                                viewModel.requestAiGeneration(source, "improve")
                                            } else {
                                                Toast.makeText(context, "Nothing to improve! Enter an idea or write in the prompt body field.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp).testTag("btn_ai_improve"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Optimize Draft", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            color = EmeraldAccent,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Gemini is engineering prompt...",
                                            color = EmeraldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (aiGenError != null) {
                                Text(
                                    text = aiGenError!!,
                                    color = ErrorColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (aiGenResult != null) {
                                val result = aiGenResult!!
                                HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.2f))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardBackground)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Preview: ${result.title}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldAccent.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = result.category,
                                                color = EmeraldAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(result.description, color = LightGrayText, fontSize = 10.sp)

                                    // Display scrollable generated body
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 100.dp)
                                            .background(ObsidianBg)
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = result.content,
                                            color = CoolGrayMuted,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }

                                    if (result.explanation.isNotBlank()) {
                                        Text(
                                            text = "Design logic: ${result.explanation}",
                                            color = CoolGrayMuted,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            title = result.title
                                            description = result.description
                                            content = result.content
                                            category = result.category
                                            Toast.makeText(context, "Applied prompt structure to edit fields! ✨", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(28.dp).testTag("apply_ai_prompt"),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Apply Generated Prompt Structure", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Custom Zod Verification Simulation
                    if (title.length < 3) {
                        errorMsg = "Schema Validation Error: Title must be at least 3 characters."
                        return@Button
                    }
                    if (content.isBlank()) {
                        errorMsg = "Schema Validation Error: Command prompts body is strictly required."
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMsg = "Schema Validation Error: Short helper description is required."
                        return@Button
                    }
                    errorMsg = null
                    onSave(title, description, content, category, wsId, isPremiumTool)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                modifier = Modifier.testTag("save_prompt_submit")
            ) {
                Text("Save to Supabase", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel", color = CoolGrayMuted)
            }
        }
    )
}

// Dialog quick input helper
@Composable
fun QuickInputDialog(
    title: String,
    placeholder: String,
    onClose: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#8B5CF6") }
    val premiumColors = remember {
        listOf(
            "#8B5CF6", // Purple
            "#10B981", // Emerald
            "#3B82F6", // Blue
            "#06B6D4", // Cyan
            "#F59E0B", // Amber
            "#F43F5E", // Rose
            "#EC4899", // Pink
            "#38BDF8"  // Sky
        )
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text(placeholder, color = CoolGrayMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = ArtisticBorder,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "CHOOSE COLOR CODES",
                    color = CoolGrayMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    premiumColors.forEach { colorStr ->
                        val color = Color(android.graphics.Color.parseColor(colorStr))
                        val isSelected = selectedColorHex == colorStr
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = colorStr }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotEmpty()) onConfirm(rawText, selectedColorHex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
            ) {
                Text("Create Folder", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel", color = CoolGrayMuted) }
        }
    )
}

// Dialog for adding custom workspace folder
@Composable
fun AddWorkspaceDialog(
    onClose: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var wsName by remember { mutableStateOf("") }
    var wsDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(12.dp),
        title = { Text("Create Collaborative Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = wsName,
                    onValueChange = { wsName = it },
                    placeholder = { Text("Folder name (e.g. Sales Reps)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = ObsidianBg,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = wsDesc,
                    onValueChange = { wsDesc = it },
                    placeholder = { Text("Brief purpose statement...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = ObsidianBg,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (wsName.isNotEmpty()) onConfirm(wsName, wsDesc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
            ) {
                Text("Create Collaboration", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel", color = CoolGrayMuted) }
        }
    )
}

@Composable
fun WorkspaceMembersDialog(
    currentUserRole: String,
    members: List<WorkspaceMembershipEntity>,
    onClose: () -> Unit,
    onInvite: (String, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("Viewer") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Workspace Team Members", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CoolGrayMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Manage user access controls for this folder. Admins can invite or remove members and set permission matrices.",
                    color = CoolGrayMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // List existing members
                Text("Active Team Members", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (member in members) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(member.userEmail, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (member.role.lowercase()) {
                                                "admin" -> Color(0xFFFF5252).copy(alpha = 0.15f)
                                                "editor" -> EmeraldAccent.copy(alpha = 0.15f)
                                                else -> CoolGrayMuted.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = member.role.uppercase(),
                                        color = when (member.role.lowercase()) {
                                            "admin" -> Color(0xFFFF5252)
                                            "editor" -> EmeraldAccent
                                            else -> CoolGrayMuted
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Show delete option ONLY if current user is Admin OR they are deleting themselves
                            if (currentUserRole == "Admin") {
                                IconButton(onClick = { onRemove(member.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove member", tint = ErrorColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // If role is Admin, allow inviting/adding users
                if (currentUserRole == "Admin") {
                    HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.4f))
                    Text("Add / Invite New Member", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            placeholder = { Text("Enter user email...", color = CoolGrayMuted, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = ArtisticBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Role Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Role:", color = CoolGrayMuted, fontSize = 13.sp)

                            Box {
                                Button(
                                    onClick = { expandedDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBg, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ArtisticBorder),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(newRole, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier.background(CardBackground)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Admin", color = Color.White) },
                                        onClick = {
                                            newRole = "Admin"
                                            expandedDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Editor", color = Color.White) },
                                        onClick = {
                                            newRole = "Editor"
                                            expandedDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Viewer", color = Color.White) },
                                        onClick = {
                                            newRole = "Viewer"
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                if (newEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                                    onInvite(newEmail.trim(), newRole)
                                    newEmail = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Invite as $newRole", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss", color = EmeraldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SupabaseSchemaConsoleDialog(
    onClose: () -> Unit,
    onCopySql: (String) -> Unit
) {
    val sqlCode = """
-- Supabase PostgreSQL Schema with Row Level Security (RLS)
-- 1. Workspaces Table (Collab Space)
CREATE TABLE workspaces (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_name VARCHAR(50) DEFAULT 'folder_shared'
);

-- 2. Workspace Memberships (Access Matrix)
CREATE TABLE workspace_memberships (
    id SERIAL PRIMARY KEY,
    workspace_id INT REFERENCES workspaces(id) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    role VARCHAR(50) CHECK (role IN ('Admin', 'Editor', 'Viewer')),
    UNIQUE(workspace_id, user_email)
);

-- 3. Prompts (Workspace Isolated)
CREATE TABLE prompts (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT NOT NULL,
    category VARCHAR(100) DEFAULT 'General',
    is_favorite BOOLEAN DEFAULT false,
    owner_email VARCHAR(255) NOT NULL,
    workspace_id INT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Prompt Versions (Automated Revisions)
CREATE TABLE prompt_versions (
    id SERIAL PRIMARY KEY,
    prompt_id INT REFERENCES prompts(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    content TEXT NOT NULL,
    edited_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Enable Row Level Security (RLS)
ALTER TABLE prompts ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_memberships ENABLE ROW LEVEL SECURITY;

-- 6. Access Control RLS Policies
-- Users select workspace prompts if they are members
CREATE POLICY view_member_prompts ON prompts
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM workspace_memberships wm
            WHERE wm.workspace_id = prompts.workspace_id
              AND wm.user_email = auth.email()
        )
    );

-- Editors & Admins insert/delete/update workspace prompts
CREATE POLICY edit_member_prompts ON prompts
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM workspace_memberships wm
            WHERE wm.workspace_id = prompts.workspace_id
              AND wm.user_email = auth.email()
              AND wm.role IN ('Admin', 'Editor')
        )
    );
""".trimIndent()

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Supabase SQL Schema Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("PostgreSQL, Roles & RLS Policies", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CoolGrayMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Copy this production-configured PostgreSQL schema script and execute it in your Supabase SQL Editor. It establishes constraints, workspace references, version audits, and row level permissions automatically.",
                    color = CoolGrayMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { onCopySql(sqlCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Copy Full SQL Script", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, ArtisticBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = sqlCode,
                        color = Color(0xFFA5D6A7),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss Console", color = EmeraldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class CuratedTemplate(
    val title: String,
    val description: String,
    val content: String,
    val category: String,
    val tags: List<String>
)

@Composable
fun PromptTemplatesLibraryDialog(
    onClose: () -> Unit,
    onClone: (title: String, desc: String, content: String, category: String) -> Unit
) {
    val curatedTemplates = remember {
        listOf(
            CuratedTemplate(
                title = "React & Tailwind Component Architect",
                category = "Coding",
                description = "Generates high-performance, accessible, and responsive React component scripts utilizing Tailwind CSS spacing and styling tokens.",
                content = "You are an expert Senior Frontend developer specializing in React and Tailwind CSS. Design a standard, production-ready React component based on the request below. Adhere strictly to the following parameters:\n- Use absolute semantic accessibility standards.\n- Keep logic modular.\n- Follow clean modern layout.\n[Parameters]\nRequest: {Specify what component you want, e.g. a dark-themed user profile card}",
                tags = listOf("react", "tailwind", "frontend", "components")
            ),
            CuratedTemplate(
                title = "SEO Blog Outline Architect",
                category = "Marketing",
                description = "Builds structured, search-engine-optimized content skeletons optimized for high-density indexing and user-intent matching.",
                content = "Act as an elite SEO Strategist and content architect. Your goal is to draft an exhaustive, highly structured, search-intent optimized article outline targeting the provided primary and secondary keywords.\n- Maximize search intent value.\n- List proposed heading titles (H1, H2, H3).\n- Specify estimated word counts per section.\n- Detail list of secondary keywords.\n[Parameters]\nTopic: {Input topic and keywords}",
                tags = listOf("seo", "content", "outline", "blogging")
            ),
            CuratedTemplate(
                title = "PostgreSQL Query Optimization Expert",
                category = "Coding",
                description = "Diagnoses SQL performance bottlenecks from query plans and suggests optimized index allocations and join sequences.",
                content = "You are a distinguished Database Administrator specializing in PostgreSQL. Analyze the following slow database query and execution plan. Recommend modern tuning strategies:\n- Identify exact scan bottlenecks (e.g. Sequential Scans vs Index Scans).\n- Suggest specific indexes with columns mapping.\n- Proactively suggest rewriting complex CTEs or joins.\n[Parameters]\nTarget Query: {Paste query & explain plan here}",
                tags = listOf("sql", "postgresql", "database", "tuning")
            ),
            CuratedTemplate(
                title = "High-Conversion B2B Cold Outreach",
                category = "Sales",
                description = "Drafts hyper-personalized B2B cold email sequences that prioritize micro-commitments and high-empathy hooks.",
                content = "Write a high-converting 3-step B2B cold outreach email sequence designed to start authentic business conversations.\n- Step 1: Eye-catching short hook, value proposition under 120 words.\n- Step 2: High value reminder/case-study snippet.\n- Step 3: Low-friction CTA (e.g., 'Do you have 2 minutes for a brief chat?').\n[Parameters]\nSender Role: {Your role}\nTarget Prospect: {Prospect bio and industry}",
                tags = listOf("sales", "email", "b2b", "outreach")
            ),
            CuratedTemplate(
                title = "System Architecture & PRD Author",
                category = "Productivity",
                description = "Generates production-grade Product Requirement Documents (PRDs) and high-level service architecture designs.",
                content = "You are a Principal Product Manager and Technical Architect. Create an exhaustive Product Requirement Document (PRD) for the following product description:\n- Abstract and Vision Statement.\n- Key User Personas & Epic/User Stories.\n- Functional and Non-Functional specifications.\n- High-level system microservices diagram outline.\n[Parameters]\nProduct Concept: {Input product features}",
                tags = listOf("prd", "architecture", "system-design", "pm")
            ),
            CuratedTemplate(
                title = "Gemini Advanced Compose Explainer",
                category = "AI",
                description = "Explains code snippets and implements new features in Jetpack Compose using elite Google Gemini Prompt engineering.",
                content = "You are an elite Google Gemini AI Engineer. Analyze the target codebase or component snippet and propose high-performance Jetpack Compose rewrites. Ensure modern dynamic M3 colors and spring animations are applied.\n[Parameters]\nCode Snippet: {Paste source snippet}",
                tags = listOf("gemini", "ai", "compose", "kotlin")
            )
        )
    }

    var selectedTemplate by remember { mutableStateOf<CuratedTemplate?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember { listOf("All", "Coding", "Marketing", "Sales", "AI", "Productivity") }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTemplate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { selectedTemplate = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldAccent)
                        }
                        Column {
                            Text("Template Preview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(selectedTemplate!!.category.uppercase(), color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column {
                        Text("Curated Prompt Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Instant Cloud-Clonable Starters", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CoolGrayMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTemplate != null) {
                    val template = selectedTemplate!!
                    
                    Text(
                        text = template.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = template.description,
                        color = LightGrayText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Prompt Instructions Template:", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, ArtisticBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = template.content,
                            color = Color(0xFFA5D6A7),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (tag in template.tags) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = CoolGrayMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onClone(template.title, template.description, template.content, template.category)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("clone_template_action_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Clone into Vault Folder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                } else {
                    Text(
                        text = "Browse high-converting, cloud-clonable industry prompt templates. Select any template to review instructions and instantly clone into your current vault category index.",
                        color = CoolGrayMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // Search inside library
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search templates & tags...", color = CoolGrayMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = ArtisticBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Categories capsule scroll row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (cat in categories) {
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) EmeraldAccent.copy(alpha = 0.15f) else ObsidianBg)
                                    .border(1.dp, if (isSel) EmeraldAccent else ArtisticBorder, RoundedCornerShape(16.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) EmeraldAccent else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Templates List
                    val filteredList = curatedTemplates.filter {
                        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
                        (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) ||
                         it.description.contains(searchQuery, ignoreCase = true) ||
                         it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) })
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No templates found matching your filter.", color = CoolGrayMuted, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (tmpl in filteredList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianBg)
                                        .border(1.dp, ArtisticBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedTemplate = tmpl }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tmpl.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(EmeraldAccent.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = tmpl.category.uppercase(),
                                                    color = EmeraldAccent,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tmpl.description,
                                            color = CoolGrayMuted,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "View details",
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss Library", color = EmeraldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PromptExportBackupDialog(
    viewModel: com.example.ui.viewmodel.PromptVaultViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val hapticCopyToClipboard by viewModel.hapticCopyToClipboard.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isJsonSelected by remember { mutableStateOf(true) }
    var useActiveWorkspaceOnly by remember { mutableStateOf(true) }
    
    var loadedCount by remember { mutableStateOf(0) }
    var isQueryingCount by remember { mutableStateOf(true) }
    var promptsToExport by remember { mutableStateOf<List<PromptEntity>>(emptyList()) }

    // Reactively refresh counts based on selected scope
    LaunchedEffect(useActiveWorkspaceOnly) {
        isQueryingCount = true
        val list = if (useActiveWorkspaceOnly) {
            viewModel.getActiveWorkspacePrompts()
        } else {
            viewModel.getFullVaultPrompts()
        }
        promptsToExport = list
        loadedCount = list.size
        isQueryingCount = false
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "Backup & Export Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Generate and share portable local backups",
                    color = EmeraldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Export fully formatted backups as spreadsheets (.csv) or developer exchange sheets (.json) securely.",
                            color = CoolGrayMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // 1. SELECT FORMAT SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "1. Select Output File Format:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // JSON format capsule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isJsonSelected) EmeraldAccent.copy(alpha = 0.12f) else ObsidianBg)
                                .border(1.dp, if (isJsonSelected) EmeraldAccent else ArtisticBorder, RoundedCornerShape(10.dp))
                                .clickable { isJsonSelected = true }
                                .padding(12.dp)
                                .testTag("export_format_json"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "JSON Format",
                                    color = if (isJsonSelected) EmeraldAccent else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Developer Backup (.json)",
                                    color = CoolGrayMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // CSV format capsule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isJsonSelected) EmeraldAccent.copy(alpha = 0.12f) else ObsidianBg)
                                .border(1.dp, if (!isJsonSelected) EmeraldAccent else ArtisticBorder, RoundedCornerShape(10.dp))
                                .clickable { isJsonSelected = false }
                                .padding(12.dp)
                                .testTag("export_format_csv"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CSV Format",
                                    color = if (!isJsonSelected) EmeraldAccent else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Spreadsheet Friendly (.csv)",
                                    color = CoolGrayMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 2. SELECT SCOPE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. Select Backup Scope:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Workspace capsule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (useActiveWorkspaceOnly) EmeraldAccent.copy(alpha = 0.12f) else ObsidianBg)
                                .border(1.dp, if (useActiveWorkspaceOnly) EmeraldAccent else ArtisticBorder, RoundedCornerShape(10.dp))
                                .clickable { useActiveWorkspaceOnly = true }
                                .padding(12.dp)
                                .testTag("export_scope_active"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Active Folder",
                                    color = if (useActiveWorkspaceOnly) EmeraldAccent else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Selected Workspace",
                                    color = CoolGrayMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Full Vault capsule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!useActiveWorkspaceOnly) EmeraldAccent.copy(alpha = 0.12f) else ObsidianBg)
                                .border(1.dp, if (!useActiveWorkspaceOnly) EmeraldAccent else ArtisticBorder, RoundedCornerShape(10.dp))
                                .clickable { useActiveWorkspaceOnly = false }
                                .padding(12.dp)
                                .testTag("export_scope_all"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Full Vault",
                                    color = if (!useActiveWorkspaceOnly) EmeraldAccent else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All Project Prompts",
                                    color = CoolGrayMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Count Indicator / Preview details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, ArtisticBorder)
                        .padding(12.dp)
                ) {
                    if (isQueryingCount) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("Calculating prompt backup volume...", color = CoolGrayMuted, fontSize = 11.sp)
                        }
                    } else {
                        Column {
                            Text(
                                text = "Backup Target Volume: $loadedCount prompts",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (loadedCount > 0) "Your backup file is compiled and ready for instant dispatch." else "No prompts found inside this scope to compile.",
                                color = CoolGrayMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                if (loadedCount > 0) {
                    HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    // 3. ACTION OPTIONS
                    Text(
                        text = "3. Select Export Destination:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Button 1: Copy String Content
                    OutlinedButton(
                        onClick = {
                            val backupText = if (isJsonSelected) {
                                viewModel.generateJsonBackup(promptsToExport)
                            } else {
                                viewModel.generateCsvBackup(promptsToExport)
                            }
                            clipboardManager.setText(AnnotatedString(backupText))
                            viewModel.showToast("Copied Backup Data to Clipboard! 📋")
                            if (hapticCopyToClipboard) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        border = BorderStroke(1.dp, ArtisticBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_action_copy")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
                            Text("Quick Copy String to Clipboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button 2: Save File to Downloads
                    OutlinedButton(
                        onClick = {
                            val backupText = if (isJsonSelected) {
                                viewModel.generateJsonBackup(promptsToExport)
                            } else {
                                viewModel.generateCsvBackup(promptsToExport)
                            }
                            val suffix = if (isJsonSelected) "json" else "csv"
                            val filename = "PromptVault_Backup_${System.currentTimeMillis()}.$suffix"
                            val mimeType = if (isJsonSelected) "application/json" else "text/csv"
                            
                            saveFileToExternalStorage(context, filename, backupText, mimeType)
                        },
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_action_save")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldAccent)
                            Text("Download File to Downloads Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button 3: Send Intent Share Sheet
                    Button(
                        onClick = {
                            val backupText = if (isJsonSelected) {
                                viewModel.generateJsonBackup(promptsToExport)
                            } else {
                                viewModel.generateCsvBackup(promptsToExport)
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TITLE, "Backup-Vault-Prompts")
                                putExtra(android.content.Intent.EXTRA_TEXT, backupText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Backup File Outside"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_action_share")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share Backup File via System App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss Backup Manager", color = EmeraldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// MediaStore helper to write the file directly to system downloads folder
private fun saveFileToExternalStorage(
    context: android.content.Context,
    filename: String,
    content: String,
    mimeType: String
) {
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            }
            Toast.makeText(context, "Saved file Successfully to Download Folder!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to create destination uri", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed writing file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ==========================================
// PREMIUM SYSTEM: PAYWALLS & COLLABORATIVE LAB
// ==========================================

@Composable
fun PremiumPaywallDialog(
    feature: String,
    onClose: () -> Unit,
    onUpgrade: () -> Unit
) {
    val context = LocalContext.current
    val title = when (feature) {
        "multiplayer_collaboration" -> "Unlock Collaborative Multiplayer"
        "team_folders" -> "Unlock Shared Team Folders"
        "premium_ai_tools" -> "Unlock Advanced AI Developer Suite"
        else -> "Unlock Premium Pro Upgrade"
    }
    
    val featureDescription = when (feature) {
        "multiplayer_collaboration" -> "Enable instant team synchronization, live seat availability check, and collaborative multi-user editing canvas."
        "team_folders" -> "Share prompts securely within isolated folder hierarchies, with custom access roles (Admin, Editor, Viewer)."
        "premium_ai_tools" -> "Unleash multi-step automated refactoring, compiler optimization loops, and unlimited Gemini 3 Pro/Ultra context blocks."
        else -> "Gain complete access to multiplayer, unlimited custom agents, and high-priority Gemini models."
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFFE2B840), RoundedCornerShape(24.dp)).testTag("premium_paywall_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFE2B840),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2B840).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFE2B840).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Access Restricted: Under Free Tier Limits",
                            color = Color(0xFFE2B840),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = featureDescription,
                            color = LightGrayText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "With Premium Pro, you receive:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val items = listOf(
                        "Workspace capacity check: Socket seating connects cleanly prior to live editing." to Icons.Default.Group,
                        "Advanced AI Routing: Eliminates token limits with prioritized Gemini 3 Ultra blocks." to Icons.Default.Troubleshoot,
                        "Multi-file self-repairing loops: Self-fixing code compiler prompts." to Icons.Default.Loop,
                        "Shared folder directory control: Granular permissions per teammate." to Icons.Default.FolderShared
                    )

                    for ((text, icon) in items) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Text(
                                text = text,
                                color = CoolGrayMuted,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpgrade()
                    Toast.makeText(context, "Successfully upgraded account to Premium Pro! 🚀", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B840)),
                modifier = Modifier.testTag("paywall_upgrade_button")
            ) {
                Text("Upgrade Instantly ($15/mo)", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Not Now", color = CoolGrayMuted, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun PremiumLabDialog(
    userTier: String,
    onClose: () -> Unit,
    onUpgrade: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("coop") } // "coop" (Team Collaboration) or "ai" (Advanced AI)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBackground, // Clean solid card #171A20
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                border = BorderStroke(1.dp, ArtisticBorder), // Premium soft border #1F2228
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("premium_lab_active_hub"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldAccent // Elegantly tint with #A8B7C6
                    )
                    Text(
                        text = "Multiplayer & AI Dev Lab",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "PREMIUM HUB",
                        color = EmeraldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Clean Top Tab-Toggle Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArtisticNavBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: Cooperation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == "coop") ArtisticActivePill else Color.Transparent)
                            .border(
                                width = if (activeTab == "coop") 1.dp else 0.dp,
                                color = if (activeTab == "coop") EmeraldAccent.copy(alpha = 0.25f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = "coop" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👥 Team Collaboration",
                                color = if (activeTab == "coop") Color.White else CoolGrayMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tab 2: AI Compiler
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == "ai") ArtisticActivePill else Color.Transparent)
                            .border(
                                width = if (activeTab == "ai") 1.dp else 0.dp,
                                color = if (activeTab == "ai") EmeraldAccent.copy(alpha = 0.25f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = "ai" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ Advanced AI",
                                color = if (activeTab == "ai") Color.White else CoolGrayMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Beautiful Centered Screen Highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ArtisticNavBg)
                        .border(1.dp, ArtisticBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeTab == "coop") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sleek programmatic drawing or icon representation of collaborative folders
                            Canvas(modifier = Modifier.size(56.dp).padding(bottom = 8.dp)) {
                                val w = size.width
                                val h = size.height
                                val path = androidx.compose.ui.graphics.Path().apply {
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
                                    color = EmeraldAccent.copy(alpha = 0.12f),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )
                                drawPath(
                                    path = path,
                                    color = EmeraldAccent,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                                )
                                drawCircle(
                                    color = EmeraldAccent,
                                    radius = 6.dp.toPx(),
                                    center = Offset(36.dp.toPx(), 32.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = Offset(36.dp.toPx(), 32.dp.toPx()),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unlock Collaborative Prompt Folders",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Work seamlessly with your team under shared secure workspaces.",
                                color = CoolGrayMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sleek tag indicating brand-color active
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.12f))
                                    .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Gemini 3 Ultra Active",
                                    color = EmeraldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Access Deep-Code Prompt Optimization",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Run automated, multi-step prompt expansions with high-context models.",
                                color = CoolGrayMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Premium Paywall Action Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ultra premium upgrade button with solid, high-contrast 브랜드 컬러 (EmeraldAccent / #A8B7C6)
                    Button(
                        onClick = {
                            onUpgrade()
                            Toast.makeText(context, "Upgraded to Premium Pro successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("intercept_upgrade_button"),
                    ) {
                        Text(
                            text = "Upgrade to Premium Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Secondary dismiss button
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Close Hub",
                            color = CoolGrayMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

// Programmatic Custom Neon Empty State Illustration
@Composable
fun NeonEmptyIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(160.dp)) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Soft violet ambient radial backing glow
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFF381E72).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                radius = width * 0.5f
            )
        )

        // Concentric orbit neon lines
        drawCircle(
            color = Color(0xFFD0BCFF).copy(alpha = 0.12f),
            radius = width * 0.42f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        )
        drawCircle(
            color = Color(0xFF80D0C7).copy(alpha = 0.18f),
            radius = width * 0.32f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        )

        // Floating little sparkle sparks (Star details)
        // spark 1
        drawLine(
            color = Color(0xFF80D0C7).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(centerX - 42.dp.toPx(), centerY - 38.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(centerX - 32.dp.toPx(), centerY - 38.dp.toPx()),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF80D0C7).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(centerX - 37.dp.toPx(), centerY - 43.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(centerX - 37.dp.toPx(), centerY - 33.dp.toPx()),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // spark 2
        drawLine(
            color = Color(0xFFD0BCFF).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(centerX + 40.dp.toPx(), centerY + 30.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(centerX + 50.dp.toPx(), centerY + 30.dp.toPx()),
            strokeWidth = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFD0BCFF).copy(alpha = 0.8f),
            start = androidx.compose.ui.geometry.Offset(centerX + 45.dp.toPx(), centerY + 25.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(centerX + 45.dp.toPx(), centerY + 35.dp.toPx()),
            strokeWidth = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Folder outline drawing path
        val folderPath = androidx.compose.ui.graphics.Path().apply {
            val startX = centerX - 40.dp.toPx()
            val startY = centerY - 20.dp.toPx()
            val endX = centerX + 40.dp.toPx()
            val endY = centerY + 30.dp.toPx()
            val tabW = 26.dp.toPx()
            val tabH = 8.dp.toPx()

            moveTo(startX, startY)
            lineTo(startX + tabW, startY)
            lineTo(startX + tabW + 6.dp.toPx(), startY + tabH)
            lineTo(endX - 6.dp.toPx(), startY + tabH)
            quadraticTo(endX, startY + tabH, endX, startY + tabH + 6.dp.toPx())
            lineTo(endX, endY - 6.dp.toPx())
            quadraticTo(endX, endY, endX - 6.dp.toPx(), endY)
            lineTo(startX + 6.dp.toPx(), endY)
            quadraticTo(startX, endY, startX, endY - 6.dp.toPx())
            lineTo(startX, startY + 6.dp.toPx())
            quadraticTo(startX, startY, startX + 6.dp.toPx(), startY)
            close()
        }

        // Draw translucent folder background backing
        drawPath(
            path = folderPath,
            color = Color(0xFF2B2930).copy(alpha = 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Double layered Neon lines
        drawPath(
            path = folderPath,
            color = Color(0xFF381E72),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        drawPath(
            path = folderPath,
            color = Color(0xFFD0BCFF).copy(alpha = 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )

        // Glow lock center piece
        val lockW = 18.dp.toPx()
        val lockH = 13.dp.toPx()
        val lockX = centerX - lockW / 2
        val lockY = centerY - 3.dp.toPx()

        // Shackle
        val shackleP = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 5.dp.toPx(), lockY)
            lineTo(centerX - 5.dp.toPx(), lockY - 6.dp.toPx())
            quadraticTo(centerX, lockY - 11.dp.toPx(), centerX + 5.dp.toPx(), lockY - 6.dp.toPx())
            lineTo(centerX + 5.dp.toPx(), lockY)
        }
        drawPath(
            path = shackleP,
            color = Color(0xFF80D0C7),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Lock body (rounded container)
        drawRoundRect(
            color = Color(0xFF80D0C7),
            topLeft = androidx.compose.ui.geometry.Offset(lockX, lockY),
            size = androidx.compose.ui.geometry.Size(lockW, lockH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Lock keyhole center
        drawCircle(
            color = Color(0xFF1C1B1F),
            radius = 2.2f.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(centerX, lockY + 5.dp.toPx())
        )
        drawLine(
            color = Color(0xFF1C1B1F),
            start = androidx.compose.ui.geometry.Offset(centerX, lockY + 5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(centerX, lockY + 9.dp.toPx()),
            strokeWidth = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// Sidebar Drawer Tree-view Content
@Composable
fun SidebarContent(
    categories: List<CategoryEntity>,
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: Int,
    selectedCategory: String?,
    userTierState: String,
    currentSubPage: SubPage,
    onSubPageSelect: (SubPage) -> Unit,
    onWorkspaceSelect: (Int) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddSubCategory: (String, Int) -> Unit,
    onNavigateToPricing: () -> Unit
) {
    var showCreateSubFolderDialog by remember { mutableStateOf(false) }
    var selectedParentIdForNewSubFolder by remember { mutableStateOf<Int?>(null) }
    var newSubFolderName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtisticNavBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Sidebar header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
        ) {
            PromptVaultLogo(
                modifier = Modifier.size(36.dp)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Prompt",
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Light,
                        fontSize = 17.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Vault",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = "CLOUDFILE ENGINE v4",
                    color = CoolGrayMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Multi-page System Navigation Modules
        Text(
            text = "SYSTEM APPLICATIONS",
            color = CoolGrayMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            val navHubItems = listOf(
                Triple(SubPage.Dashboard, "Dashboard Overview", Icons.Default.Dashboard),
                Triple(SubPage.Library, "Formula Library", Icons.Default.FolderOpen),
                Triple(SubPage.Templates, "Deploy Templates", Icons.Default.Extension),
                Triple(SubPage.Analytics, "Telemetry Analytics", Icons.Default.TrendingUp),
                Triple(SubPage.Activity, "Recent Logs", Icons.Default.History),
                Triple(SubPage.Settings, "Linear Settings", Icons.Default.Settings)
            )

            for (item in navHubItems) {
                val isSelected = currentSubPage == item.first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) EmeraldAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSubPageSelect(item.first) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = item.third,
                        contentDescription = null,
                        tint = if (isSelected) EmeraldAccent else CoolGrayMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = item.second,
                        color = if (isSelected) Color.White else LightGrayText.copy(alpha = 0.85f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // Workspaces list
        Text(
            text = "COLLABORATIVE WORKSPACES",
            color = CoolGrayMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (ws in workspaces) {
                val isSelected = selectedWorkspaceId == ws.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ArtisticActivePill else Color.Transparent)
                        .clickable { onWorkspaceSelect(ws.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (ws.iconName) {
                            "campaign" -> Icons.Default.Campaign
                            "code" -> Icons.Default.Code
                            else -> Icons.Default.FolderShared
                        },
                        contentDescription = null,
                        tint = if (isSelected) EmeraldAccent else CoolGrayMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = ws.name,
                        color = if (isSelected) Color.White else LightGrayText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Hierarchical Categories (Nested Folders)
        Text(
            text = "HIERARCHICAL DIRECTORIES",
            color = CoolGrayMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // All option
        val isAllSelected = selectedCategory == "All"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAllSelected) ArtisticActivePill else Color.Transparent)
                .clickable { onCategorySelect("All") }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = if (isAllSelected) EmeraldAccent else CoolGrayMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Show All Prompts",
                color = if (isAllSelected) Color.White else LightGrayText,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }

        // Favorites Option
        val isFavoritesSelected = selectedCategory == "Favorites"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFavoritesSelected) ArtisticActivePill else Color.Transparent)
                .clickable { onCategorySelect("Favorites") }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (isFavoritesSelected) EmeraldAccent else CoolGrayMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Show Favorites Only",
                color = if (isFavoritesSelected) Color.White else LightGrayText,
                fontWeight = if (isFavoritesSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }

        val topLevelCategories = categories.filter { it.parentId == null }
        val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (parentCat in topLevelCategories) {
                val isParentSel = selectedCategory == parentCat.name
                val isExpanded = expandedStates[parentCat.id] ?: false
                val children = categories.filter { it.parentId == parentCat.id }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isParentSel) ArtisticActivePill else Color.Transparent)
                            .clickable {
                                onCategorySelect(parentCat.name)
                                expandedStates[parentCat.id] = !isExpanded
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isParentSel) EmeraldAccent else Color(0xFFC084FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = parentCat.name,
                            color = if (isParentSel) Color.White else LightGrayText,
                            fontWeight = if (isParentSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add nested directory",
                                tint = CoolGrayMuted.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        selectedParentIdForNewSubFolder = parentCat.id
                                        showCreateSubFolderDialog = true
                                    }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (children.isNotEmpty()) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = CoolGrayMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Nested children drawings with Indent & branch line
                    if (isExpanded && children.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(start = 22.dp)
                        ) {
                            for (child in children) {
                                val isChildSel = selectedCategory == child.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChildSel) ArtisticActivePill else Color.Transparent)
                                        .clickable { onCategorySelect(child.name) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Custom branch-like line mark
                                    Canvas(modifier = Modifier.size(8.dp, 12.dp)) {
                                        drawLine(
                                            color = Color(0xFF49454F),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 2f
                                        )
                                        drawLine(
                                            color = Color(0xFF49454F),
                                            start = Offset(0f, size.height / 2),
                                            end = Offset(size.width, size.height / 2),
                                            strokeWidth = 2f
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.SnippetFolder,
                                        contentDescription = null,
                                        tint = if (isChildSel) EmeraldAccent else Color(0xFF80D0C7),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = child.name,
                                        color = if (isChildSel) Color.White else CoolGrayMuted,
                                        fontWeight = if (isChildSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sub Folder Add Dialog
        if (showCreateSubFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateSubFolderDialog = false },
                containerColor = CardBackground,
                title = { Text("Create Nested Folder", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Adding nested directory inside: ${topLevelCategories.find { it.id == selectedParentIdForNewSubFolder }?.name ?: ""}",
                            color = CoolGrayMuted,
                            fontSize = 12.sp
                        )
                        TextField(
                            value = newSubFolderName,
                            onValueChange = { newSubFolderName = it },
                            placeholder = { Text("e.g. Hooks, Social, Coding") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = ArtisticBorder,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val parentId = selectedParentIdForNewSubFolder
                            if (newSubFolderName.isNotBlank() && parentId != null) {
                                onAddSubCategory(newSubFolderName, parentId)
                                showCreateSubFolderDialog = false
                                newSubFolderName = ""
                                expandedStates[parentId] = true // auto expand!
                            }
                        }
                    ) {
                        Text("Add Nested Folder", color = EmeraldAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateSubFolderDialog = false }) {
                        Text("Cancel", color = CoolGrayMuted)
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardWidget(usageLogs: List<PromptUsageEntity>) {
    // Group the logs by day for the last 30 days
    val dailyCounts = remember(usageLogs) {
        val counts = IntArray(30)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        for (log in usageLogs) {
            val daysAgo = ((now - log.timestamp) / oneDayMs).toInt()
            if (daysAgo in 0..29) {
                counts[29 - daysAgo]++ // Index from oldest (29 days ago) to newest (today)
            }
        }
        counts.toList()
    }

    val totalInteractions = dailyCounts.sum()

    // Glassmorphism card for the sparkline chart
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .glassmorphicContainer(RoundedCornerShape(16.dp))
            .testTag("dashboard_usage_widget"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "30-DAY PROMPT FREQUENCY",
                        color = Color(0xFFC084FC), // Spark violet
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalInteractions Interactions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Recharts Engine",
                        color = Color(0xFF80D0C7),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas sparkline chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val pointsCount = dailyCounts.size
                    if (pointsCount < 2) return@Canvas

                    val maxVal = (dailyCounts.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
                    val xInterval = width / (pointsCount - 1)

                    val path = Path()
                    val fillPath = Path()

                    val firstY = height - (dailyCounts[0] / maxVal) * (height - 10f) - 5f
                    path.moveTo(0f, firstY)
                    fillPath.moveTo(0f, height)
                    fillPath.lineTo(0f, firstY)

                    for (i in 1 until pointsCount) {
                        val x = i * xInterval
                        val y = height - (dailyCounts[i] / maxVal) * (height - 10f) - 5f
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    // Draw the smooth filled gradient below sparkline
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                Color(0xFF8B5CF6).copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Draw the primary sparkline curve
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFC084FC), Color(0xFF818CF8), Color(0xFF34D399))
                        ),
                        style = Stroke(
                            width = 2.5f.dp.toPx(),
                            pathEffect = PathEffect.cornerPathEffect(15f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw grid/target indicator dots representing recent active peaks
                    dailyCounts.forEachIndexed { index, count ->
                        if (count > 0 && index % 3 == 0) {
                            val cx = index * xInterval
                            val cy = height - (count / maxVal) * (height - 10f) - 5f
                            drawCircle(
                                color = Color(0xFF34D399).copy(alpha = 0.4f),
                                radius = 4f.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.5f.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer with period info and sparkline limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30 days ago",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Live stats summary dots representation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFC084FC)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copies", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edits", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                    }
                }

                Text(
                    text = "Today",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =========================================================================
// PREMIUM PRODUCT SUITE - MULTI-PAGE SaaS SCREEN COMPOSABLES
// =========================================================================

@Composable
fun DashboardSubPage(
    prompts: List<PromptEntity>,
    favorites: List<PromptEntity>,
    usageLogs: List<PromptUsageEntity>,
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: Int,
    onNavigateToSubPage: (SubPage) -> Unit,
    onCreatePromptClick: () -> Unit,
    onSelectPrompt: (PromptEntity) -> Unit,
    onQuickCopy: (String) -> Unit,
    userTier: String,
    onUpgradeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome and Quick Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vault Overview",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Encrypted project workspace analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoolGrayMuted
                    )
                }

                Button(
                    onClick = onCreatePromptClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Prompt", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Modular KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Prompts
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSubPage(SubPage.Library) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Total Prompts", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = prompts.size.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Favorites
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSubPage(SubPage.Favorites) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pinned Items", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = favorites.size.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Activity Insights
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSubPage(SubPage.Activity) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actions", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = usageLogs.size.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Live Sparkline Chart Block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, ArtisticBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Interact Volume Trend",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rolling 30-day interactive sync velocity logs",
                        color = CoolGrayMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DashboardWidget(usageLogs = usageLogs)
                }
            }
        }

        // Quick Actions Dashboard Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, ArtisticBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Quick Command Center",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable { onNavigateToSubPage(SubPage.Library) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldAccent)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Workspace", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable { onNavigateToSubPage(SubPage.Templates) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanAccent)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Templates", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable { onNavigateToSubPage(SubPage.Settings) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldAccent)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Settings", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Recent Prompts Quick-View (Max 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Added Projects",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
                TextButton(onClick = { onNavigateToSubPage(SubPage.Library) }) {
                    Text("View Vault", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (prompts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No prompt entries. Deploy a prompt templates package or click New Prompt to get started.", color = CoolGrayMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(prompts.take(3)) { prompt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(prompt) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prompt.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(prompt.description, color = CoolGrayMuted, fontSize = 10.sp, maxLines = 1)
                        }

                        IconButton(
                            onClick = { onQuickCopy(prompt.content) }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Content", tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySubPage(
    prompts: List<PromptEntity>,
    categories: List<CategoryEntity>,
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: Int,
    selectedCategory: String?,
    searchQuery: String,
    sortBy: String,
    currentUserRole: String,
    userTierState: String,
    hapticSwipeToDelete: Boolean,
    onWorkspaceSelect: (Int) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onSelectPrompt: (PromptEntity) -> Unit,
    onQuickCopy: (String) -> Unit,
    onEditPrompt: (PromptEntity) -> Unit,
    onDeletePrompt: (PromptEntity) -> Unit,
    onToggleFavorite: (PromptEntity) -> Unit,
    onUpgradeDialog: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Filter Block
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text("Search instructions, titles, category hooks...", color = CoolGrayMuted, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = ArtisticBorder,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Workspaces selector horizontally in Library
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (ws in workspaces) {
                val isSelected = selectedWorkspaceId == ws.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) EmeraldAccent else CardBackground)
                        .clickable {
                            if (ws.id != 0 && userTierState == "Free") {
                                onUpgradeDialog("team_folders")
                            } else {
                                onWorkspaceSelect(ws.id)
                            }
                        }
                        .border(1.dp, if (isSelected) EmeraldAccent else ArtisticBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = ws.name,
                        color = if (isSelected) ObsidianBg else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Categories selector
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedCategory == null) CyanAccent else CardBackground)
                    .clickable { onCategorySelect(null) }
                    .border(1.dp, if (selectedCategory == null) CyanAccent else ArtisticBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("All Categories", color = if (selectedCategory == null) ObsidianBg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            for (cat in categories.map { it.name }.distinct()) {
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) CyanAccent else CardBackground)
                        .clickable { onCategorySelect(cat) }
                        .border(1.dp, if (isSel) CyanAccent else ArtisticBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(cat, color = if (isSel) ObsidianBg else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Grid stream
        if (prompts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vault directory is clear.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No prompt project items matched the criteria filter.", color = CoolGrayMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(prompts) { prompt ->
                    SwipeToActionWrapper(
                        onSwipeRight = {
                            onToggleFavorite(prompt)
                            Toast.makeText(context, if (prompt.isFavorite) "Removed Pin" else "Pinned Prompt!", Toast.LENGTH_SHORT).show()
                        },
                        onSwipeLeft = {
                            if (currentUserRole != "Viewer") {
                                onDeletePrompt(prompt)
                                Toast.makeText(context, "Removed Prompt Projects", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Viewers can't delete items", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isFavorite = prompt.isFavorite,
                        hapticFeedbackEnabled = hapticSwipeToDelete
                    ) {
                        PromptItemCard(
                            prompt = prompt,
                            onSelect = {
                                if (prompt.isPremiumTool && userTierState == "Free") {
                                    onUpgradeDialog("premium_ai_tools")
                                } else {
                                    onSelectPrompt(prompt)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(prompt) },
                            onQuickCopy = {
                                if (prompt.isPremiumTool && userTierState == "Free") {
                                    onUpgradeDialog("premium_ai_tools")
                                } else {
                                    onQuickCopy(prompt.content)
                                }
                            },
                            onEdit = {
                                if (prompt.isPremiumTool && userTierState == "Free") {
                                    onUpgradeDialog("premium_ai_tools")
                                } else {
                                    onEditPrompt(prompt)
                                }
                            },
                            onDelete = { onDeletePrompt(prompt) },
                            isEditable = (currentUserRole != "Viewer"),
                            categories = categories
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesSubPage(
    prompts: List<PromptEntity>,
    categories: List<CategoryEntity>,
    onSelectCategory: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Workspace Classifications",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Structure prompt vaults based on specific classifications",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        val hardcodedCategories = listOf("Marketing", "SEO", "Coding", "Sales", "Business", "Research", "Writing")
        val availableNames = (categories.map { it.name } + hardcodedCategories).distinct()

        items(availableNames) { catName ->
            val count = prompts.count { it.category.equals(catName, ignoreCase = true) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCategory(catName) },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, ArtisticBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (catName) {
                                    "Coding" -> Icons.Default.Code
                                    "Marketing" -> Icons.Default.Campaign
                                    else -> Icons.Default.Layers
                                },
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(catName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$count project formulas deployed", color = CoolGrayMuted, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Active", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesSubPage(
    favorites: List<PromptEntity>,
    categories: List<CategoryEntity>,
    onSelectPrompt: (PromptEntity) -> Unit,
    onQuickCopy: (String) -> Unit,
    onToggleFavorite: (PromptEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Pinned Favorites Directory",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Quick access repository of high frequency deployment tags",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        if (favorites.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = CoolGrayMuted.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Pinned Favorites Locked", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Swipe right on item card inside Library to highlight tags", color = CoolGrayMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(favorites) { prompt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(prompt) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E1E2D))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(prompt.category.uppercase(), color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = { onToggleFavorite(prompt) }) {
                                    Icon(Icons.Default.Star, contentDescription = "Unfav", tint = CyanAccent, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onQuickCopy(prompt.content) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(prompt.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(prompt.description, color = CoolGrayMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsSubPage(
    prompts: List<PromptEntity>,
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: Int,
    onWorkspaceSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Collection Spaces",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Segregated deployment libraries with restricted security scopes",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        items(workspaces) { ws ->
            val isCurrent = ws.id == selectedWorkspaceId
            val wsCount = prompts.count { it.workspaceId == ws.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWorkspaceSelect(ws.id) },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, if (isCurrent) EmeraldAccent else ArtisticBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        }

                        Column {
                            Text(ws.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (ws.id == 0) "Personal Local Safe" else "Shared Workspace Workspace", color = CoolGrayMuted, fontSize = 10.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("$wsCount Items", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (isCurrent) {
                            Text("Active Space", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatesSubPage(
    onCreateFromTemplate: (String, String, String, String) -> Unit
) {
    val templatesList = listOf(
        Triple("SEO Article Optimizer Pro", "Enforces deep semantic optimization frameworks targeting search vectors.", "Deploying optimization protocols to target structural indexing schemas..."),
        Triple("Executive Code Generator", "Refactors raw descriptions to clean modular design layout vectors.", "Creating highly typed components utilizing clean standard methodologies..."),
        Triple("Copywriting Hook Creator", "Utilizes AIDA frameworks targeting digital conversion pathways.", "Write an engaging AIDA conversion script centering the value product of...")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Curated Template Library",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "One-click deployment templates generated by prompt architects",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        items(templatesList) { temp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, ArtisticBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(temp.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(temp.second, color = CoolGrayMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onCreateFromTemplate(temp.first, temp.second, temp.third, "Templates") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Deploy Template", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsSubPage(
    prompts: List<PromptEntity>,
    favorites: List<PromptEntity>,
    usageLogs: List<PromptUsageEntity>
) {
    // 1. Process 30-day activity trend
    val dailyCounts = remember(usageLogs) {
        val counts = IntArray(30)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        for (log in usageLogs) {
            val daysAgo = ((now - log.timestamp) / oneDayMs).toInt()
            if (daysAgo in 0..29) {
                counts[29 - daysAgo]++
            }
        }
        counts.toList()
    }
    val totalExecutions = usageLogs.size
    val peakActivityValue = dailyCounts.maxOrNull() ?: 1
    val averageActivityValue = if (dailyCounts.isNotEmpty()) dailyCounts.average() else 0.0

    // 2. Process hierarchical directory proportions
    val categoryCounts = remember(prompts) {
        prompts.groupBy { it.category }.mapValues { it.value.size }
    }
    val totalPromptsCount = prompts.size.coerceAtLeast(1)

    // Layout configuration
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Telemetry header
        item {
            Column {
                Text(
                    text = "Telemetry & Diagnostics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Live telemetry of formula executions, folder usage ratios, and system commits",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrayMuted
                )
            }
        }

        // Row 1: KPI Bento Summaries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Volume card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("FORMULA RATIOS", color = CoolGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(prompts.size.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Active compilation keys", color = EmeraldAccent, fontSize = 9.sp)
                    }
                }

                // Deployments card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("TOTAL DEPLOYMENTS", color = CoolGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(totalExecutions.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Execution actions (30d)", color = EmeraldAccent, fontSize = 9.sp)
                    }
                }

                // Concurrency card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("VELOCITY RATIO", color = CoolGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(String.format(Locale.US, "%.1f", averageActivityValue), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Launches / 24-hr period", color = EmeraldAccent, fontSize = 9.sp)
                    }
                }
            }
        }

        // Row 2: Premium Interactive Usage Sparkline with high-contrast diagnostics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, ArtisticBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DEPLOYMENT INTERACTIONS TELEMETRY", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Text("Interactive Execution Trends (30-day spectrum)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(EmeraldAccent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("PEAK: $peakActivityValue HITS", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Custom Line chart with background grid overlay and coordinates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val maxVal = peakActivityValue.coerceAtLeast(1).toFloat()
                            val points = dailyCounts.size

                            if (points >= 2) {
                                val dx = w / (points - 1)

                                // Draw subtle background grid lines
                                val gridUnits = 5
                                for (gi in 0..gridUnits) {
                                        val gy = h * gi / gridUnits
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.05f),
                                            start = Offset(0f, gy),
                                            end = Offset(w, gy),
                                            strokeWidth = 1f
                                        )
                                }

                                // Create line curve and fill paths
                                val linePath = Path()
                                val fillGradientPath = Path()

                                val firstX = 0f
                                val firstY = h - (dailyCounts[0] / maxVal) * (h - 15f) - 5f
                                linePath.moveTo(firstX, firstY)
                                fillGradientPath.moveTo(firstX, h)
                                fillGradientPath.lineTo(firstX, firstY)

                                for (pi in 1 until points) {
                                    val px = pi * dx
                                    val py = h - (dailyCounts[pi] / maxVal) * (h - 15f) - 5f
                                    linePath.lineTo(px, py)
                                    fillGradientPath.lineTo(px, py)
                                }

                                fillGradientPath.lineTo(w, h)
                                fillGradientPath.close()

                                // Draw the dynamic trend filling gradient
                                drawPath(
                                    path = fillGradientPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            EmeraldAccent.copy(alpha = 0.22f),
                                            Color.Transparent
                                        )
                                    )
                                )

                                // Draw the main stroke line
                                drawPath(
                                    path = linePath,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            EmeraldAccent.copy(alpha = 0.5f),
                                            EmeraldAccent,
                                            EmeraldAccent.copy(alpha = 0.5f)
                                        )
                                    ),
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.cornerPathEffect(24f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Highlight peak points with localized coordinates
                                dailyCounts.forEachIndexed { index, count ->
                                    if (count == peakActivityValue) {
                                        val cx = index * dx
                                        val cy = h - (count / maxVal) * (h - 15f) - 5f
                                        drawCircle(
                                            color = EmeraldAccent.copy(alpha = 0.35f),
                                            radius = 10f.dp.toPx(),
                                            center = Offset(cx, cy)
                                        )
                                        drawCircle(
                                            color = EmeraldAccent,
                                            radius = 5f.dp.toPx(),
                                            center = Offset(cx, cy)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 2f.dp.toPx(),
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sparkline axis labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("30 DAYS AGO", color = CoolGrayMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("DEPLOYMENT WAVE PROFILE", color = CoolGrayMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("TODAY", color = CoolGrayMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Row 3: Custom Category Donut Breakout + Engagement Commitment Intensity Heat Grid
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Section left: Directory Distribution
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DIRECTORY DENSITY PROFILE", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Text("Proportional layout of workspace elements", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryCounts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No formula categories compiled yet.", color = CoolGrayMuted, fontSize = 11.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Premium circular donut chart with Custom Canvas details
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        var currentAngle = -90f
                                        val strokeWidth = 14f.dp.toPx()

                                        categoryCounts.forEach { (catName, count) ->
                                            val sweep = (count.toFloat() / totalPromptsCount) * 360f
                                            val catColors = getCategoryColors(catName)
                                            drawArc(
                                                color = catColors.first,
                                                startAngle = currentAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                            currentAngle += sweep
                                        }
                                    }

                                    // Center of the Donut
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = prompts.size.toString(),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Keys",
                                            color = CoolGrayMuted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Legend listing categories with specific progress bars
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categoryCounts.entries.take(4).forEach { (catName, count) ->
                                        val percentage = (count.toFloat() / totalPromptsCount)
                                        val catColors = getCategoryColors(catName)

                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(catColors.first)
                                                    )
                                                    Text(
                                                        text = catName,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "${(percentage * 100).toInt()}%",
                                                    color = CoolGrayMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))

                                            // Styled bar representation
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(percentage)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(catColors.first)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 30-Day developer engagement commitment tracker heatmap card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SYSTEM DEPLOYMENT ENGAGEMENT GRIDS", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Text("Tactile Commit Heatmap (Last 30-day index intensity)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render 30 individual square intensity blocks representing activity logs
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // 3 rows representing blocks
                                val itemsPerRow = 10
                                for (rowIndex in 0..2) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (colIndex in 0 until itemsPerRow) {
                                            val dayId = rowIndex * itemsPerRow + colIndex
                                            if (dayId in dailyCounts.indices) {
                                                val count = dailyCounts[dayId]
                                                // Map count to beautiful opacity states representing committing intensities
                                                val intensityColor = when {
                                                    count == 0 -> Color.White.copy(alpha = 0.04f)
                                                    count == 1 -> EmeraldAccent.copy(alpha = 0.2f)
                                                    count == 2 -> EmeraldAccent.copy(alpha = 0.45f)
                                                    count == 3 -> EmeraldAccent.copy(alpha = 0.7f)
                                                    else -> EmeraldAccent
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(intensityColor)
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (count > 0) EmeraldAccent.copy(alpha = 0.5f) else Color.Transparent,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                ) {
                                                    // Optional count label centered inside squares for tactile feeling
                                                    if (count > 0) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = count.toString(),
                                                                color = ObsidianBg,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Small legend in standard layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("COMMITS TELEMETRY", color = CoolGrayMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Less", color = CoolGrayMuted, fontSize = 8.sp)
                                val colorsGrid = listOf(
                                    Color.White.copy(alpha = 0.04f),
                                    EmeraldAccent.copy(alpha = 0.2f),
                                    EmeraldAccent.copy(alpha = 0.45f),
                                    EmeraldAccent.copy(alpha = 0.7f),
                                    EmeraldAccent
                                )
                                for (col in colorsGrid) {
                                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(1.dp)).background(col))
                                }
                                Text("More", color = CoolGrayMuted, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamSubPage(
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: Int,
    currentUserRole: String,
    currentWorkspaceMembers: List<WorkspaceMembershipEntity>,
    onInviteMember: (String, String) -> Unit
) {
    var inviteEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Editor") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Security & Collaboration",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Define folder access permissions and invite co-contributors",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        if (selectedWorkspaceId == 0) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Private local vault does not support shared team directories. Please deploy a custom shared folder space.", color = CoolGrayMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Invite Contributor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextField(
                            value = inviteEmail,
                            onValueChange = { inviteEmail = it },
                            placeholder = { Text("email@example.com", fontSize = 11.sp, color = CoolGrayMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = ArtisticBorder,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selectedRole == "Editor") EmeraldAccent else ObsidianBg, RoundedCornerShape(6.dp))
                                    .clickable { selectedRole = "Editor" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Editor", color = if (selectedRole == "Editor") ObsidianBg else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selectedRole == "Viewer") EmeraldAccent else ObsidianBg, RoundedCornerShape(6.dp))
                                    .clickable { selectedRole = "Viewer" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Viewer", color = if (selectedRole == "Viewer") ObsidianBg else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                if (inviteEmail.isNotBlank()) {
                                    onInviteMember(inviteEmail, selectedRole)
                                    inviteEmail = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Invite", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(currentWorkspaceMembers) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(member.userEmail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(member.role, color = CoolGrayMuted, fontSize = 10.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active Invite", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivitySubPage(
    usageLogs: List<PromptUsageEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Interactive Event Stream",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Pruned transactional execution audits matching compile security keys",
                style = MaterialTheme.typography.bodySmall,
                color = CoolGrayMuted
            )
        }

        if (usageLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged in prompt execution telemetry streams.", color = CoolGrayMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(usageLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(EmeraldAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                            }
                            Column {
                                Text(log.type.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Formula sync trigger ID: #${log.id}", color = CoolGrayMuted, fontSize = 10.sp)
                            }
                        }

                        Text(
                            text = "COMPLETED",
                            color = EmeraldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSubPage(
    prompts: List<PromptEntity>,
    categories: List<CategoryEntity>,
    onSelectPrompt: (PromptEntity) -> Unit,
    onQuickCopy: (String) -> Unit
) {
    var searchTxt by remember { mutableStateOf("") }
    val results = remember(searchTxt, prompts) {
        if (searchTxt.isBlank()) emptyList()
        else prompts.filter {
            it.title.contains(searchTxt, ignoreCase = true) ||
                    it.description.contains(searchTxt, ignoreCase = true) ||
                    it.content.contains(searchTxt, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Global Deep Search",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )

        TextField(
            value = searchTxt,
            onValueChange = { searchTxt = it },
            placeholder = { Text("Search instructions, variables, execution tags...", fontSize = 11.sp, color = CoolGrayMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = ArtisticBorder,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground
            ),
            shape = RoundedCornerShape(8.dp)
        )

        if (results.isEmpty() && searchTxt.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No formulas matched search variables", color = CoolGrayMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results) { prompt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectPrompt(prompt) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, ArtisticBorder)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prompt.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(prompt.description, color = CoolGrayMuted, fontSize = 10.sp, maxLines = 1)
                            }
                            IconButton(onClick = { onQuickCopy(prompt.content) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSubPage(
    currentUserEmail: String,
    userTierState: String,
    highContrast: Boolean,
    onHighContrastToggle: () -> Unit,
    hapticSwipeToDelete: Boolean,
    onHapticSwipeToDeleteToggle: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Linear Section Header
        item {
            Column {
                Text(
                    text = "System Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage account, local preferences, workspaces, and service integrations",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrayMuted
                )
            }
        }

        // Section 1: Profile (Account Preferences)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PROFILE IDENTIFICATION",
                    color = CoolGrayMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(EmeraldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUserEmail.take(1).uppercase(),
                                        color = ObsidianBg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                                Column {
                                    Text(currentUserEmail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Primary Organization Owner", color = CoolGrayMuted, fontSize = 11.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(EmeraldAccent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(userTierState.uppercase() + " ACCESS", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        // Member metadata details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ACCOUNT PLAN", color = CoolGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(userTierState, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ORGANIZATION ROUTE", color = CoolGrayMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("com.vault.org.administrator", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Workspace Setup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLLABORATIVE WORKSPACE CONFIG",
                    color = CoolGrayMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Multi-Tenant Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Enable concurrent syncing with secondary workspace members", color = CoolGrayMuted, fontSize = 10.sp)
                            }
                            // Styled Badge representing active status
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("AUTOMATIC", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Subscription Hub", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Release high capacity shared directories and premium auto-conversions", color = CoolGrayMuted, fontSize = 10.sp)
                            }
                            if (userTierState == "Free") {
                                Button(
                                    onClick = onUpgradeClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Upgrade ($9/mo)", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = false
                                ) {
                                    Text("Premium Active", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Appearance Preferences
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VISUAL ENVIRONMENT PREFERENCES",
                    color = CoolGrayMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("High Contrast Grid-Overlays", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Enforces absolute high-contrast border outlines in poor lighting", color = CoolGrayMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = highContrast,
                                onCheckedChange = { onHighContrastToggle() },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent, checkedTrackColor = EmeraldAccent.copy(alpha = 0.5f))
                            )
                        }

                        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Haptic Action Swipes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Triggers localized physical vibrations when swiping list elements", color = CoolGrayMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = hapticSwipeToDelete,
                                onCheckedChange = { onHapticSwipeToDeleteToggle() },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent, checkedTrackColor = EmeraldAccent.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        // Section 4: System Tunnels & Integrations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SYSTEM TUNNELS & INTEGRATIONS",
                    color = CoolGrayMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // DB Client Line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981))
                                )
                                Column {
                                    Text("SQLite Persistent Engine", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Active system connection, localized transactional operations", color = CoolGrayMuted, fontSize = 10.sp)
                                }
                            }
                            Text("CONNECTED", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        // Supabase Line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldAccent)
                                )
                                Column {
                                    Text("Supabase Synchronization Engine", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Main system connection, remote workspace repositories", color = CoolGrayMuted, fontSize = 10.sp)
                                }
                            }
                            Text("SYNCED", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                        // Gemini API line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF06B6D4))
                                )
                                Column {
                                    Text("Gemini Intelligence Tunnel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Natural language compiler for auto-suggestion completions", color = CoolGrayMuted, fontSize = 10.sp)
                                }
                            }
                            Text("ACTIVE", color = Color(0xFF06B6D4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromptDetailPage(
    prompt: PromptEntity,
    categories: List<CategoryEntity>,
    onBackToLibrary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickCopy: () -> Unit,
    onEdit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val dateStr = dateFormatter.format(Date(prompt.createdAt))
    val dateUpdateStr = dateFormatter.format(Date(prompt.updatedAt))

    val wordCount = remember(prompt.content) {
        val trimmed = prompt.content.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
    }
    val charCount = prompt.content.length

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Three-Column Notion-Like Premium Desktop/Tablet Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBackToLibrary) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Return", tint = Color.White)
                        }
                        Text(
                            text = "Workspace / Formula Editor View",
                            color = CoolGrayMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Live connection indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(EmeraldAccent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                        )
                        Text(
                            text = "Live Vault Synced",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = ArtisticBorder.copy(alpha = 0.3f), thickness = 1.dp)

                // Layout Columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: Properties Panel (Metadata Panel)
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, ArtisticBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "PROPERTIES",
                                color = CoolGrayMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            // 1. Category
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(14.dp))
                                    Text("Category", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                val pColors = getCategoryColors(prompt.category)
                                Box(
                                    modifier = Modifier
                                        .background(pColors.first.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = prompt.category.uppercase(),
                                        color = pColors.first,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 2. Access Level
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(14.dp))
                                    Text("Access Restrictions", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (prompt.isPremiumTool) CyanAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (prompt.isPremiumTool) "PREMIUM FORMULA" else "STANDARD ACCESS",
                                        color = if (prompt.isPremiumTool) CyanAccent else Color.White.copy(alpha = 0.7f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 3. User Owner
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(14.dp))
                                    Text("Owner ID", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = prompt.ownerEmail,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 4. Time logs
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = CoolGrayMuted, modifier = Modifier.size(14.dp))
                                    Text("Commit Integrity", color = CoolGrayMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("Committed: $dateStr", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                                Text("Synced: $dateUpdateStr", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Normal)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Character metrics telemetry
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                                border = BorderStroke(1.dp, ArtisticBorder.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("COMPILATION METRICS", color = CoolGrayMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Words:", color = CoolGrayMuted, fontSize = 11.sp)
                                        Text(wordCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Characters:", color = CoolGrayMuted, fontSize = 11.sp)
                                        Text(charCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // CENTER COLUMN: Prime Content Area (Notion Doc Space)
                    Card(
                        modifier = Modifier
                            .weight(2.6f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, ArtisticBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "CORE FORMULA DOCUMENT",
                                color = EmeraldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Title Block
                            Text(
                                text = prompt.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Pull-quote summary layout for Description
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ObsidianBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, ArtisticBorder.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(26.dp)
                                            .background(EmeraldAccent, RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = prompt.description,
                                        color = CoolGrayMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "INSTRUCTION SCHEMA CONTENT",
                                color = CoolGrayMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Text area box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(ObsidianBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, ArtisticBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = prompt.content,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // RIGHT COLUMN: Contextual Actions Panel
                    Card(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, ArtisticBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DECK OPTIONS",
                                color = CoolGrayMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Copy Action
                            Button(
                                onClick = {
                                    onQuickCopy()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Copied Project Formula! ✨", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(14.dp))
                                    Text("Copy Raw", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Favorite Action
                            OutlinedButton(
                                onClick = {
                                    onToggleFavorite()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                border = BorderStroke(1.dp, if (prompt.isFavorite) CyanAccent else ArtisticBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (prompt.isFavorite) CyanAccent.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (prompt.isFavorite) CyanAccent else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (prompt.isFavorite) "Favorited" else "Favorite",
                                        color = if (prompt.isFavorite) CyanAccent else Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Edit Action
                            OutlinedButton(
                                onClick = onEdit,
                                border = BorderStroke(1.dp, ArtisticBorder),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Text("Edit Setup", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Share Action
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, prompt.content)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Formula")
                                    context.startActivity(shareIntent)
                                },
                                border = BorderStroke(1.dp, ArtisticBorder),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Encryption Status Footer
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                                border = BorderStroke(1.dp, ArtisticBorder.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "E2EE AES-GCM V1.2",
                                        color = CoolGrayMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Stacked mobile layout (Vertical viewport)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Navigation / Compact actions strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToLibrary) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Pin Favorite",
                                tint = if (prompt.isFavorite) CyanAccent else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(onClick = {
                            onQuickCopy()
                            Toast.makeText(context, "Formula Copied! ✨", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Quick Copy", tint = EmeraldAccent)
                        }

                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Formula", tint = Color.LightGray)
                        }

                        IconButton(onClick = {
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, prompt.content)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Formula")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.LightGray)
                        }
                    }
                }

                // Notion-style Metadata Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, ArtisticBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val pColors = getCategoryColors(prompt.category)
                            Box(
                                modifier = Modifier
                                    .background(pColors.first.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = prompt.category.uppercase(),
                                    color = pColors.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (prompt.isPremiumTool) {
                                Box(
                                    modifier = Modifier
                                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("PREMIUM", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = prompt.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = prompt.description,
                            color = CoolGrayMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Content area (Mobile)
                Text(
                    text = "PROMPT SCHEMA CONTENT",
                    color = CoolGrayMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, ArtisticBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = prompt.content,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Metatags and telemetry indices (Mobile)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, ArtisticBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "METADATA & PARAMETERS",
                            color = CoolGrayMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Author Profile", color = CoolGrayMuted, fontSize = 10.sp)
                                Text(prompt.ownerEmail, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Words Count", color = CoolGrayMuted, fontSize = 10.sp)
                                Text(wordCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Committed Version", color = CoolGrayMuted, fontSize = 10.sp)
                                Text(dateStr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Characters", color = CoolGrayMuted, fontSize = 10.sp)
                                Text(charCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}



