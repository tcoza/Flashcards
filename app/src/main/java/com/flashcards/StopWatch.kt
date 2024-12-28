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
        if (isRunning) return
        startTime = SystemClock.elapsedRealtime()
        isRunning = true
    }

    // Pause the stopwatch
    fun pause() {
        if (!isRunning) return
        accumulatedTime += SystemClock.elapsedRealtime() - startTime
        isRunning = false
    }

    // Reset the stopwatch
    fun reset() {
        startTime = 0L
        accumulatedTime = 0L
        isRunning = false
    }

    fun restart() {
        reset()
        start()
    }

    fun getElapsedTimeMillis() = accumulatedTime + if(isRunning) (SystemClock.elapsedRealtime() - startTime) else 0
}