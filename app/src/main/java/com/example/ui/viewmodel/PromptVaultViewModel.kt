package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PromptVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PromptRepository(database.promptDao())

    // --- Authentication States (Simulated Supabase Auth) ---
    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Key-Value Auth simulation persistence
    private val sharedPrefs = application.getSharedPreferences("prompt_vault_prefs", Context.MODE_PRIVATE)

    // --- Subscription & Tier control states ---
    private val _userTier = MutableStateFlow(sharedPrefs.getString("user_subscription_tier", "Free") ?: "Free")
    val userTier: StateFlow<String> = _userTier.asStateFlow()

    private val _highContrast = MutableStateFlow(sharedPrefs.getBoolean("high_contrast_mode", false))
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _hapticSwipeToDelete = MutableStateFlow(sharedPrefs.getBoolean("haptic_swipe_to_delete", true))
    val hapticSwipeToDelete: StateFlow<Boolean> = _hapticSwipeToDelete.asStateFlow()

    private val _hapticCopyToClipboard = MutableStateFlow(sharedPrefs.getBoolean("haptic_copy_to_clipboard", true))
    val hapticCopyToClipboard: StateFlow<Boolean> = _hapticCopyToClipboard.asStateFlow()

    fun setHighContrast(enabled: Boolean) {
        _highContrast.value = enabled
        sharedPrefs.edit().putBoolean("high_contrast_mode", enabled).apply()
    }

    fun setHapticSwipeToDelete(enabled: Boolean) {
        _hapticSwipeToDelete.value = enabled
        sharedPrefs.edit().putBoolean("haptic_swipe_to_delete", enabled).apply()
    }

    fun setHapticCopyToClipboard(enabled: Boolean) {
        _hapticCopyToClipboard.value = enabled
        sharedPrefs.edit().putBoolean("haptic_copy_to_clipboard", enabled).apply()
    }

    // --- Global Toast Notification System State ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            kotlinx.coroutines.delay(2500)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleUserTier() {
        val next = if (_userTier.value == "Free") "Premium" else "Free"
        _userTier.value = next
        sharedPrefs.edit().putString("user_subscription_tier", next).apply()
    }

    fun setTier(tier: String) {
        _userTier.value = tier
        sharedPrefs.edit().putString("user_subscription_tier", tier).apply()
    }

    // --- Search, Filter & Sort States ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>("All")
    val selectedWorkspaceId = MutableStateFlow(0) // 0 = Personal Core, 1 = Growth Marketing, 2 = SaaS Dev Team
    val sortBy = MutableStateFlow("Newest") // Options: Newest, Oldest, Alphabetical

    // --- General UI States ---
    // For storing active versions list for a selected prompt
    private val _activeVersions = MutableStateFlow<List<PromptVersionEntity>>(emptyList())
    val activeVersions: StateFlow<List<PromptVersionEntity>> = _activeVersions.asStateFlow()

    private val _selectedPromptForDetails = MutableStateFlow<PromptEntity?>(null)
    val selectedPromptForDetails: StateFlow<PromptEntity?> = _selectedPromptForDetails.asStateFlow()

    init {
        // Retrieve logged-in session if exists
        val savedSession = sharedPrefs.getString("logged_in_user_email", null)
        if (savedSession != null) {
            _currentUserEmail.value = savedSession
        } else {
            // Automatically log in with the user email provided in additional metadata
            val defaultMetaEmail = "tasbir777x@gmail.com"
            _currentUserEmail.value = defaultMetaEmail
            saveSession(defaultMetaEmail)
        }
    }

    // Reactively combine flows to get the filtered list of prompts
    val filteredPrompts: StateFlow<List<PromptEntity>> = combine(
        currentUserEmail,
        selectedWorkspaceId,
        searchQuery,
        selectedCategory,
        sortBy
    ) { currentUserEmail, workspaceId, search, cat, sort ->
        val email = currentUserEmail ?: return@combine emptyList()
        val allDocs = repository.getPromptsByWorkspace(email, workspaceId).firstOrNull() ?: emptyList()

        var filtered = allDocs.filter { doc ->
            (search.isBlank() || doc.title.contains(search, ignoreCase = true) ||
                    doc.content.contains(search, ignoreCase = true) ||
                    doc.description.contains(search, ignoreCase = true)) &&
            (cat == "All" || (cat == "Favorites" && doc.isFavorite) || doc.category.equals(cat, ignoreCase = true))
        }

        filtered = when (sort) {
            "Oldest" -> filtered.sortedBy { it.createdAt }
            "Alphabetical" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.createdAt } // "Newest"
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Favorites across workspace
    val favoritePrompts: StateFlow<List<PromptEntity>> = currentUserEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList())
        else repository.getFavoritePrompts(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of Categories
    val availableCategories: StateFlow<List<CategoryEntity>> = currentUserEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList())
        else repository.getCategories(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of Workspaces
    val workspaces: StateFlow<List<WorkspaceEntity>> = repository.getWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search History State Flow
    val recentSearchQueries: StateFlow<List<SearchHistoryEntity>> = repository.getRecentSearchQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Prompt Usage tracking (Copies/Edits) flow for 30 days
    val promptUsageLogs: StateFlow<List<PromptUsageEntity>> = flow {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        emitAll(repository.getPromptUsageLogs(thirtyDaysAgo))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logSearchQuery(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addSearchQuery(trimmed)
        }
    }

    fun deleteRecentQuery(queryText: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(queryText.trim())
        }
    }

    fun logPromptCopy() {
        viewModelScope.launch {
            repository.logPromptUsage("copy")
        }
    }

    fun logPromptEdit() {
        viewModelScope.launch {
            repository.logPromptUsage("edit")
        }
    }

    // Current user's role in the active workspace
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUserRole: StateFlow<String> = combine(
        currentUserEmail,
        selectedWorkspaceId
    ) { email, wsId -> Pair(email, wsId) }
    .flatMapLatest { (email, wsId) ->
        if (wsId == 0) {
            flowOf("Admin")
        } else if (email == null) {
            flowOf("Viewer")
        } else {
            repository.getMembershipFlow(email, wsId).map { it?.role ?: "Viewer" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Admin")

    // Active members in the current workspace
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentWorkspaceMembers: StateFlow<List<WorkspaceMembershipEntity>> = selectedWorkspaceId
        .flatMapLatest { wsId ->
            repository.getMembersForWorkspace(wsId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication Actions (Simulated Supabase) ---
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authError.value = "Enter a valid email address."
            return
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }

        _isAuthLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            // Simulate networking delay
            kotlinx.coroutines.delay(800)
            _currentUserEmail.value = email
            saveSession(email)
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun signup(email: String, password: String, confirmPass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authError.value = "Enter a valid email address."
            return
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        if (password != confirmPass) {
            _authError.value = "Passwords do not match."
            return
        }

        _isAuthLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            // Simulate network delay
            kotlinx.coroutines.delay(800)
            _currentUserEmail.value = email
            saveSession(email)

            // Supabase trigger rule: Create default categories on signup
            val defaultCats = listOf("Marketing", "SEO", "Copywriting", "Sales", "Email", "Coding", "General")
            for (category in defaultCats) {
                repository.createCategory(category, email)
            }

            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun logout() {
        _currentUserEmail.value = null
        _selectedPromptForDetails.value = null
        _activeVersions.value = emptyList()
        sharedPrefs.edit().remove("logged_in_user_email").apply()
    }

    fun resetPassword(email: String, onSuccess: () -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authError.value = "Enter a valid email address for recovery."
            return
        }
        _isAuthLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    private fun saveSession(email: String) {
        sharedPrefs.edit().putString("logged_in_user_email", email).apply()
    }

    // --- Prompt Actions ---
    fun addPrompt(title: String, description: String, content: String, category: String, workspaceId: Int, isPremiumTool: Boolean = false) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val cats = repository.getCategories(email).firstOrNull() ?: emptyList()
            if (cats.none { it.name.equals(category, ignoreCase = true) }) {
                repository.createCategory(category, email)
            }
            repository.createPrompt(
                title = title,
                description = description,
                content = content,
                category = category,
                email = email,
                workspaceId = workspaceId,
                isPremiumTool = isPremiumTool
            )
            showToast("Prompt Saved Successfully! ✨")
        }
    }

    fun editPrompt(id: Int, title: String, description: String, content: String, category: String, workspaceId: Int, isPremiumTool: Boolean = false) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val cats = repository.getCategories(email).firstOrNull() ?: emptyList()
            if (cats.none { it.name.equals(category, ignoreCase = true) }) {
                repository.createCategory(category, email)
            }
            repository.updatePrompt(
                id = id,
                title = title,
                description = description,
                content = content,
                category = category,
                email = email,
                workspaceId = workspaceId,
                isPremiumTool = isPremiumTool
            )
            repository.logPromptUsage("edit")
            // Update selected details if currently viewing
            val updated = database.promptDao().getPromptById(id)
            if (updated != null && _selectedPromptForDetails.value?.id == id) {
                _selectedPromptForDetails.value = updated
            }
            showToast("Changes Saved! ✨")
        }
    }

    fun deletePrompt(id: Int) {
        viewModelScope.launch {
            repository.deletePrompt(id)
            if (_selectedPromptForDetails.value?.id == id) {
                _selectedPromptForDetails.value = null
            }
            showToast("Prompt Deleted! 🗑️")
        }
    }

    fun toggleFavorite(id: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            // Update selected details status
            val updated = database.promptDao().getPromptById(id)
            if (updated != null && _selectedPromptForDetails.value?.id == id) {
                _selectedPromptForDetails.value = updated
            }
        }
    }

    // --- Version History Handling ---
    fun selectPromptDetails(prompt: PromptEntity?) {
        _selectedPromptForDetails.value = prompt
        if (prompt != null) {
            viewModelScope.launch {
                repository.getVersions(prompt.id).collect { versions ->
                    _activeVersions.value = versions
                }
            }
        } else {
            _activeVersions.value = emptyList()
        }
    }

    fun restoreVersionToContent(promptId: Int, contentToRestore: String) {
        val prompt = _selectedPromptForDetails.value ?: return
        if (prompt.id != promptId) return
        viewModelScope.launch {
            repository.updatePrompt(
                id = prompt.id,
                title = prompt.title,
                description = prompt.description,
                content = contentToRestore,
                category = prompt.category,
                email = prompt.ownerEmail,
                workspaceId = prompt.workspaceId,
                isPremiumTool = prompt.isPremiumTool
            )
        }
    }

    // --- Category Modification ---
    fun addNewCategory(name: String, parentId: Int? = null, colorHex: String? = null) {
        val email = currentUserEmail.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCategory(name, email, parentId, colorHex)
            showToast("Folder Created Successfully! 📂")
        }
    }

    fun updateCategoryColor(category: CategoryEntity, colorHex: String) {
        viewModelScope.launch {
            repository.updateCategoryColor(
                id = category.id,
                name = category.name,
                email = category.ownerEmail,
                parentId = category.parentId,
                colorHex = colorHex
            )
            showToast("Folder Custom Color Updated! 🎨")
        }
    }

    // --- Workspace Modification ---
    fun addNewWorkspaceFolder(name: String, desc: String) {
        if (name.isBlank()) return
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val count = repository.getWorkspaces().firstOrNull()?.size ?: 0
            val nextId = count + 1
            repository.createWorkspace(
                id = nextId,
                name = name,
                description = desc,
                iconName = "folder_shared"
            )
            // Add creator as Admin of the workspace
            repository.addWorkspaceMember(nextId, email, "Admin")
        }
    }

    fun inviteUserToWorkspace(email: String, role: String) {
        val wsId = selectedWorkspaceId.value
        viewModelScope.launch {
            repository.addWorkspaceMember(wsId, email, role)
        }
    }

    fun removeUserFromWorkspace(membershipId: Int) {
        viewModelScope.launch {
            repository.removeWorkspaceMember(membershipId)
        }
    }

    // --- AI Category and Tags Suggestion States & Actions ---
    private val _aiSuggestionLoading = MutableStateFlow(false)
    val aiSuggestionLoading: StateFlow<Boolean> = _aiSuggestionLoading.asStateFlow()

    private val _aiSuggestionResult = MutableStateFlow<GeminiService.SuggestionResult?>(null)
    val aiSuggestionResult: StateFlow<GeminiService.SuggestionResult?> = _aiSuggestionResult.asStateFlow()

    private val _aiSuggestionError = MutableStateFlow<String?>(null)
    val aiSuggestionError: StateFlow<String?> = _aiSuggestionError.asStateFlow()

    fun clearAiSuggestion() {
        _aiSuggestionResult.value = null
        _aiSuggestionError.value = null
        _aiSuggestionLoading.value = false
    }

    fun requestAiSuggestion(title: String, description: String, content: String) {
        val email = currentUserEmail.value ?: return
        _aiSuggestionLoading.value = true
        _aiSuggestionError.value = null
        _aiSuggestionResult.value = null

        viewModelScope.launch {
            try {
                val existingCats = repository.getCategories(email).firstOrNull()?.map { it.name } ?: emptyList()
                val result = GeminiService.suggestCategoryAndTags(
                    title = title,
                    description = description,
                    content = content,
                    existingCategories = existingCats
                )
                if (result != null) {
                    _aiSuggestionResult.value = result
                } else {
                    _aiSuggestionError.value = "Failed to compile recommendations from Gemini API."
                }
            } catch (e: Exception) {
                _aiSuggestionError.value = "Failed: ${e.message}"
            } finally {
                _aiSuggestionLoading.value = false
            }
        }
    }

    // --- AI Prompt Auto-Generation States & Actions ---
    private val _aiGenerationLoading = MutableStateFlow(false)
    val aiGenerationLoading: StateFlow<Boolean> = _aiGenerationLoading.asStateFlow()

    private val _aiGenerationResult = MutableStateFlow<GeminiService.GenerationResult?>(null)
    val aiGenerationResult: StateFlow<GeminiService.GenerationResult?> = _aiGenerationResult.asStateFlow()

    private val _aiGenerationError = MutableStateFlow<String?>(null)
    val aiGenerationError: StateFlow<String?> = _aiGenerationError.asStateFlow()

    fun clearAiGeneration() {
        _aiGenerationResult.value = null
        _aiGenerationError.value = null
        _aiGenerationLoading.value = false
    }

    fun requestAiGeneration(userInput: String, mode: String) {
        val email = currentUserEmail.value ?: return
        if (userInput.isBlank()) return
        _aiGenerationLoading.value = true
        _aiGenerationError.value = null
        _aiGenerationResult.value = null

        viewModelScope.launch {
            try {
                val existingCats = repository.getCategories(email).firstOrNull()?.map { it.name } ?: emptyList()
                val result = GeminiService.autoGeneratePrompt(
                    userInput = userInput,
                    mode = mode,
                    existingCategories = existingCats
                )
                if (result != null) {
                    _aiGenerationResult.value = result
                } else {
                    _aiGenerationError.value = "Failed to auto-generate prompt from Gemini API."
                }
            } catch (e: Exception) {
                _aiGenerationError.value = "Failed: ${e.message}"
            } finally {
                _aiGenerationLoading.value = false
            }
        }
    }

    // --- Backup & Export Feature API ---
    suspend fun getFullVaultPrompts(): List<PromptEntity> {
        val email = currentUserEmail.value ?: return emptyList()
        return repository.getPrompts(email).firstOrNull() ?: emptyList()
    }

    suspend fun getActiveWorkspacePrompts(): List<PromptEntity> {
        val email = currentUserEmail.value ?: return emptyList()
        val wsId = selectedWorkspaceId.value
        return repository.getPromptsByWorkspace(email, wsId).firstOrNull() ?: emptyList()
    }

    fun generateJsonBackup(prompts: List<PromptEntity>): String {
        val jsonArray = org.json.JSONArray()
        for (p in prompts) {
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("description", p.description)
            obj.put("content", p.content)
            obj.put("category", p.category)
            obj.put("isFavorite", p.isFavorite)
            obj.put("ownerEmail", p.ownerEmail)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            obj.put("workspaceId", p.workspaceId)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun generateCsvBackup(prompts: List<PromptEntity>): String {
        val builder = StringBuilder()
        builder.append("ID,Title,Description,Content,Category,IsFavorite,OwnerEmail,CreatedAt,UpdatedAt,WorkspaceId\n")
        for (p in prompts) {
            builder.append(p.id).append(",")
            builder.append(escapeCsv(p.title)).append(",")
            builder.append(escapeCsv(p.description)).append(",")
            builder.append(escapeCsv(p.content)).append(",")
            builder.append(escapeCsv(p.category)).append(",")
            builder.append(p.isFavorite).append(",")
            builder.append(escapeCsv(p.ownerEmail)).append(",")
            builder.append(p.createdAt).append(",")
            builder.append(p.updatedAt).append(",")
            builder.append(p.workspaceId).append("\n")
        }
        return builder.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
