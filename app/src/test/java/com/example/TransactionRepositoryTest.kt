package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionDao
import com.example.data.model.TransactionEntry
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
        repository = TransactionRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFindMostRecentUnmergedByType() = runBlocking {
        val now = System.currentTimeMillis()

        // Insert unmerged SCREEN_A within window
        val id1 = dao.insert(
            TransactionEntry(
                amount = "₹100",
                sourceScreenType = "SCREEN_A",
                isMerged = false,
                createdAt = now - 60_000 // 1 min ago
            )
        )

        // Insert unmerged SCREEN_A older than 10 mins
        dao.insert(
            TransactionEntry(
                amount = "₹200",
                sourceScreenType = "SCREEN_A",
                isMerged = false,
                createdAt = now - 700_000 // 11.6 mins ago
            )
        )

        // Insert merged SCREEN_A
        dao.insert(
            TransactionEntry(
                amount = "₹300",
                sourceScreenType = "SCREEN_A",
                isMerged = true,
                createdAt = now - 30_000
            )
        )

        val found = dao.findMostRecentUnmergedByType("SCREEN_A", now - 600_000)
        assertNotNull(found)
        assertEquals(id1, found?.id)
        assertEquals("₹100", found?.amount)

        // Opposite type query should return null
        val notFound = dao.findMostRecentUnmergedByType("SCREEN_B", now - 600_000)
        assertNull(notFound)
    }

    @Test
    fun testSequenceBasedMergeFallbackScreenAThenScreenB() = runBlocking {
        val screenAOcr = """
            Rohan Sharma
            ₹250
            Paid successfully
            Axis Bank XX1234
        """.trimIndent()

        // 1. Process Screen A
        val (entryA, mergedA) = repository.processAndStoreCapture(
            rawOcrText = screenAOcr,
            screenshotFilePath = "/path/to/screen_a.png",
            expectedScreenType = "SCREEN_A"
        )
        assertEquals(false, mergedA)
        assertEquals("₹250", entryA.amount)
        assertEquals("Rohan Sharma", entryA.payee)
        assertEquals("SCREEN_A", entryA.sourceScreenType)

        val screenBOcr = """
            Transaction Details
            Note: Team lunch treats
        """.trimIndent()

        // 2. Process Screen B (no ref number)
        val (mergedEntry, isMerged) = repository.processAndStoreCapture(
            rawOcrText = screenBOcr,
            screenshotFilePath = "/path/to/screen_b.png",
            expectedScreenType = "SCREEN_B"
        )

        assertTrue(isMerged)
        assertEquals(entryA.id, mergedEntry.id)
        assertEquals("₹250", mergedEntry.amount)
        assertEquals("Rohan Sharma", mergedEntry.payee)
        assertEquals("Team lunch treats", mergedEntry.note)
        assertEquals("MERGED", mergedEntry.sourceScreenType)
        assertTrue(mergedEntry.isMerged)
        assertEquals("/path/to/screen_a.png", mergedEntry.screenshotAPath)
        assertEquals("/path/to/screen_b.png", mergedEntry.screenshotBPath)
    }

    @Test
    fun testScreenACropsCaptureAndSequenceMerge() = runBlocking {
        val headerText = """
            Rohan Sharma
            rohan@okhdfcbank
        """.trimIndent()
        val amountText = "₹350"

        // 1. Process Screen A via crops
        val (entryA, mergedA) = repository.processAndStoreCapture(
            rawOcrText = "$headerText\n$amountText",
            screenshotFilePath = "/path/to/screen_a_crop.png",
            expectedScreenType = "SCREEN_A",
            headerText = headerText,
            amountText = amountText
        )
        assertEquals(false, mergedA)
        assertEquals("₹350", entryA.amount)
        assertEquals("Rohan Sharma", entryA.payee)
        assertEquals("rohan@okhdfcbank", entryA.vpa)
        assertEquals("", entryA.referenceNumber)
        assertEquals("SCREEN_A", entryA.sourceScreenType)

        // 2. Process Screen B with Note and Reference Number
        val screenBOcr = """
            Date and time: 8 September 2026 • 04:15PM
            UPI Transaction Id: 987654321000
            Note
            Dinner with friends
        """.trimIndent()

        val (mergedEntry, isMerged) = repository.processAndStoreCapture(
            rawOcrText = screenBOcr,
            screenshotFilePath = "/path/to/screen_b.png",
            expectedScreenType = "SCREEN_B"
        )

        assertTrue(isMerged)
        assertEquals(entryA.id, mergedEntry.id)
        assertEquals("₹350", mergedEntry.amount)
        assertEquals("Rohan Sharma", mergedEntry.payee)
        assertEquals("rohan@okhdfcbank", mergedEntry.vpa)
        assertEquals("Dinner with friends", mergedEntry.note)
        assertEquals("987654321000", mergedEntry.referenceNumber)
        assertEquals("MERGED", mergedEntry.sourceScreenType)
        assertTrue(mergedEntry.isMerged)
        assertEquals("/path/to/screen_a_crop.png", mergedEntry.screenshotAPath)
        assertEquals("/path/to/screen_b.png", mergedEntry.screenshotBPath)
    }

    @Test
    fun testReprocessAllEntries_TextOnlyAndRetroactiveMerge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = System.currentTimeMillis()

        // 1. Insert unmerged Screen A entry with raw text only
        val idA = dao.insert(
            TransactionEntry(
                amount = "₹500",
                payee = "Priya Sharma",
                vpa = "priya@okaxis",
                rawOcrText = "Paid to Priya Sharma\npriya@okaxis\n₹500",
                sourceScreenType = "SCREEN_A",
                screenshotAPath = "/non/existent/path_a.png",
                isMerged = false,
                createdAt = now - 60_000
            )
        )

        // 2. Insert unmerged Screen B entry 30 seconds later with note and ref
        val idB = dao.insert(
            TransactionEntry(
                date = "8 Sep 2026",
                note = "Office Lunch",
                referenceNumber = "987654321098",
                rawOcrText = "Date and time: 8 September 2026 • 01:30PM\nUPI Transaction Id: 987654321098\nNote\nOffice Lunch",
                sourceScreenType = "SCREEN_B",
                screenshotBPath = "/non/existent/path_b.png",
                isMerged = false,
                createdAt = now - 30_000
            )
        )

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val summary = repository.reprocessAllEntries(context) { curr, tot ->
            progressUpdates.add(curr to tot)
        }

        assertEquals(2, summary.totalEntries)
        assertEquals(0, summary.reprocessedFromImage)
        assertEquals(2, summary.reprocessedFromTextOnly)
        assertEquals(2, summary.missingScreenshotFiles)
        assertEquals(1, summary.newlyMergedPairs)
        assertEquals(listOf(1 to 2, 2 to 2), progressUpdates)

        // Verify remaining entries in Room
        val remaining = dao.getAllTransactionsSync()
        assertEquals(1, remaining.size)
        val merged = remaining[0]
        assertEquals(idA, merged.id)
        assertEquals("₹500", merged.amount)
        assertEquals("Priya Sharma", merged.payee)
        assertEquals("priya@okaxis", merged.vpa)
        assertEquals("Office Lunch", merged.note)
        assertEquals("987654321098", merged.referenceNumber)
        assertTrue(merged.isMerged)
        assertEquals("MERGED", merged.sourceScreenType)
    }

    @Test
    fun testReprocessAllEntries_FarApartEntriesNotMerged() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = System.currentTimeMillis()

        // 1. Screen A at T=0
        dao.insert(
            TransactionEntry(
                amount = "₹100",
                rawOcrText = "₹100\nRahul",
                sourceScreenType = "SCREEN_A",
                isMerged = false,
                createdAt = now - 600_000 // 10 minutes ago
            )
        )

        // 2. Screen B 10 minutes later (difference > 5 mins)
        dao.insert(
            TransactionEntry(
                note = "Coffee",
                rawOcrText = "Note\nCoffee",
                sourceScreenType = "SCREEN_B",
                isMerged = false,
                createdAt = now
            )
        )

        val summary = repository.reprocessAllEntries(context) { _, _ -> }
        assertEquals(2, summary.totalEntries)
        assertEquals(0, summary.newlyMergedPairs)

        val all = dao.getAllTransactionsSync()
        assertEquals(2, all.size)
    }
}
