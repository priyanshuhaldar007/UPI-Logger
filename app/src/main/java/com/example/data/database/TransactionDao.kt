package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntry>>

    @Query("SELECT * FROM transactions ORDER BY createdAt ASC")
    suspend fun getAllTransactionsSync(): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE isReviewed = 0 ORDER BY createdAt DESC")
    fun getUnreviewedTransactions(): Flow<List<TransactionEntry>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isReviewed = 0")
    fun getUnreviewedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntry?

    @Query("SELECT * FROM transactions WHERE referenceNumber = :ref AND referenceNumber != '' ORDER BY createdAt DESC")
    suspend fun findByReference(ref: String): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE isMerged = 0 AND sourceScreenType = :screenType AND createdAt >= :sinceTimestamp ORDER BY createdAt DESC LIMIT 1")
    suspend fun findMostRecentUnmergedByType(screenType: String, sinceTimestamp: Long): TransactionEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TransactionEntry): Long

    @Update
    suspend fun update(entry: TransactionEntry)

    @Delete
    suspend fun delete(entry: TransactionEntry)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE isMerged = 0 AND id != :excludeId ORDER BY createdAt DESC")
    suspend fun getUnmergedEntries(excludeId: Long): List<TransactionEntry>

    @Query("UPDATE transactions SET screenshotAPath = NULL, screenshotBPath = NULL")
    suspend fun clearAllScreenshotPaths()
}
