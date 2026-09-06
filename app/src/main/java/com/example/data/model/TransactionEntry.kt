package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a logged UPI transaction entry, either from a single capture (Screen A or B)
 * or merged from both captures sharing the same UPI Reference ID.
 */
@Entity(tableName = "transactions")
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Extracted / edited fields
    val amount: String = "",
    val date: String = "",
    val payee: String = "",
    val vpa: String = "",
    val referenceNumber: String = "", // 10-14 digit UPI Reference ID / Transaction Id
    val paymentMethod: String = "",
    val note: String = "", // Highest-priority field from Screen B
    val superMoneyTransactionId: String = "",

    // Raw OCR text for verification and manual corrections
    val rawOcrText: String = "",

    // Local file paths to stored screenshots
    val screenshotAPath: String? = null,
    val screenshotBPath: String? = null,

    // Status flags
    val isMerged: Boolean = false,
    val isReviewed: Boolean = false,
    val sourceScreenType: String = "UNKNOWN", // "SCREEN_A", "SCREEN_B", or "MERGED"

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
