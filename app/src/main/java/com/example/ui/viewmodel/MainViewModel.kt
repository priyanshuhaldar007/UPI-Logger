package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.UpiNoteLoggerApplication
import com.example.data.model.TransactionEntry
import com.example.export.CsvExporter
import com.example.ocr.OcrProcessor
import com.example.service.CaptureOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class ReviewFilter {
    ALL,
    UNREVIEWED_ONLY,
    MERGED_ONLY,
    UNPAIRED_ONLY
}

data class ScreenshotStorageStats(
    val count: Int = 0,
    val totalBytes: Long = 0L,
    val formattedSize: String = "0 KB"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as UpiNoteLoggerApplication).repository

    val isCaptureServiceRunning: StateFlow<Boolean> = CaptureOverlayService.isRunning

    val allTransactions: StateFlow<List<TransactionEntry>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreviewedCount: StateFlow<Int> = repository.unreviewedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _storageStats = MutableStateFlow(ScreenshotStorageStats())
    val storageStats: StateFlow<ScreenshotStorageStats> = _storageStats.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val (count, bytes) = repository.getScreenshotStorageInfo(getApplication())
            val formatted = when {
                bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
            _storageStats.value = ScreenshotStorageStats(count, bytes, formatted)
        }
    }

    // Review screen filter & search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(ReviewFilter.ALL)
    val activeFilter: StateFlow<ReviewFilter> = _activeFilter.asStateFlow()

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<TransactionEntry>> = combine(
        allTransactions,
        _searchQuery,
        _activeFilter
    ) { list, query, filter ->
        list.filter { entry ->
            val matchesFilter = when (filter) {
                ReviewFilter.ALL -> true
                ReviewFilter.UNREVIEWED_ONLY -> !entry.isReviewed
                ReviewFilter.MERGED_ONLY -> entry.isMerged
                ReviewFilter.UNPAIRED_ONLY -> !entry.isMerged
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                entry.note.lowercase().contains(q) ||
                        entry.payee.lowercase().contains(q) ||
                        entry.vpa.lowercase().contains(q) ||
                        entry.referenceNumber.contains(q) ||
                        entry.amount.contains(q) ||
                        entry.paymentMethod.lowercase().contains(q) ||
                        entry.superMoneyTransactionId.lowercase().contains(q)
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ReviewFilter) {
        _activeFilter.value = filter
    }

    fun updateEntry(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.update(entry)
            _snackbarMessage.value = "Transaction updated"
        }
    }

    fun deleteEntry(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            refreshStorageStats()
            _snackbarMessage.value = "Transaction deleted"
        }
    }

    fun deleteScreenshotA(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.deleteScreenshotA(entry)
            refreshStorageStats()
            _snackbarMessage.value = "Screen A screenshot deleted"
        }
    }

    fun deleteScreenshotB(entry: TransactionEntry) {
        viewModelScope.launch {
            repository.deleteScreenshotB(entry)
            refreshStorageStats()
            _snackbarMessage.value = "Screen B screenshot deleted"
        }
    }

    fun deleteScreenshotByPath(entry: TransactionEntry, path: String) {
        viewModelScope.launch {
            repository.deleteScreenshotByPath(entry, path)
            refreshStorageStats()
            _snackbarMessage.value = "Screenshot deleted"
        }
    }

    fun deleteAllScreenshots() {
        viewModelScope.launch {
            val count = repository.deleteAllScreenshots(getApplication())
            refreshStorageStats()
            _snackbarMessage.value = if (count > 0) "Deleted $count screenshot files" else "No screenshots found to delete"
        }
    }

    fun toggleReviewed(entry: TransactionEntry) {
        viewModelScope.launch {
            val newStatus = !entry.isReviewed
            repository.setReviewed(entry.id, newStatus)
            _snackbarMessage.value = if (newStatus) "Marked as reviewed" else "Marked as unreviewed"
        }
    }

    fun manualMerge(primaryId: Long, secondaryId: Long) {
        viewModelScope.launch {
            val result = repository.manualMerge(primaryId, secondaryId)
            if (result != null) {
                _snackbarMessage.value = "Successfully merged 2 entries"
            } else {
                _snackbarMessage.value = "Failed to merge entries"
            }
        }
    }

    suspend fun getUnmergedEntries(excludeId: Long): List<TransactionEntry> {
        return repository.getUnmergedEntries(excludeId)
    }

    fun exportCsv(context: Context, onlyReviewed: Boolean, onResult: (CsvExporter.ExportResult) -> Unit) {
        viewModelScope.launch {
            val entries = allTransactions.value
            val result = CsvExporter.exportAndShare(context, entries, onlyReviewed)
            onResult(result)
        }
    }

    /**
     * Checks whether SYSTEM_ALERT_WINDOW permission is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Creates mock sample captures for testing in emulator or when UPI app is not available.
     */
    fun insertSamplePair(context: Context) {
        viewModelScope.launch {
            try {
                val ref = "4248" + (10000000..99999999).random().toString()
                val capturesDir = File(context.filesDir, "captures").apply { mkdirs() }

                // 1. Create simulated Screen A Bitmap
                val bmpA = createSampleBitmap("Screen A\nPaid to: Priya Sharma\n₹48\nAxis CC XX87\nUPI Reference ID: $ref\nSeptember 5 at 9:38 PM")
                val fileA = File(capturesDir, "sample_screen_a_${System.currentTimeMillis()}.png")
                FileOutputStream(fileA).use { bmpA.compress(Bitmap.CompressFormat.PNG, 90, it) }

                // 2. Create simulated Screen B Bitmap
                val bmpB = createSampleBitmap("Screen B (Details)\nDate and time: 5 September 2026 • 09:38PM\nPaid to: priyasharma@oksbi\nUPI Transaction Id: $ref\nSuper.Money transaction ID: SMTX99881122\nPayment Method: Axis CC XX87\nNote\nLunch with team at cafe")
                val fileB = File(capturesDir, "sample_screen_b_${System.currentTimeMillis()}.png")
                FileOutputStream(fileB).use { bmpB.compress(Bitmap.CompressFormat.PNG, 90, it) }

                // Process Screen A
                val rawA = OcrProcessor.extractText(bmpA).ifBlank {
                    "Paid to: Priya Sharma\n₹48\nAxis CC XX87\nUPI Reference ID: $ref\nSeptember 5 at 9:38 PM"
                }
                repository.processAndStoreCapture(rawA, fileA.absolutePath, "SCREEN_A")

                // Process Screen B (Auto-matches ref)
                val rawB = OcrProcessor.extractText(bmpB).ifBlank {
                    "Date and time: 5 September 2026 • 09:38PM\nPaid to: priyasharma@oksbi\nUPI Transaction Id: $ref\nSuper.Money transaction ID: SMTX99881122\nPayment Method: Axis CC XX87\nNote\nLunch with team at cafe"
                }
                repository.processAndStoreCapture(rawB, fileB.absolutePath, "SCREEN_B")

                refreshStorageStats()
                _snackbarMessage.value = "Sample Screen A + B pair created & auto-merged!"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error generating samples: ${e.message}"
            }
        }
    }

    private fun createSampleBitmap(text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0F172A"))

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isAntiAlias = true
        }

        var y = 140f
        for (line in text.lines()) {
            canvas.drawText(line, 40f, y, paint)
            y += 55f
        }
        return bitmap
    }
}
