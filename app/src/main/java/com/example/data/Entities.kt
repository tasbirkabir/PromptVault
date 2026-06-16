package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val content: String,
    val category: String,
    val isFavorite: Boolean = false,
    val ownerEmail: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val workspaceId: Int = 0, // 0 = Personal, 1 = Marketing Team, 2 = Tech Team, etc.
    val isPremiumTool: Boolean = false
)

@Entity(tableName = "prompt_versions")
data class PromptVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val promptId: Int,
    val versionNumber: Int,
    val content: String,
    val editedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ownerEmail: String,
    val parentId: Int? = null,
    val colorHex: String = "#8B5CF6"
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val iconName: String
)

@Entity(tableName = "workspace_memberships", indices = [androidx.room.Index(value = ["workspaceId", "userEmail"], unique = true)])
data class WorkspaceMembershipEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workspaceId: Int,
    val userEmail: String,
    val role: String // "Admin", "Editor", "Viewer"
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prompt_usage")
data class PromptUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "copy" or "edit"
    val timestamp: Long = System.currentTimeMillis()
)


