package com.flashcards

import android.content.Context
import androidx.room.Room

class Application : android.app.Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database =
            Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "database")
                .allowMainThreadQueries()
                .build()
    }
}

fun db() = Application.database