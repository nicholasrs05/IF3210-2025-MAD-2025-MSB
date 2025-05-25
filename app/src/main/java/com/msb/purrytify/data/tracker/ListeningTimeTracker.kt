package com.msb.purrytify.data.tracker

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningTimeTracker @Inject constructor() {
    private var startTime: LocalDateTime? = null
    private var accumulatedMinutes: Int = 0
    private var isTracking: Boolean = false

    fun startTracking() {
        if (!isTracking) {
            startTime = LocalDateTime.now()
            isTracking = true
        }
    }

    fun pauseTracking() {
        if (isTracking) {
            accumulateTime()
            isTracking = false
        }
    }

    fun stopTracking(): Int {
        if (isTracking) {
            accumulateTime()
        }
        val total = accumulatedMinutes
        reset()
        return total
    }

    private fun accumulateTime() {
        startTime?.let { start ->
            val now = LocalDateTime.now()
            val minutes = ChronoUnit.MINUTES.between(start, now).toInt()
            accumulatedMinutes += minutes
            startTime = now
        }
    }

    private fun reset() {
        startTime = null
        accumulatedMinutes = 0
        isTracking = false
    }

    fun flushAccumulatedTime(): Int {
        if (isTracking) {
            accumulateTime()
        }
        val minutes = accumulatedMinutes
        accumulatedMinutes = 0 // Reset accumulated minutes after flushing
        return minutes
    }
} 