package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.TransactionRepository

class UpiNoteLoggerApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
