package com.example.data.repository

import android.util.Log
import com.example.data.database.TransactionDao
import com.example.data.model.TransactionEntry
import com.example.parser.FieldParser
import com.example.parser.ParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    val allTransactions: Flow<List<TransactionEntry>> = transactionDao.getAllTransactions()
    val unreviewedTransactions: Flow<List<TransactionEntry>> = transactionDao.getUnreviewedTransactions()
    val unreviewedCount: Flow<Int> = transactionDao.getUnreviewedCount()
    val totalCount: Flow<Int> = transactionDao.getTotalCount()

    suspend fun getTransactionById(id: Long): TransactionEntry? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id)
    }

    suspend fun getUnmergedEntries(excludeId: Long): List<TransactionEntry> = withContext(Dispatchers.IO) {
        transactionDao.getUnmergedEntries(excludeId)
    }

    /**
     * Processes OCR text from a captured screenshot, runs FieldParser,
     * checks for auto-matching with an existing entry having the same reference number,
     * and saves or updates the record in Room.
     */
    suspend fun processAndStoreCapture(
        rawOcrText: String,
        screenshotFilePath: String
    ): Pair<TransactionEntry, Boolean> = withContext(Dispatchers.IO) {
        val parsed = FieldParser.parse(rawOcrText)
        val ref = parsed.referenceNumber.trim()
        val isScreenB = parsed.detectedScreenType == ParsedTransaction.ScreenType.SCREEN_B

        // Check if there is an existing entry with the same reference number
        if (ref.isNotEmpty()) {
            val existingMatches = transactionDao.findByReference(ref)
            val match = existingMatches.firstOrNull()

            if (match != null) {
                // Auto-merge with existing entry!
                val mergedEntry = match.copy(
                    amount = if (match.amount.isNotBlank()) match.amount else parsed.amount,
                    date = if (match.date.isNotBlank()) match.date else parsed.date,
                    payee = if (match.payee.isNotBlank()) match.payee else parsed.payee,
                    vpa = if (match.vpa.isNotBlank()) match.vpa else parsed.vpa,
                    paymentMethod = if (match.paymentMethod.isNotBlank()) match.paymentMethod else parsed.paymentMethod,
                    note = if (parsed.note.isNotBlank()) parsed.note else match.note,
                    superMoneyTransactionId = if (parsed.superMoneyTransactionId.isNotBlank()) {
                        parsed.superMoneyTransactionId
                    } else {
                        match.superMoneyTransactionId
                    },
                    screenshotAPath = if (isScreenB) match.screenshotAPath else (screenshotFilePath.ifBlank { match.screenshotAPath }),
                    screenshotBPath = if (isScreenB) screenshotFilePath else match.screenshotBPath,
                    rawOcrText = buildMergedRawText(match.rawOcrText, rawOcrText, isScreenB),
                    isMerged = true,
                    sourceScreenType = "MERGED",
                    updatedAt = System.currentTimeMillis()
                )
                transactionDao.update(mergedEntry)
                Log.d("TransactionRepo", "Auto-merged entry ID ${mergedEntry.id} with ref $ref")
                return@withContext Pair(mergedEntry, true)
            }
        }

        // Otherwise, insert as a new unmerged entry
        val newEntry = TransactionEntry(
            amount = parsed.amount,
            date = parsed.date,
            payee = parsed.payee,
            vpa = parsed.vpa,
            referenceNumber = ref,
            paymentMethod = parsed.paymentMethod,
            note = parsed.note,
            superMoneyTransactionId = parsed.superMoneyTransactionId,
            rawOcrText = rawOcrText,
            screenshotAPath = if (!isScreenB) screenshotFilePath else null,
            screenshotBPath = if (isScreenB) screenshotFilePath else null,
            isMerged = false,
            isReviewed = false,
            sourceScreenType = if (isScreenB) "SCREEN_B" else "SCREEN_A",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newId = transactionDao.insert(newEntry)
        val created = newEntry.copy(id = newId)
        Log.d("TransactionRepo", "Inserted new entry ID $newId (ref: $ref)")
        Pair(created, false)
    }

    /**
     * Manually merges two entries that failed to auto-merge.
     * The primary entry retains combined values, and the secondary entry is deleted.
     */
    suspend fun manualMerge(primaryId: Long, secondaryId: Long): TransactionEntry? = withContext(Dispatchers.IO) {
        val primary = transactionDao.getTransactionById(primaryId) ?: return@withContext null
        val secondary = transactionDao.getTransactionById(secondaryId) ?: return@withContext null

        val merged = primary.copy(
            amount = primary.amount.ifBlank { secondary.amount },
            date = primary.date.ifBlank { secondary.date },
            payee = primary.payee.ifBlank { secondary.payee },
            vpa = primary.vpa.ifBlank { secondary.vpa },
            referenceNumber = primary.referenceNumber.ifBlank { secondary.referenceNumber },
            paymentMethod = primary.paymentMethod.ifBlank { secondary.paymentMethod },
            note = primary.note.ifBlank { secondary.note },
            superMoneyTransactionId = primary.superMoneyTransactionId.ifBlank { secondary.superMoneyTransactionId },
            screenshotAPath = primary.screenshotAPath ?: secondary.screenshotAPath,
            screenshotBPath = primary.screenshotBPath ?: secondary.screenshotBPath,
            rawOcrText = "--- Entry 1 OCR ---\n${primary.rawOcrText}\n\n--- Entry 2 OCR ---\n${secondary.rawOcrText}",
            isMerged = true,
            sourceScreenType = "MERGED",
            updatedAt = System.currentTimeMillis()
        )

        transactionDao.update(merged)
        transactionDao.deleteById(secondaryId)
        merged
    }

    suspend fun update(entry: TransactionEntry) = withContext(Dispatchers.IO) {
        transactionDao.update(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(entry: TransactionEntry) = withContext(Dispatchers.IO) {
        // Delete local screenshot files if present
        entry.screenshotAPath?.let { File(it).delete() }
        entry.screenshotBPath?.let { File(it).delete() }
        transactionDao.delete(entry)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val entry = transactionDao.getTransactionById(id)
        if (entry != null) {
            delete(entry)
        }
    }

    suspend fun setReviewed(id: Long, reviewed: Boolean) = withContext(Dispatchers.IO) {
        val entry = transactionDao.getTransactionById(id) ?: return@withContext
        transactionDao.update(entry.copy(isReviewed = reviewed, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Deletes the Screen A screenshot file from device storage and updates the transaction entry.
     */
    suspend fun deleteScreenshotA(entry: TransactionEntry): TransactionEntry = withContext(Dispatchers.IO) {
        entry.screenshotAPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        val updated = entry.copy(screenshotAPath = null, updatedAt = System.currentTimeMillis())
        transactionDao.update(updated)
        updated
    }

    /**
     * Deletes the Screen B screenshot file from device storage and updates the transaction entry.
     */
    suspend fun deleteScreenshotB(entry: TransactionEntry): TransactionEntry = withContext(Dispatchers.IO) {
        entry.screenshotBPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        val updated = entry.copy(screenshotBPath = null, updatedAt = System.currentTimeMillis())
        transactionDao.update(updated)
        updated
    }

    /**
     * Deletes the screenshot matching the provided file path and updates the entry in Room.
     */
    suspend fun deleteScreenshotByPath(entry: TransactionEntry, path: String): TransactionEntry = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        val updated = when {
            entry.screenshotAPath == path -> entry.copy(screenshotAPath = null, updatedAt = System.currentTimeMillis())
            entry.screenshotBPath == path -> entry.copy(screenshotBPath = null, updatedAt = System.currentTimeMillis())
            else -> entry
        }
        if (updated != entry) {
            transactionDao.update(updated)
        }
        updated
    }

    /**
     * Deletes all screenshot files from internal storage ("captures" directory)
     * and clears screenshot paths from all records in the database.
     * Retains all extracted transaction records, notes, amounts, and dates.
     * Returns the count of deleted image files.
     */
    suspend fun deleteAllScreenshots(context: android.content.Context): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        val capturesDir = File(context.filesDir, "captures")
        if (capturesDir.exists() && capturesDir.isDirectory) {
            val files = capturesDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg"))) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
        }
        // Clear paths in database
        transactionDao.clearAllScreenshotPaths()
        deletedCount
    }

    /**
     * Calculates storage statistics for screenshots in internal storage.
     * Returns Pair(fileCount, totalSizeBytes).
     */
    suspend fun getScreenshotStorageInfo(context: android.content.Context): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val capturesDir = File(context.filesDir, "captures")
        if (!capturesDir.exists() || !capturesDir.isDirectory) {
            return@withContext Pair(0, 0L)
        }
        val files = capturesDir.listFiles() ?: return@withContext Pair(0, 0L)
        var count = 0
        var totalBytes = 0L
        for (file in files) {
            if (file.isFile && (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg"))) {
                count++
                totalBytes += file.length()
            }
        }
        Pair(count, totalBytes)
    }

    private fun buildMergedRawText(existingText: String, newText: String, isScreenB: Boolean): String {
        return if (isScreenB) {
            "--- Screen A OCR ---\n$existingText\n\n--- Screen B (Details) OCR ---\n$newText"
        } else {
            "--- Screen A OCR ---\n$newText\n\n--- Screen B (Details) OCR ---\n$existingText"
        }
    }
}
