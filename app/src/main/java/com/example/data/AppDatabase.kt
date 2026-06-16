package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PromptEntity::class,
        PromptVersionEntity::class,
        CategoryEntity::class,
        WorkspaceEntity::class,
        WorkspaceMembershipEntity::class,
        SearchHistoryEntity::class,
        PromptUsageEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prompt_vault_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.promptDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: PromptDao) {
                // Populate default Workspaces
                dao.insertWorkspace(
                    WorkspaceEntity(
                        id = 0,
                        name = "Personal Core",
                        description = "Your private encrypted AI drive",
                        iconName = "folder_person"
                    )
                )
                dao.insertWorkspace(
                    WorkspaceEntity(
                        id = 1,
                        name = "Growth Marketing",
                        description = "Collaborative SEO, ads & email copies",
                        iconName = "campaign"
                    )
                )
                dao.insertWorkspace(
                    WorkspaceEntity(
                        id = 2,
                        name = "SaaS Dev Team",
                        description = "Standard prompts for coding & code generation",
                        iconName = "code"
                    )
                )

                // Fill pre-defined categories for an generic admin user or as base defaults
                val defaultEmail = "tasbir777x@gmail.com"

                // Seed dynamic Workspace Memberships for Workspace 0, 1, 2
                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 0, userEmail = defaultEmail, role = "Admin"))

                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 1, userEmail = defaultEmail, role = "Editor"))
                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 1, userEmail = "alice@growth.com", role = "Editor"))
                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 1, userEmail = "bob@growth.com", role = "Viewer"))

                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 2, userEmail = defaultEmail, role = "Viewer"))
                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 2, userEmail = "charlie@devteam.com", role = "Admin"))
                dao.insertMembership(WorkspaceMembershipEntity(workspaceId = 2, userEmail = "dave@devteam.com", role = "Editor"))
                val defaultCategories = listOf(
                    "General", "Marketing", "SEO", "Copywriting", "Sales", "Email", "Coding"
                )
                val catIdsMap = mutableMapOf<String, Int>()
                for (catName in defaultCategories) {
                    val catId = dao.insertCategory(CategoryEntity(name = catName, ownerEmail = defaultEmail)).toInt()
                    catIdsMap[catName] = catId
                    // Also define fallback category for other generic test credentials
                    dao.insertCategory(CategoryEntity(name = catName, ownerEmail = ""))
                }

                // Seed nested categories/folders!
                val marketingId = catIdsMap["Marketing"] ?: 0
                if (marketingId > 0) {
                    dao.insertCategory(CategoryEntity(name = "Social Media", ownerEmail = defaultEmail, parentId = marketingId))
                    dao.insertCategory(CategoryEntity(name = "Ads Copy", ownerEmail = defaultEmail, parentId = marketingId))
                }

                val codingId = catIdsMap["Coding"] ?: 0
                if (codingId > 0) {
                    dao.insertCategory(CategoryEntity(name = "Boilerplates", ownerEmail = defaultEmail, parentId = codingId))
                    dao.insertCategory(CategoryEntity(name = "Refactoring", ownerEmail = defaultEmail, parentId = codingId))
                }

                // Add nice default prompts to immediately demo capabilities
                val welcomePromptId = dao.insertPrompt(
                    PromptEntity(
                        title = "Welcome to PromptVault 🚀",
                        description = "An interactive introduction prompt to help you start.",
                        content = "You are a professional copywriter. Create a compelling marketing headline for PromptVault, a beautiful and secure cloud-hosted vault for AI engineers to save, organize, and copy prompts.",
                        category = "General",
                        isFavorite = true,
                        ownerEmail = defaultEmail,
                        workspaceId = 0
                    )
                ).toInt()

                // Insert automated version 1 history
                dao.insertVersion(
                    PromptVersionEntity(
                        promptId = welcomePromptId,
                        versionNumber = 1,
                        content = "You are a professional copywriter. Create a compelling marketing headline for PromptVault, a beautiful and secure cloud-hosted vault for AI engineers to save, organize, and copy prompts."
                    )
                )

                val seoPromptId = dao.insertPrompt(
                    PromptEntity(
                        title = "SEO Strategy & Hook Generator",
                        description = "Optimize social posts for maximum CTR and organic search relevance.",
                        content = "Given the topic: {topic}, act as an expert SEO growth hacker. Outline 10 click-worthy blog title variants with correct word count guidelines and a meta description rich in commercial intent keywords.",
                        category = "SEO",
                        isFavorite = false,
                        ownerEmail = defaultEmail,
                        workspaceId = 1
                    )
                ).toInt()

                dao.insertVersion(
                    PromptVersionEntity(
                        promptId = seoPromptId,
                        versionNumber = 1,
                        content = "Given the topic: {topic}, act as an expert SEO growth hacker. Outline 10 click-worthy blog titles."
                    )
                )
                dao.insertVersion(
                    PromptVersionEntity(
                        promptId = seoPromptId,
                        versionNumber = 2,
                        content = "Given the topic: {topic}, act as an expert SEO growth hacker. Outline 10 click-worthy blog title variants with correct word count guidelines and a meta description rich in commercial intent keywords."
                    )
                )

                // Coding helper - Premium
                val gitPromptId = dao.insertPrompt(
                    PromptEntity(
                        title = "Jetpack Compose Builder Assistant",
                        description = "Quick boilerplate for consistent Material Design 3 screens.",
                        content = "Act as an expert Android engineer. Write a highly stylized Jetpack Compose screen implementing dynamic M3 theme properties, full edge-to-edge layout offsets, and spring animation transitions for an interactive list.",
                        category = "Coding",
                        isFavorite = true,
                        ownerEmail = defaultEmail,
                        workspaceId = 2,
                        isPremiumTool = true
                    )
                ).toInt()

                dao.insertVersion(
                    PromptVersionEntity(
                        promptId = gitPromptId,
                        versionNumber = 1,
                        content = "Act as an expert Android engineer. Write a highly stylized Jetpack Compose screen implementing dynamic M3 theme properties, full edge-to-edge layout offsets, and spring animation transitions for an interactive list."
                    )
                )

                // Advanced Premium Tool
                val loopPromptId = dao.insertPrompt(
                    PromptEntity(
                        title = "Deep-Code Loop Optimizer ⚡",
                        description = "[PREMIUM] Resolves compiler exceptions, performance bottlenecks, and does iterative self-repair.",
                        content = "You are a compiler architecture specialist. Review index allocations and join sequences. Suggest custom Proguard optimization and recursive loops mapping to reduce CPU garbage collection peaks.",
                        category = "Coding",
                        isFavorite = false,
                        ownerEmail = defaultEmail,
                        workspaceId = 2,
                        isPremiumTool = true
                    )
                ).toInt()

                dao.insertVersion(
                    PromptVersionEntity(
                        promptId = loopPromptId,
                        versionNumber = 1,
                        content = "You are a compiler architecture specialist. Review index allocations and join sequences. Suggest custom Proguard optimization and recursive loops mapping to reduce CPU garbage collection peaks."
                    )
                )

                // Seed 30 days of prompt copies and edits usage history (for sparkline chart)
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val seedUsages = listOf(
                    PromptUsageEntity(type = "copy", timestamp = now - 28 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 27 * oneDayMs + 10000),
                    PromptUsageEntity(type = "edit", timestamp = now - 25 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 22 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 21 * oneDayMs),
                    PromptUsageEntity(type = "edit", timestamp = now - 21 * oneDayMs + 5000),
                    PromptUsageEntity(type = "copy", timestamp = now - 20 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 18 * oneDayMs),
                    PromptUsageEntity(type = "edit", timestamp = now - 15 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 14 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 12 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 10 * oneDayMs),
                    PromptUsageEntity(type = "edit", timestamp = now - 10 * oneDayMs + 2000),
                    PromptUsageEntity(type = "copy", timestamp = now - 8 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 6 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 5 * oneDayMs),
                    PromptUsageEntity(type = "edit", timestamp = now - 5 * oneDayMs + 4000),
                    PromptUsageEntity(type = "copy", timestamp = now - 4 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 3 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 2 * oneDayMs),
                    PromptUsageEntity(type = "edit", timestamp = now - 1 * oneDayMs),
                    PromptUsageEntity(type = "copy", timestamp = now - 1 * oneDayMs + 12000),
                    PromptUsageEntity(type = "copy", timestamp = now)
                )
                seedUsages.forEach { dao.insertPromptUsage(it) }
            }
        }
    }
}
