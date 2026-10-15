package com.example.notelink.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notelink.data.model.Link
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var linkDao: LinkDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        linkDao = database.linkDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetLink() = runBlocking {
        val link = Link(name = "Google", url = "https://google.com", category = "Search")
        linkDao.insertLink(link)
        val allLinks = linkDao.getAllLinks().first()
        assertEquals(1, allLinks.size)
        assertEquals("Google", allLinks[0].name)
    }

}
