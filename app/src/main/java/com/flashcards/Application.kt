package com.flashcards

import androidx.room.Room
import com.flashcards.database.AppDatabase

class Application : android.app.Application() {
    companion object {
        lateinit var instance: Application
            private set
    }

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        openDatabase()
    }

    fun openDatabase() { database = AppDatabase.build(applicationContext) }
}

fun db() = Application.instance.database