package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts WHERE ownerEmail = :ownerEmail ORDER BY id DESC")
    fun getAllPrompts(ownerEmail: String): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE ownerEmail = :ownerEmail AND workspaceId = :workspaceId ORDER BY id DESC")
    fun getPromptsInWorkspace(ownerEmail: String, workspaceId: Int): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE ownerEmail = :ownerEmail AND isFavorite = 1 ORDER BY id DESC")
    fun getFavoritePrompts(ownerEmail: String): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE id = :id LIMIT 1")
    suspend fun getPromptById(id: Int): PromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long

    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deletePromptById(id: Int)

    // Versions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: PromptVersionEntity)

    @Query("SELECT * FROM prompt_versions WHERE promptId = :promptId ORDER BY versionNumber DESC")
    fun getVersionsForPrompt(promptId: Int): Flow<List<PromptVersionEntity>>

    // Categories
    @Query("SELECT * FROM categories WHERE ownerEmail = :ownerEmail")
    fun getCategories(ownerEmail: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    // Workspaces / Team Folders
    @Query("SELECT * FROM workspaces")
    fun getWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    // Workspace Memberships
    @Query("SELECT * FROM workspace_memberships WHERE workspaceId = :workspaceId ORDER BY id ASC")
    fun getMembersForWorkspace(workspaceId: Int): Flow<List<WorkspaceMembershipEntity>>

    @Query("SELECT * FROM workspace_memberships WHERE userEmail = :userEmail AND workspaceId = :workspaceId LIMIT 1")
    suspend fun getMembershipForUserInWorkspace(userEmail: String, workspaceId: Int): WorkspaceMembershipEntity?

    @Query("SELECT * FROM workspace_memberships WHERE userEmail = :userEmail AND workspaceId = :workspaceId LIMIT 1")
    fun getMembershipFlow(userEmail: String, workspaceId: Int): Flow<WorkspaceMembershipEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: WorkspaceMembershipEntity)

    @Query("DELETE FROM workspace_memberships WHERE id = :id")
    suspend fun deleteMembershipById(id: Int)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSearchQueries(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query NOT IN (SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 5)")
    suspend fun pruneSearchHistory()

    @Query("DELETE FROM search_history WHERE `query` = :queryText")
    suspend fun deleteSearchQuery(queryText: String)

    // Prompt Usage tracking (copies & edits) inside 30 days
    @Query("SELECT * FROM prompt_usage WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getPromptUsageLogs(sinceTimestamp: Long): Flow<List<PromptUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromptUsage(usage: PromptUsageEntity)
}
