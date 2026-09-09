package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.database.TransactionDao
import com.example.data.model.TransactionEntry
import com.example.ocr.OcrProcessor
import com.example.parser.FieldParser
import com.example.parser.ParsedTransaction
import com.example.service.CaptureOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class ReprocessSummary(
    val totalEntries: Int,
    val reprocessedFromImage: Int,
    val reprocessedFromTextOnly: Int,
    val missingScreenshotFiles: Int,
    val newlyMergedPairs: Int
)

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
        screenshotFilePath: String,
        expectedScreenType: String = "SCREEN_A",
        headerText: String = "",
        amountText: String = ""
    ): Pair<TransactionEntry, Boolean> = withContext(Dispatchers.IO) {
        val parsed = if (expectedScreenType == "SCREEN_A") {
            if (headerText.isNotEmpty() || amountText.isNotEmpty()) {
                FieldParser.parseScreenACrops(headerText, amountText)
            } else {
                FieldParser.parse(rawOcrText)
            }
        } else {
            FieldParser.parse(rawOcrText)
        }
        val ref = parsed.referenceNumber.trim()
        val isScreenB = expectedScreenType == "SCREEN_B"

        // 1. Check if there is an existing entry with the same reference number
        val existingMatch = if (ref.isNotEmpty()) {
            transactionDao.findByReference(ref).firstOrNull()
        } else {
            null
        }

        // 2. Sequence-based merge fallback: check for most recent unmerged entry of opposite type within 10 minutes
        val match = existingMatch ?: run {
            val oppositeType = if (expectedScreenType == "SCREEN_A") "SCREEN_B" else "SCREEN_A"
            val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L
            transactionDao.findMostRecentUnmergedByType(oppositeType, tenMinutesAgo)
        }

        if (match != null) {
            // Auto-merge with existing entry!
            val mergedEntry = match.copy(
                amount = if (match.amount.isNotBlank()) match.amount else parsed.amount,
                date = if (match.date.isNotBlank()) match.date else parsed.date,
                payee = if (match.payee.isNotBlank()) match.payee else parsed.payee,
                vpa = if (match.vpa.isNotBlank()) match.vpa else parsed.vpa,
                referenceNumber = if (match.referenceNumber.isNotBlank()) match.referenceNumber else ref,
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
            Log.d("TransactionRepo", "Auto-merged entry ID ${mergedEntry.id} with ref $ref (matchedByRef=${existingMatch != null})")
            return@withContext Pair(mergedEntry, true)
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
            sourceScreenType = expectedScreenType,
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
     * Returns the count of transactions whose createdAt falls within the specified range (e.g. a calendar month).
     */
    suspend fun getTransactionCountInMonth(startTimeMillis: Long, endTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        transactionDao.getCountByCreatedAtRange(startTimeMillis, endTimeMillis)
    }

    /**
     * Permanently deletes all transactions whose createdAt falls within the specified range,
     * as well as deleting their attached screenshot image files from internal storage.
     */
    suspend fun deleteTransactionsInMonth(startTimeMillis: Long, endTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        val entries = transactionDao.getTransactionsByCreatedAtRange(startTimeMillis, endTimeMillis)
        for (entry in entries) {
            entry.screenshotAPath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            entry.screenshotBPath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
        transactionDao.deleteByCreatedAtRange(startTimeMillis, endTimeMillis)
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

    /**
     * Re-analyzes all stored entries using current cropping and parsing logic,
     * and retroactively merges unpaired entries within 5 minutes.
     */
    suspend fun reprocessAllEntries(
        context: Context,
        onProgress: (current: Int, total: Int) -> Unit
    ): ReprocessSummary = withContext(Dispatchers.IO) {
        val entries = transactionDao.getAllTransactionsSync()
        val totalEntries = entries.size
        var reprocessedFromImage = 0
        var reprocessedFromTextOnly = 0
        var missingScreenshotFiles = 0

        if (totalEntries == 0) {
            return@withContext ReprocessSummary(
                totalEntries = 0,
                reprocessedFromImage = 0,
                reprocessedFromTextOnly = 0,
                missingScreenshotFiles = 0,
                newlyMergedPairs = 0
            )
        }

        // Step b: Refresh each entry
        for ((index, entry) in entries.withIndex()) {
            var updated = entry
            var processedFromImageThisEntry = false
            var hadMissingFileThisEntry = false

            val hasPathA = !entry.screenshotAPath.isNullOrBlank()
            val hasPathB = !entry.screenshotBPath.isNullOrBlank()

            if (!hasPathA && !hasPathB) {
                hadMissingFileThisEntry = true
                missingScreenshotFiles++
            } else {
                // Process Screen A if path present
                if (hasPathA) {
                    val fileA = File(entry.screenshotAPath!!)
                    if (fileA.exists()) {
                        val bitmap = BitmapFactory.decodeFile(fileA.absolutePath)
                        if (bitmap != null) {
                            val hX = (bitmap.width * CaptureOverlayService.SCREEN_A_HEADER_X_PERCENT).toInt().coerceIn(0, bitmap.width - 1)
                            val hY = (bitmap.height * CaptureOverlayService.SCREEN_A_HEADER_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                            val hWidth = (bitmap.width * CaptureOverlayService.SCREEN_A_HEADER_WIDTH_PERCENT).toInt().coerceAtMost(bitmap.width - hX).coerceAtLeast(1)
                            val hHeight = (bitmap.height * CaptureOverlayService.SCREEN_A_HEADER_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - hY).coerceAtLeast(1)
                            val headerCrop = Bitmap.createBitmap(bitmap, hX, hY, hWidth, hHeight)

                            val aX = (bitmap.width * CaptureOverlayService.SCREEN_A_AMOUNT_X_PERCENT).toInt().coerceIn(0, bitmap.width - 1)
                            val aY = (bitmap.height * CaptureOverlayService.SCREEN_A_AMOUNT_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                            val aWidth = (bitmap.width * CaptureOverlayService.SCREEN_A_AMOUNT_WIDTH_PERCENT).toInt().coerceAtMost(bitmap.width - aX).coerceAtLeast(1)
                            val aHeight = (bitmap.height * CaptureOverlayService.SCREEN_A_AMOUNT_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - aY).coerceAtLeast(1)
                            val amountCrop = Bitmap.createBitmap(bitmap, aX, aY, aWidth, aHeight)

                            try {
                                val headerText = OcrProcessor.extractText(headerCrop)
                                val amountText = OcrProcessor.extractText(amountCrop)
                                val parsedA = FieldParser.parseScreenACrops(headerText, amountText)
                                if (parsedA.amount.isNotBlank()) updated = updated.copy(amount = parsedA.amount)
                                if (parsedA.payee.isNotBlank()) updated = updated.copy(payee = parsedA.payee)
                                if (parsedA.vpa.isNotBlank()) updated = updated.copy(vpa = parsedA.vpa)
                                processedFromImageThisEntry = true
                            } catch (e: Exception) {
                                Log.e("TransactionRepo", "Error OCR on Screen A crop", e)
                                hadMissingFileThisEntry = true
                                missingScreenshotFiles++
                            } finally {
                                if (headerCrop != bitmap) headerCrop.recycle()
                                if (amountCrop != bitmap) amountCrop.recycle()
                                bitmap.recycle()
                            }
                        } else {
                            hadMissingFileThisEntry = true
                            missingScreenshotFiles++
                        }
                    } else {
                        hadMissingFileThisEntry = true
                        missingScreenshotFiles++
                    }
                }

                // Process Screen B if path present
                if (hasPathB) {
                    val fileB = File(entry.screenshotBPath!!)
                    if (fileB.exists()) {
                        val bitmap = BitmapFactory.decodeFile(fileB.absolutePath)
                        if (bitmap != null) {
                            val startY = (bitmap.height * CaptureOverlayService.SCREEN_B_TOP_Y_PERCENT).toInt().coerceIn(0, bitmap.height - 1)
                            val cropWidth = (bitmap.width * CaptureOverlayService.SCREEN_B_LEFT_WIDTH_PERCENT).toInt().coerceIn(1, bitmap.width)
                            val cropHeight = (bitmap.height * CaptureOverlayService.SCREEN_B_BOTTOM_HEIGHT_PERCENT).toInt().coerceAtMost(bitmap.height - startY).coerceAtLeast(1)
                            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, startY, cropWidth, cropHeight)

                            try {
                                val rawB = OcrProcessor.extractText(croppedBitmap)
                                val parsedB = FieldParser.parse(rawB)
                                if (parsedB.date.isNotBlank()) updated = updated.copy(date = parsedB.date)
                                if (parsedB.paymentMethod.isNotBlank()) updated = updated.copy(paymentMethod = parsedB.paymentMethod)
                                if (parsedB.note.isNotBlank()) updated = updated.copy(note = parsedB.note)
                                if (parsedB.referenceNumber.isNotBlank()) updated = updated.copy(referenceNumber = parsedB.referenceNumber)
                                if (parsedB.superMoneyTransactionId.isNotBlank()) updated = updated.copy(superMoneyTransactionId = parsedB.superMoneyTransactionId)
                                processedFromImageThisEntry = true
                            } catch (e: Exception) {
                                Log.e("TransactionRepo", "Error OCR on Screen B crop", e)
                                hadMissingFileThisEntry = true
                                missingScreenshotFiles++
                            } finally {
                                if (croppedBitmap != bitmap) croppedBitmap.recycle()
                                bitmap.recycle()
                            }
                        } else {
                            hadMissingFileThisEntry = true
                            missingScreenshotFiles++
                        }
                    } else {
                        hadMissingFileThisEntry = true
                        missingScreenshotFiles++
                    }
                }
            }

            if (processedFromImageThisEntry) {
                reprocessedFromImage++
            } else {
                // Re-run existing parse() on stored rawOcrText
                val parsed = FieldParser.parse(entry.rawOcrText)
                if (parsed.amount.isNotBlank()) updated = updated.copy(amount = parsed.amount)
                if (parsed.payee.isNotBlank()) updated = updated.copy(payee = parsed.payee)
                if (parsed.vpa.isNotBlank()) updated = updated.copy(vpa = parsed.vpa)
                if (parsed.date.isNotBlank()) updated = updated.copy(date = parsed.date)
                if (parsed.paymentMethod.isNotBlank()) updated = updated.copy(paymentMethod = parsed.paymentMethod)
                if (parsed.note.isNotBlank()) updated = updated.copy(note = parsed.note)
                if (parsed.referenceNumber.isNotBlank()) updated = updated.copy(referenceNumber = parsed.referenceNumber)
                if (parsed.superMoneyTransactionId.isNotBlank()) updated = updated.copy(superMoneyTransactionId = parsed.superMoneyTransactionId)

                reprocessedFromTextOnly++
                if (!hadMissingFileThisEntry) {
                    missingScreenshotFiles++
                }
            }

            updated = updated.copy(updatedAt = System.currentTimeMillis())
            transactionDao.update(updated)
            onProgress(index + 1, totalEntries)
        }

        // Step c: Retroactive pairing pass
        val unmerged = transactionDao.getAllTransactionsSync().filter { !it.isMerged }
        val screenAOnly = mutableListOf<TransactionEntry>()
        val screenBOnly = mutableListOf<TransactionEntry>()

        for (item in unmerged) {
            if (item.screenshotBPath == null && (item.screenshotAPath != null || item.sourceScreenType == "SCREEN_A")) {
                screenAOnly.add(item)
            } else if (item.screenshotAPath == null && (item.screenshotBPath != null || item.sourceScreenType == "SCREEN_B")) {
                screenBOnly.add(item)
            }
        }

        var newlyMergedPairs = 0
        val maxDiffMs = 5 * 60 * 1000L // 5 minutes

        for (entryA in screenAOnly) {
            if (screenBOnly.isEmpty()) break

            var bestIndex = -1
            var minDiff = Long.MAX_VALUE

            for (i in screenBOnly.indices) {
                val candidateB = screenBOnly[i]
                val diff = Math.abs(entryA.createdAt - candidateB.createdAt)
                if (diff < minDiff) {
                    minDiff = diff
                    bestIndex = i
                }
            }

            if (bestIndex != -1 && minDiff <= maxDiffMs) {
                val matchedB = screenBOnly.removeAt(bestIndex)

                val mergedEntry = entryA.copy(
                    amount = if (entryA.amount.isNotBlank()) entryA.amount else matchedB.amount,
                    date = if (entryA.date.isNotBlank()) entryA.date else matchedB.date,
                    payee = if (entryA.payee.isNotBlank()) entryA.payee else matchedB.payee,
                    vpa = if (entryA.vpa.isNotBlank()) entryA.vpa else matchedB.vpa,
                    referenceNumber = if (entryA.referenceNumber.isNotBlank()) entryA.referenceNumber else matchedB.referenceNumber,
                    paymentMethod = if (entryA.paymentMethod.isNotBlank()) entryA.paymentMethod else matchedB.paymentMethod,
                    note = if (matchedB.note.isNotBlank()) matchedB.note else entryA.note,
                    superMoneyTransactionId = if (entryA.superMoneyTransactionId.isNotBlank()) entryA.superMoneyTransactionId else matchedB.superMoneyTransactionId,
                    screenshotAPath = entryA.screenshotAPath ?: matchedB.screenshotAPath,
                    screenshotBPath = matchedB.screenshotBPath ?: entryA.screenshotBPath,
                    rawOcrText = buildMergedRawText(entryA.rawOcrText, matchedB.rawOcrText, isScreenB = true),
                    isMerged = true,
                    sourceScreenType = "MERGED",
                    updatedAt = System.currentTimeMillis()
                )

                transactionDao.update(mergedEntry)
                transactionDao.delete(matchedB)
                newlyMergedPairs++
            }
        }

        ReprocessSummary(
            totalEntries = totalEntries,
            reprocessedFromImage = reprocessedFromImage,
            reprocessedFromTextOnly = reprocessedFromTextOnly,
            missingScreenshotFiles = missingScreenshotFiles,
            newlyMergedPairs = newlyMergedPairs
        )
    }

    private fun buildMergedRawText(existingText: String, newText: String, isScreenB: Boolean): String {
        return if (isScreenB) {
            "--- Screen A OCR ---\n$existingText\n\n--- Screen B (Details) OCR ---\n$newText"
        } else {
            "--- Screen A OCR ---\n$newText\n\n--- Screen B (Details) OCR ---\n$existingText"
        }
    }
}
