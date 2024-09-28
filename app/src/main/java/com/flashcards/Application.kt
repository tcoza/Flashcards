package com.flashcards

import android.content.Context
import androidx.room.Room

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

    fun openDatabase() {
        database =
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                AppDatabase.DB_NAME)
                .allowMainThreadQueries()
                .build()
    }
}

fun db() = Application.instance.database