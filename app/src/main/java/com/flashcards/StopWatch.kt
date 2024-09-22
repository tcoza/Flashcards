package com.flashcards

import android.os.SystemClock

// Courtesy of ChatGPT
class Stopwatch {
    private var startTime = 0L
    private var accumulatedTime = 0L
    private var running = false
    var isRunning
        get() = running
        private set(value) { running = value }

    // Start or resume the stopwatch
    fun start() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime()
            isRunning = true
        }
    }

    // Pause the stopwatch
    fun pause() {
        if (isRunning) {
            accumulatedTime += SystemClock.elapsedRealtime() - startTime
            isRunning = false
        }
    }

    // Reset the stopwatch
    fun reset() {
        startTime = 0L
        accumulatedTime = 0L
        isRunning = false
    }

    fun getElapsedTimeMillis(): Long {
        return if (isRunning) {
            accumulatedTime + (SystemClock.elapsedRealtime() - startTime)
        } else {
            accumulatedTime
        }
    }
}