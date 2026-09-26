package com.inklazy.routeverge.service

/** The loop publishes every tick, but writes session progress at most every five seconds. */
internal object ProgressPersistence {
    private const val INTERVAL_MILLIS = 5_000L

    fun shouldWrite(now: Long, lastWrite: Long, force: Boolean = false): Boolean =
        force || lastWrite == 0L || now < lastWrite || now - lastWrite >= INTERVAL_MILLIS
}
