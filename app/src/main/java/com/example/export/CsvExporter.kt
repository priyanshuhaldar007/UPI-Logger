package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val TAG = "CsvExporter"

    data class ExportResult(
        val isSuccess: Boolean,
        val rowCount: Int,
        val localFilePath: String? = null,
        val shareUri: Uri? = null,
        val errorMessage: String? = null
    ) {
        val success: Boolean get() = isSuccess
    }

    /**
     * Generates CSV content from transaction entries and saves to Downloads + cache for sharing.
     */
    suspend fun exportAndShare(
        context: Context,
        entries: List<TransactionEntry>,
        onlyReviewed: Boolean
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val filteredEntries = if (onlyReviewed) {
                entries.filter { it.isReviewed }
            } else {
                entries
            }

            if (filteredEntries.isEmpty()) {
                return@withContext ExportResult(
                    isSuccess = false,
                    rowCount = 0,
                    errorMessage = "No entries to export"
                )
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "upi_notes_$timestamp.csv"

            val csvContent = buildString {
                // Prepend UTF-8 BOM so Excel and other spreadsheet viewers render ₹ and non-ASCII correctly
                append("\uFEFF")

                // Header
                appendLine("Date,Amount,Payee,VPA,Reference,Payment Method,Note,Super.Money Transaction ID,Merge Status")

                for (entry in filteredEntries) {
                    val mergeStatus = when (entry.sourceScreenType) {
                        "MERGED" -> "Merged"
                        "SCREEN_A" -> "Screen A Only (missing Note)"
                        "SCREEN_B" -> "Screen B Only (missing Amount)"
                        else -> "Unknown"
                    }
                    append(escapeCsv(entry.date)).append(",")
                    append(escapeCsv(entry.amount)).append(",")
                    append(escapeCsv(entry.payee)).append(",")
                    append(escapeCsv(entry.vpa)).append(",")
                    append(escapeCsv(entry.referenceNumber)).append(",")
                    append(escapeCsv(entry.paymentMethod)).append(",")
                    append(escapeCsv(entry.note)).append(",")
                    append(escapeCsv(entry.superMoneyTransactionId)).append(",")
                    append(escapeCsv(mergeStatus))
                    appendLine()
                }
            }

            // 1. Save to cache dir for FileProvider sharing
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val shareFile = File(cacheDir, fileName)
            shareFile.writeText(csvContent)

            val shareUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "FileProvider error: ${e.message}", e)
                null
            }

            // 2. Save to device public Downloads folder
            var savedPublicPath: String? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/UpiNoteLogger")
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                        }
                        savedPublicPath = "Downloads/UpiNoteLogger/$fileName"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetDir = File(downloadsDir, "UpiNoteLogger").apply { mkdirs() }
                    val targetFile = File(targetDir, fileName)
                    targetFile.writeText(csvContent)
                    savedPublicPath = targetFile.absolutePath
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving to public Downloads (fallback to internal share): ${e.message}")
            }

            ExportResult(
                isSuccess = true,
                rowCount = filteredEntries.size,
                localFilePath = savedPublicPath ?: shareFile.absolutePath,
                shareUri = shareUri
            )
        } catch (e: Exception) {
            Log.e(TAG, "Export error: ${e.message}", e)
            ExportResult(
                isSuccess = false,
                rowCount = 0,
                errorMessage = e.message ?: "Failed to generate CSV"
            )
        }
    }

    /**
     * Creates an Android Share Sheet Intent for the exported CSV file.
     */
    fun createShareIntent(shareUri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "UPI Notes Export")
            putExtra(Intent.EXTRA_TEXT, "Exported UPI transaction notes from UPI Note Logger.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\r", " ").replace("\n", " ").trim()
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "'$clean"
        }
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }
}
