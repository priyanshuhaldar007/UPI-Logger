package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs

object OcrProcessor {

    private const val TAG = "OcrProcessor"

    // Lazily initialize ML Kit Latin text recognizer (runs 100% offline on device)
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from the provided Bitmap using on-device ML Kit Text Recognition.
     * Guaranteed to run offline without internet connectivity.
     *
     * Improvements applied:
     * - Hardware bitmap safety to prevent crashes on Android 10+
     * - Top-to-bottom & left-to-right natural reading order line reconstruction
     * - Horizontal line-merging for side-by-side key-value pairs (e.g. "Note: Dinner")
     * - OCR character and whitespace normalization
     */
    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Ensure bitmap is safe for ML Kit (HARDWARE config cannot be accessed directly by CPU)
                val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
                } else {
                    bitmap
                }

                val inputImage = InputImage.fromBitmap(safeBitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { textResult ->
                        val reconstructed = reconstructNaturalReadingOrder(textResult)
                        val normalized = normalizeOcrText(reconstructed)
                        Log.d(TAG, "OCR Success: extracted ${normalized.lines().size} lines (${normalized.length} chars)")
                        continuation.resume(normalized)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "OCR Failure: ${exception.message}", exception)
                        // Do not crash: return empty text as fallback
                        continuation.resume("")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating InputImage: ${e.message}", e)
                continuation.resume("")
            }
        }
    }

    /**
     * Reconstructs text in natural human reading order (top-to-bottom, left-to-right).
     * Automatically groups side-by-side elements onto the same line.
     */
    private fun reconstructNaturalReadingOrder(textResult: Text): String {
        val allLines = textResult.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) {
            return textResult.text.trim()
        }

        // Sort lines primarily by vertical Y coordinate with line-height grouping tolerance
        val sortedLines = allLines.sortedWith { line1, line2 ->
            val box1 = line1.boundingBox
            val box2 = line2.boundingBox
            if (box1 == null || box2 == null) {
                0
            } else {
                val topDiff = box1.top - box2.top
                val avgHeight = ((box1.height() + box2.height()) / 2).coerceAtLeast(10)
                // If tops are close (within ~35% of line height), sort left-to-right
                if (abs(topDiff) < avgHeight * 0.35) {
                    box1.left - box2.left
                } else {
                    topDiff
                }
            }
        }

        val result = StringBuilder()
        var previousBox: Rect? = null

        for (line in sortedLines) {
            val text = line.text.trim()
            if (text.isEmpty()) continue
            val box = line.boundingBox

            if (previousBox != null && box != null) {
                val avgHeight = ((previousBox.height() + box.height()) / 2).coerceAtLeast(10)
                val isSameHorizontalRow = abs(box.centerY() - previousBox.centerY()) < avgHeight * 0.45

                if (isSameHorizontalRow && box.left > previousBox.right - 5) {
                    // Same line, horizontally adjacent (e.g. "Note" and "Dinner with friends")
                    val separator = if (result.endsWith(":") || text.startsWith(":")) " " else " "
                    result.append(separator).append(text)
                } else {
                    // New line
                    result.append("\n").append(text)
                }
            } else {
                if (result.isNotEmpty()) result.append("\n")
                result.append(text)
            }
            previousBox = box
        }

        val reconstructed = result.toString().trim()
        return if (reconstructed.isNotEmpty()) reconstructed else textResult.text.trim()
    }

    /**
     * Cleans up common OCR artifacts, non-standard unicode spaces, and formatting glitches.
     */
    private fun normalizeOcrText(text: String): String {
        return text
            // Replace non-breaking and irregular spaces with standard space
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('\u200B', ' ')
            .replace('\uFEFF', ' ')
            .replace('\t', ' ')
            // Normalize smart quotes
            .replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            // Normalize bullets
            .replace('·', '•')
            .replace('●', '•')
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}

