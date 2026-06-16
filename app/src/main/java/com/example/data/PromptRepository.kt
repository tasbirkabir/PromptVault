package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PromptRepository(private val promptDao: PromptDao) {

    fun getPrompts(email: String): Flow<List<PromptEntity>> =
        promptDao.getAllPrompts(email)

    fun getPromptsByWorkspace(email: String, workspaceId: Int): Flow<List<PromptEntity>> =
        promptDao.getPromptsInWorkspace(email, workspaceId)

    fun getFavoritePrompts(email: String): Flow<List<PromptEntity>> =
        promptDao.getFavoritePrompts(email)

    fun getVersions(promptId: Int): Flow<List<PromptVersionEntity>> =
        promptDao.getVersionsForPrompt(promptId)

    fun getCategories(email: String): Flow<List<CategoryEntity>> =
        promptDao.getCategories(email)

    fun getWorkspaces(): Flow<List<WorkspaceEntity>> =
        promptDao.getWorkspaces()

    suspend fun createPrompt(
        title: String,
        description: String,
        content: String,
        category: String,
        email: String,
        workspaceId: Int,
        isPremiumTool: Boolean = false
    ): Int {
        val prompt = PromptEntity(
            title = title,
            description = description,
            content = content,
            category = category,
            ownerEmail = email,
            workspaceId = workspaceId,
            isPremiumTool = isPremiumTool
        )
        val promptId = promptDao.insertPrompt(prompt).toInt()

        // Automatically save original version
        promptDao.insertVersion(
            PromptVersionEntity(
                promptId = promptId,
                versionNumber = 1,
                content = content
            )
        )
        return promptId
    }

    suspend fun updatePrompt(
        id: Int,
        title: String,
        description: String,
        content: String,
        category: String,
        email: String,
        workspaceId: Int,
        isPremiumTool: Boolean = false
    ) {
        val existing = promptDao.getPromptById(id) ?: return
        
        // Update the prompt
        val updated = existing.copy(
            title = title,
            description = description,
            content = content,
            category = category,
            workspaceId = workspaceId,
            isPremiumTool = isPremiumTool,
            updatedAt = System.currentTimeMillis()
        )
        promptDao.updatePrompt(updated)

        // Check version history to append version
        val previousVersions = promptDao.getVersionsForPrompt(id).firstOrNull() ?: emptyList()
        val latestContent = previousVersions.firstOrNull()?.content
        
        if (latestContent != content) {
            val nextVersionNo = (previousVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1
            promptDao.insertVersion(
                PromptVersionEntity(
                    promptId = id,
                    versionNumber = nextVersionNo,
                    content = content
                )
            )
        }
    }

    suspend fun deletePrompt(id: Int) {
        promptDao.deletePromptById(id)
    }

    suspend fun toggleFavorite(id: Int) {
        val existing = promptDao.getPromptById(id) ?: return
        val updated = existing.copy(isFavorite = !existing.isFavorite)
        promptDao.updatePrompt(updated)
    }

    suspend fun createCategory(name: String, email: String, parentId: Int? = null, colorHex: String? = null): Int {
        return promptDao.insertCategory(
            CategoryEntity(
                name = name,
                ownerEmail = email,
                parentId = parentId,
                colorHex = colorHex ?: "#8B5CF6"
            )
        ).toInt()
    }

    suspend fun updateCategoryColor(id: Int, name: String, email: String, parentId: Int?, colorHex: String) {
        promptDao.insertCategory(
            CategoryEntity(
                id = id,
                name = name,
                ownerEmail = email,
                parentId = parentId,
                colorHex = colorHex
            )
        )
    }

    suspend fun createWorkspace(id: Int, name: String, description: String, iconName: String) {
        promptDao.insertWorkspace(WorkspaceEntity(id, name, description, iconName))
    }

    fun getMembersForWorkspace(workspaceId: Int): Flow<List<WorkspaceMembershipEntity>> =
        promptDao.getMembersForWorkspace(workspaceId)

    suspend fun getMembershipForUserInWorkspace(email: String, workspaceId: Int): WorkspaceMembershipEntity? =
        promptDao.getMembershipForUserInWorkspace(email, workspaceId)

    fun getMembershipFlow(email: String, workspaceId: Int): Flow<WorkspaceMembershipEntity?> =
        promptDao.getMembershipFlow(email, workspaceId)

    suspend fun addWorkspaceMember(workspaceId: Int, userEmail: String, role: String) {
        promptDao.insertMembership(WorkspaceMembershipEntity(workspaceId = workspaceId, userEmail = userEmail, role = role))
    }

    suspend fun removeWorkspaceMember(id: Int) {
        promptDao.deleteMembershipById(id)
    }

    // Search History
    fun getRecentSearchQueries(): Flow<List<SearchHistoryEntity>> =
        promptDao.getRecentSearchQueries()

    suspend fun addSearchQuery(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isEmpty()) return
        promptDao.insertSearchQuery(SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis()))
        promptDao.pruneSearchHistory()
    }

    suspend fun deleteSearchQuery(queryText: String) {
        promptDao.deleteSearchQuery(queryText)
    }

    // Prompt Usage Logs
    fun getPromptUsageLogs(sinceTimestamp: Long): Flow<List<PromptUsageEntity>> =
        promptDao.getPromptUsageLogs(sinceTimestamp)

    suspend fun logPromptUsage(type: String) {
        promptDao.insertPromptUsage(PromptUsageEntity(type = type, timestamp = System.currentTimeMillis()))
    }
}
