package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var dao: PromptDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    dao = db.promptDao()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PromptVault", appName)
  }

  @Test
  fun `test search history local storage insertion and limit to 5`() = runBlocking {
    // Insert distinct queries
    dao.insertSearchQuery(SearchHistoryEntity("jetpack compose", 1000L))
    dao.insertSearchQuery(SearchHistoryEntity("room database", 2000L))
    dao.insertSearchQuery(SearchHistoryEntity("coroutine flows", 3000L))
    dao.insertSearchQuery(SearchHistoryEntity("material design 3", 4000L))
    dao.insertSearchQuery(SearchHistoryEntity("dependency injection", 5000L))
    dao.insertSearchQuery(SearchHistoryEntity("hilt testing", 6000L))

    // Prune entries above 5
    dao.pruneSearchHistory()

    val history = dao.getRecentSearchQueries().first()
    assertEquals(5, history.size)
    
    // The oldest entry "jetpack compose" (timestamp = 1000) should be pruned
    // Order should be descending by timestamp
    assertEquals("hilt testing", history[0].query)
    assertEquals("dependency injection", history[1].query)
    assertEquals("material design 3", history[2].query)
    assertEquals("coroutine flows", history[3].query)
    assertEquals("room database", history[4].query)
  }

  @Test
  fun `test search history duplicates overwrite and update timestamp`() = runBlocking {
    dao.insertSearchQuery(SearchHistoryEntity("jetpack compose", 1000L))
    dao.insertSearchQuery(SearchHistoryEntity("room database", 2000L))
    
    // Insert "jetpack compose" again with newer timestamp
    dao.insertSearchQuery(SearchHistoryEntity("jetpack compose", 3000L))

    val history = dao.getRecentSearchQueries().first()
    assertEquals(2, history.size)
    assertEquals("jetpack compose", history[0].query)
    assertEquals("room database", history[1].query)
  }

  @Test
  fun `test delete query from search history`() = runBlocking {
    dao.insertSearchQuery(SearchHistoryEntity("jetpack compose", 1000L))
    dao.insertSearchQuery(SearchHistoryEntity("room database", 2000L))

    dao.deleteSearchQuery("jetpack compose")

    val history = dao.getRecentSearchQueries().first()
    assertEquals(1, history.size)
    assertEquals("room database", history[0].query)
  }
}
